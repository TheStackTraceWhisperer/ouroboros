"""Tests for LLM insight analysis functionality."""

import json
import pytest
from unittest.mock import Mock, patch, MagicMock
from datetime import datetime
from uuid import uuid4

from src.insights.llm_service import LLMInsightService
from src.models import (
    FeedbackItem,
    SentimentAnalysis,
    TopicAnalysis,
    InsightAnalysis,
    SentimentType,
    TopicTag,
)


class TestLLMInsightService:
    """Test cases for LLMInsightService."""

    def setup_method(self):
        """Set up test instance with mocked configuration."""
        with patch("src.insights.llm_service.settings") as mock_settings:
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = "test-key"
            mock_settings.anthropic_model = "claude-3-haiku-20240307"
            mock_settings.google_api_key = None

            with patch("src.insights.llm_service.anthropic.Anthropic"):
                self.service = LLMInsightService()

    def test_init_anthropic_provider(self):
        """Test initialization with Anthropic provider."""
        with patch("src.insights.llm_service.settings") as mock_settings:
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = "test-key"
            mock_settings.anthropic_model = "claude-3-haiku-20240307"

            with patch(
                "src.insights.llm_service.anthropic.Anthropic"
            ) as mock_anthropic:
                service = LLMInsightService()
                assert service.provider == "anthropic"
                assert service.model == "claude-3-haiku-20240307"
                mock_anthropic.assert_called_once_with(api_key="test-key")

    def test_init_google_provider(self):
        """Test initialization with Google provider."""
        with patch("src.insights.llm_service.settings") as mock_settings:
            mock_settings.llm_provider = "google"
            mock_settings.google_api_key = "test-key"
            mock_settings.google_model = "gemini-1.5-flash"
            mock_settings.anthropic_api_key = None

            with patch("src.insights.llm_service.genai") as mock_genai:
                service = LLMInsightService()
                assert service.provider == "google"
                assert service.model == "gemini-1.5-flash"
                mock_genai.configure.assert_called_once_with(api_key="test-key")

    def test_init_missing_api_key(self):
        """Test initialization fails when API key is missing."""
        with patch("src.insights.llm_service.settings") as mock_settings:
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = None

            with pytest.raises(ValueError, match="Anthropic API key is required"):
                LLMInsightService()

    def test_init_unsupported_provider(self):
        """Test initialization fails with unsupported provider."""
        with patch("src.insights.llm_service.settings") as mock_settings:
            mock_settings.llm_provider = "invalid"

            with pytest.raises(ValueError, match="Unsupported LLM provider"):
                LLMInsightService()

    def test_prepare_content(self, sample_feedback_item):
        """Test content preparation for LLM analysis."""
        content = self.service._prepare_content(sample_feedback_item)

        assert "Title: Test feedback title" in content
        assert "Content: This is a test feedback content" in content

    def test_prepare_content_no_title(self, sample_feedback_item):
        """Test content preparation when title is missing."""
        sample_feedback_item.title = None
        content = self.service._prepare_content(sample_feedback_item)

        assert "Title:" not in content
        assert "Content: This is a test feedback content" in content

    def test_analyze_sentiment_success(self):
        """Test successful sentiment analysis."""
        mock_response = json.dumps(
            {
                "sentiment": "positive",
                "confidence": 0.85,
                "reasoning": "The feedback expresses satisfaction",
            }
        )

        with patch.object(self.service, "_call_llm", return_value=mock_response):
            result = self.service._analyze_sentiment("Great app!")

            assert isinstance(result, SentimentAnalysis)
            assert result.sentiment == SentimentType.POSITIVE
            assert result.confidence == 0.85
            assert "satisfaction" in result.reasoning

    def test_analyze_sentiment_invalid_json(self):
        """Test sentiment analysis with invalid JSON response."""
        with patch.object(self.service, "_call_llm", return_value="invalid json"):
            result = self.service._analyze_sentiment("Test content")

            assert result.sentiment == SentimentType.NEUTRAL
            assert result.confidence == 0.1
            assert "Analysis failed" in result.reasoning

    def test_analyze_sentiment_api_error(self):
        """Test sentiment analysis with API error."""
        with patch.object(
            self.service, "_call_llm", side_effect=Exception("API Error")
        ):
            result = self.service._analyze_sentiment("Test content")

            assert result.sentiment == SentimentType.NEUTRAL
            assert result.confidence == 0.1
            assert "Analysis failed" in result.reasoning

    def test_analyze_topics_success(self):
        """Test successful topic analysis."""
        mock_response = json.dumps(
            {
                "topics": ["bug", "performance"],
                "confidence": 0.9,
                "reasoning": "User reports a slow loading issue",
            }
        )

        with patch.object(self.service, "_call_llm", return_value=mock_response):
            result = self.service._analyze_topics("The app is loading slowly")

            assert isinstance(result, TopicAnalysis)
            assert TopicTag.BUG in result.topics
            assert TopicTag.PERFORMANCE in result.topics
            assert result.confidence == 0.9
            assert "slow loading" in result.reasoning

    def test_analyze_topics_invalid_topic(self):
        """Test topic analysis with invalid topic."""
        mock_response = json.dumps(
            {
                "topics": ["bug", "invalid-topic"],
                "confidence": 0.8,
                "reasoning": "Bug report",
            }
        )

        with patch.object(self.service, "_call_llm", return_value=mock_response):
            result = self.service._analyze_topics("Bug report")

            assert TopicTag.BUG in result.topics
            assert TopicTag.GENERAL not in result.topics or len(result.topics) == 1

    def test_analyze_topics_no_valid_topics(self):
        """Test topic analysis with no valid topics."""
        mock_response = json.dumps(
            {
                "topics": ["invalid1", "invalid2"],
                "confidence": 0.8,
                "reasoning": "Invalid topics",
            }
        )

        with patch.object(self.service, "_call_llm", return_value=mock_response):
            result = self.service._analyze_topics("Test content")

            assert result.topics == [TopicTag.GENERAL]

    def test_analyze_topics_error(self):
        """Test topic analysis with error."""
        with patch.object(
            self.service, "_call_llm", side_effect=Exception("API Error")
        ):
            result = self.service._analyze_topics("Test content")

            assert result.topics == [TopicTag.GENERAL]
            assert result.confidence == 0.1
            assert "Analysis failed" in result.reasoning

    def test_analyze_single_item_success(self, sample_feedback_item):
        """Test successful single item analysis."""
        mock_sentiment = SentimentAnalysis(
            sentiment=SentimentType.POSITIVE,
            confidence=0.8,
            reasoning="Positive feedback",
        )
        mock_topics = TopicAnalysis(
            topics=[TopicTag.FEATURE_REQUEST],
            confidence=0.9,
            reasoning="Feature request detected",
        )

        with patch.object(
            self.service, "_analyze_sentiment", return_value=mock_sentiment
        ):
            with patch.object(
                self.service, "_analyze_topics", return_value=mock_topics
            ):
                result = self.service.analyze_single_item(sample_feedback_item)

                assert isinstance(result, InsightAnalysis)
                assert result.feedback_id == sample_feedback_item.id
                assert result.sentiment_analysis == mock_sentiment
                assert result.topic_analysis == mock_topics

    def test_analyze_single_item_error(self, sample_feedback_item):
        """Test single item analysis with error."""
        with patch.object(
            self.service, "_prepare_content", side_effect=Exception("Error")
        ):
            result = self.service.analyze_single_item(sample_feedback_item)
            assert result is None

    def test_analyze_feedback_items_success(self, sample_feedback_items):
        """Test successful analysis of multiple feedback items."""
        mock_analysis = InsightAnalysis(
            feedback_id=sample_feedback_items[0].id,
            sentiment_analysis=SentimentAnalysis(
                sentiment=SentimentType.POSITIVE, confidence=0.8, reasoning="Test"
            ),
            topic_analysis=TopicAnalysis(
                topics=[TopicTag.GENERAL], confidence=0.7, reasoning="Test"
            ),
        )

        with patch.object(
            self.service, "analyze_single_item", return_value=mock_analysis
        ):
            results = self.service.analyze_feedback_items(sample_feedback_items)

            assert len(results) == len(sample_feedback_items)
            assert all(isinstance(r, InsightAnalysis) for r in results)

    def test_analyze_feedback_items_with_failures(self, sample_feedback_items):
        """Test analysis with some items failing."""

        def mock_analyze(item):
            if item == sample_feedback_items[1]:
                return None  # Simulate failure
            return InsightAnalysis(
                feedback_id=item.id,
                sentiment_analysis=SentimentAnalysis(
                    sentiment=SentimentType.NEUTRAL, confidence=0.5, reasoning="Test"
                ),
                topic_analysis=TopicAnalysis(
                    topics=[TopicTag.GENERAL], confidence=0.5, reasoning="Test"
                ),
            )

        with patch.object(
            self.service, "analyze_single_item", side_effect=mock_analyze
        ):
            results = self.service.analyze_feedback_items(sample_feedback_items)

            # Should have 2 successful analyses (3 items - 1 failure)
            assert len(results) == 2

    def test_call_anthropic_success(self):
        """Test successful Anthropic API call."""
        mock_response = Mock()
        mock_response.content = [Mock(text="Test response")]

        with patch.object(self.service, "anthropic_client") as mock_client:
            mock_client.messages.create.return_value = mock_response

            result = self.service._call_anthropic("test prompt")
            assert result == "Test response"

            mock_client.messages.create.assert_called_once_with(
                model=self.service.model,
                max_tokens=1000,
                temperature=0.1,
                messages=[{"role": "user", "content": "test prompt"}],
            )

    def test_call_anthropic_error(self):
        """Test Anthropic API call with error."""
        with patch.object(self.service, "anthropic_client") as mock_client:
            mock_client.messages.create.side_effect = Exception("API Error")

            with pytest.raises(Exception, match="API Error"):
                self.service._call_anthropic("test prompt")

    def test_call_google_success(self):
        """Test successful Google API call."""
        mock_response = Mock()
        mock_response.text = "Test response"

        with patch("src.insights.llm_service.genai") as mock_genai:
            mock_model = Mock()
            mock_model.generate_content.return_value = mock_response
            mock_genai.GenerativeModel.return_value = mock_model

            # Set up service for Google
            self.service.provider = "google"

            result = self.service._call_google("test prompt")
            assert result == "Test response"

    def test_call_google_error(self):
        """Test Google API call with error."""
        with patch("src.insights.llm_service.genai") as mock_genai:
            mock_genai.GenerativeModel.side_effect = Exception("API Error")

            # Set up service for Google
            self.service.provider = "google"

            with pytest.raises(Exception, match="API Error"):
                self.service._call_google("test prompt")

    def test_test_connection_success(self):
        """Test successful connection test."""
        with patch.object(
            self.service, "_call_llm", return_value="Success! I can see this message."
        ):
            assert self.service.test_connection() is True

    def test_test_connection_failure(self):
        """Test failed connection test."""
        with patch.object(
            self.service, "_call_llm", side_effect=Exception("Connection failed")
        ):
            assert self.service.test_connection() is False

    def test_test_connection_no_success_word(self):
        """Test connection test without success word in response."""
        with patch.object(
            self.service, "_call_llm", return_value="This is a different response"
        ):
            assert self.service.test_connection() is False
