"""Simplified integration tests for core functionality."""

from unittest.mock import patch
from datetime import datetime

from src.processing.data_processor import DataProcessor
from src.insights.llm_service import LLMInsightService
from src.models import FeedbackItem, SentimentType, TopicTag


class TestCoreIntegration:
    """Integration tests focusing on core data flow."""

    def test_data_processing_flow(self):
        """Test the data processing pipeline with real processors."""
        # Create sample feedback item
        sample_item = FeedbackItem(
            source="reddit",
            source_id="test123",
            source_url="https://reddit.com/test",
            author="testuser",
            title="App crashes on startup",
            content="The app keeps crashing every time I try to open it. Very frustrating!",
            timestamp=datetime.utcnow(),
            raw_data={"score": 5},
        )

        # Process with real data processor
        processor = DataProcessor()
        processed_items = processor.process_feedback_items([sample_item])

        # Verify processing
        assert len(processed_items) == 1
        processed_item = processed_items[0]
        assert processed_item.content is not None
        assert len(processed_item.content) > 0
        assert processed_item.author == "testuser"

    def test_llm_service_initialization(self):
        """Test LLM service can be initialized with proper configuration."""
        with patch("src.insights.llm_service.settings") as mock_settings:
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = "test-key"
            mock_settings.anthropic_model = "claude-3-haiku-20240307"

            with patch("src.insights.llm_service.anthropic.Anthropic"):
                service = LLMInsightService()
                assert service.provider == "anthropic"
                assert service.model == "claude-3-haiku-20240307"

    def test_end_to_end_analysis_flow(self):
        """Test end-to-end analysis with mocked LLM responses."""
        # Create test feedback
        feedback_item = FeedbackItem(
            source="reddit",
            source_id="test123",
            source_url="https://reddit.com/test",
            author="testuser",
            title="Bug Report",
            content="The app crashes when I click the settings button.",
            timestamp=datetime.utcnow(),
            raw_data={},
        )

        # Mock LLM service
        with patch("src.insights.llm_service.settings") as mock_settings:
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = "test-key"
            mock_settings.anthropic_model = "claude-3-haiku-20240307"

            with patch("src.insights.llm_service.anthropic.Anthropic"):
                service = LLMInsightService()

                # Mock LLM responses
                sentiment_response = '{"sentiment": "negative", "confidence": 0.9, "reasoning": "User reports crashes"}'
                topic_response = '{"topics": ["bug"], "confidence": 0.95, "reasoning": "Clear bug report"}'

                with patch.object(
                    service,
                    "_call_llm",
                    side_effect=[sentiment_response, topic_response],
                ):
                    analysis = service.analyze_single_item(feedback_item)

                    assert analysis is not None
                    assert (
                        analysis.sentiment_analysis.sentiment == SentimentType.NEGATIVE
                    )
                    assert TopicTag.BUG in analysis.topic_analysis.topics
                    assert analysis.sentiment_analysis.confidence == 0.9

    def test_data_validation_rules(self):
        """Test that data validation works correctly."""
        processor = DataProcessor()

        # Valid item
        valid_item = FeedbackItem(
            source="reddit",
            source_id="test123",
            source_url="https://reddit.com/test",
            author="testuser",
            title="Good feedback",
            content="This is a detailed piece of feedback with enough content to be meaningful.",
            timestamp=datetime.utcnow(),
            raw_data={},
        )

        # Invalid item (too short)
        invalid_item = FeedbackItem(
            source="reddit",
            source_id="test456",
            source_url="https://reddit.com/test2",
            author="testuser2",
            title="Short",
            content="Bad",
            timestamp=datetime.utcnow(),
            raw_data={},
        )

        assert processor._is_valid_feedback(valid_item)
        assert not processor._is_valid_feedback(invalid_item)

    def test_configuration_externalization(self):
        """Test that configuration can be loaded from environment."""
        import os
        from src.config import get_settings

        # Test with environment variables
        test_env = {
            "REDDIT_SUBREDDIT": "test_subreddit",
            "LLM_PROVIDER": "google",
            "MAX_POSTS_PER_RUN": "50",
            "LOG_LEVEL": "DEBUG",
        }

        with patch.dict(os.environ, test_env):
            settings = get_settings()
            assert settings.reddit_subreddit == "test_subreddit"
            assert settings.llm_provider == "google"
            assert settings.max_posts_per_run == 50
            assert settings.log_level == "DEBUG"

    def test_error_handling_in_processing(self):
        """Test error handling in data processing."""
        processor = DataProcessor()

        # Create an item that might cause processing issues
        problematic_item = FeedbackItem(
            source="reddit",
            source_id="test789",
            source_url="https://reddit.com/test3",
            author="",  # Empty author
            title=None,  # None title
            content="Some content here",
            timestamp=datetime.utcnow(),
            raw_data={},
        )

        # Processing should handle this gracefully
        result = processor.process_single_item(problematic_item)

        # Should still return a processed item with normalized data
        assert result is not None
        assert result.author == "anonymous"  # Should normalize empty author
        assert result.content == "Some content here"
