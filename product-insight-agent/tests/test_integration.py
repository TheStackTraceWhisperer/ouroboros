"""Integration tests for the Product Insight Agent pipeline."""

import json
import pytest
import tempfile
import os
from datetime import datetime
from pathlib import Path
from unittest.mock import patch, Mock

from src.main import ProductInsightAgent
from src.models import FeedbackItem, SentimentType, TopicTag


class TestProductInsightAgentIntegration:
    """Integration tests for the complete pipeline."""
    
    @pytest.fixture
    def sample_reddit_data(self):
        """Sample Reddit data for testing."""
        return [
            {
                "id": "test123",
                "title": "App crashes when opening settings",
                "selftext": "Every time I try to open the settings page, the app just crashes. This is really frustrating and makes the app unusable. Can you please fix this bug?",
                "author": "user123",
                "created_utc": datetime.utcnow().timestamp(),
                "score": 5,
                "upvote_ratio": 0.8,
                "permalink": "/r/test/comments/test123",
                "url": "https://reddit.com/r/test/comments/test123"
            },
            {
                "id": "test456", 
                "title": "Feature request: Dark mode",
                "selftext": "I would love to see a dark mode option in the app. It would be much easier on the eyes, especially when using the app at night. Many other apps have this feature.",
                "author": "user456",
                "created_utc": datetime.utcnow().timestamp(),
                "score": 15,
                "upvote_ratio": 0.95,
                "permalink": "/r/test/comments/test456",
                "url": "https://reddit.com/r/test/comments/test456"
            },
            {
                "id": "test789",
                "title": "Great app, love the new update!",
                "selftext": "The latest update is fantastic! The performance improvements are noticeable and the new features work great. Keep up the good work!",
                "author": "user789", 
                "created_utc": datetime.utcnow().timestamp(),
                "score": 25,
                "upvote_ratio": 0.98,
                "permalink": "/r/test/comments/test789",
                "url": "https://reddit.com/r/test/comments/test789"
            }
        ]
    
    @pytest.fixture
    def sample_data_file(self, sample_reddit_data):
        """Create a temporary JSON file with sample data."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as f:
            json.dump(sample_reddit_data, f)
            yield f.name
        os.unlink(f.name)
    
    @pytest.fixture
    def mock_agent_dependencies(self):
        """Mock external dependencies for integration testing."""
        with patch('src.main.settings') as mock_settings:
            # Configure mock settings
            mock_settings.database_url = "sqlite:///:memory:"
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = "test-key"
            mock_settings.anthropic_model = "claude-3-haiku-20240307"
            mock_settings.reddit_client_id = "test-id"
            mock_settings.reddit_client_secret = "test-secret"
            mock_settings.reddit_subreddit = "test"
            mock_settings.max_posts_per_run = 100
            mock_settings.days_to_analyze = 1
            
            # Mock Reddit connector
            with patch('src.main.RedditConnector') as mock_reddit:
                mock_reddit_instance = Mock()
                mock_reddit.return_value = mock_reddit_instance
                
                # Mock storage service
                with patch('src.main.StorageService') as mock_storage:
                    mock_storage_instance = Mock()
                    mock_storage.return_value = mock_storage_instance
                    
                    # Mock LLM service
                    with patch('src.main.LLMInsightService') as mock_llm:
                        mock_llm_instance = Mock()
                        mock_llm.return_value = mock_llm_instance
                        
                        yield {
                            'settings': mock_settings,
                            'reddit': mock_reddit_instance,
                            'storage': mock_storage_instance,
                            'llm': mock_llm_instance
                        }
    
    def test_end_to_end_pipeline_with_sample_data(self, sample_reddit_data, mock_agent_dependencies):
        """Test the complete pipeline from ingestion to analysis."""
        mocks = mock_agent_dependencies
        
        # Setup mock data flow
        feedback_items = []
        for data in sample_reddit_data:
            item = FeedbackItem(
                source="reddit",
                source_id=data["id"],
                source_url=data["url"],
                author=data["author"],
                title=data["title"],
                content=data["selftext"],
                timestamp=datetime.fromtimestamp(data["created_utc"]),
                raw_data=data
            )
            feedback_items.append(item)
        
        # Mock LLM service to return realistic analyses
        from src.models import InsightAnalysis, SentimentAnalysis, TopicAnalysis
        
        mock_analyses = [
            InsightAnalysis(
                feedback_id=feedback_items[0].id,
                sentiment_analysis=SentimentAnalysis(
                    sentiment=SentimentType.NEGATIVE,
                    confidence=0.9,
                    reasoning="User reports app crashes, expressing frustration"
                ),
                topic_analysis=TopicAnalysis(
                    topics=[TopicTag.BUG],
                    confidence=0.95,
                    reasoning="Clear bug report about app crashing"
                )
            ),
            InsightAnalysis(
                feedback_id=feedback_items[1].id,
                sentiment_analysis=SentimentAnalysis(
                    sentiment=SentimentType.NEUTRAL,
                    confidence=0.8,
                    reasoning="Polite feature request without strong emotion"
                ),
                topic_analysis=TopicAnalysis(
                    topics=[TopicTag.FEATURE_REQUEST, TopicTag.UI_FEEDBACK],
                    confidence=0.9,
                    reasoning="Requesting dark mode UI feature"
                )
            ),
            InsightAnalysis(
                feedback_id=feedback_items[2].id,
                sentiment_analysis=SentimentAnalysis(
                    sentiment=SentimentType.POSITIVE,
                    confidence=0.95,
                    reasoning="Expressing love and satisfaction with update"
                ),
                topic_analysis=TopicAnalysis(
                    topics=[TopicTag.PERFORMANCE, TopicTag.GENERAL],
                    confidence=0.85,
                    reasoning="Praising performance improvements and general features"
                )
            )
        ]
        
        # Mock data processor to return processed feedback items
        with patch('src.main.DataProcessor') as mock_data_processor:
            mock_processor_instance = Mock()
            mock_processor_instance.process_feedback_items.return_value = feedback_items
            mock_data_processor.return_value = mock_processor_instance
            
            # Mock Reddit raw data format
            raw_reddit_data = [
                {
                    'id': data['id'],
                    'title': data['title'],
                    'content': data['selftext'],
                    'author': data['author'],
                    'timestamp': datetime.fromtimestamp(data['created_utc']),
                    'url': data['url'],
                    'raw_data': data
                }
                for data in sample_reddit_data
            ]
            
            mocks['reddit'].ingest_posts_since.return_value = raw_reddit_data
            mocks['llm'].analyze_feedback_items.return_value = mock_analyses
            mocks['storage'].get_unanalyzed_feedback_items.return_value = []
            
            # Create agent and run pipeline
            agent = ProductInsightAgent()
            
            # Test individual components
            # 1. Data ingestion
            ingested_items = agent.ingest_data()
            assert len(ingested_items) > 0
            
            # Verify Reddit connector was called
            mocks['reddit'].ingest_posts_since.assert_called_once()
            
            # Verify storage was called to store feedback items
            mocks['storage'].store_feedback_items.assert_called_once()
            
            # 2. Analysis
            analyses = agent.analyze_feedback()
            assert len(analyses) == len(mock_analyses)
            
            # Verify LLM service was called
            mocks['llm'].analyze_feedback_items.assert_called_once()
            
            # Verify storage was called to store analyses
            mocks['storage'].store_insight_analyses.assert_called_once()
            
            # 3. Report generation
            summary = agent.generate_report()
            assert summary is not None
    
    def test_data_validation_and_processing(self, sample_reddit_data):
        """Test that data processing validates and cleans input correctly."""
        from src.processing.data_processor import DataProcessor
        
        processor = DataProcessor()
        
        # Convert sample data to FeedbackItem objects
        feedback_items = []
        for data in sample_reddit_data:
            item = FeedbackItem(
                source="reddit",
                source_id=data["id"],
                source_url=data["url"],
                author=data["author"],
                title=data["title"],
                content=data["selftext"],
                timestamp=datetime.fromtimestamp(data["created_utc"]),
                raw_data=data
            )
            feedback_items.append(item)
        
        # Process the items
        processed_items = processor.process_feedback_items(feedback_items)
        
        # Verify all items were processed
        assert len(processed_items) == len(feedback_items)
        
        # Verify data cleaning occurred
        for item in processed_items:
            assert item.content is not None
            assert len(item.content.strip()) > 0
            assert item.author is not None
            assert item.timestamp is not None
    
    def test_sentiment_analysis_accuracy(self, sample_reddit_data):
        """Test that sentiment analysis produces expected results for known data."""
        # This test would require actual LLM responses, so we'll mock them
        # but verify the structure and logic
        
        from src.insights.llm_service import LLMInsightService
        
        with patch('src.insights.llm_service.settings') as mock_settings:
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = "test-key"
            mock_settings.anthropic_model = "claude-3-haiku-20240307"
            
            with patch('src.insights.llm_service.anthropic.Anthropic'):
                service = LLMInsightService()
                
                # Mock responses for each type of feedback
                mock_responses = [
                    '{"sentiment": "negative", "confidence": 0.9, "reasoning": "User reports crashes and frustration"}',
                    '{"sentiment": "neutral", "confidence": 0.8, "reasoning": "Polite feature request"}',
                    '{"sentiment": "positive", "confidence": 0.95, "reasoning": "Expressing satisfaction and praise"}'
                ]
                
                with patch.object(service, '_call_llm', side_effect=mock_responses):
                    # Test each piece of sample content
                    bug_content = sample_reddit_data[0]["selftext"]
                    feature_content = sample_reddit_data[1]["selftext"]
                    positive_content = sample_reddit_data[2]["selftext"]
                    
                    bug_sentiment = service._analyze_sentiment(bug_content)
                    feature_sentiment = service._analyze_sentiment(feature_content)
                    positive_sentiment = service._analyze_sentiment(positive_content)
                    
                    # Verify expected sentiments
                    assert bug_sentiment.sentiment == SentimentType.NEGATIVE
                    assert feature_sentiment.sentiment == SentimentType.NEUTRAL
                    assert positive_sentiment.sentiment == SentimentType.POSITIVE
                    
                    # Verify confidence levels are reasonable
                    assert bug_sentiment.confidence >= 0.8
                    assert feature_sentiment.confidence >= 0.7
                    assert positive_sentiment.confidence >= 0.9
    
    def test_topic_classification_accuracy(self, sample_reddit_data):
        """Test that topic classification works correctly for known data."""
        from src.insights.llm_service import LLMInsightService
        
        with patch('src.insights.llm_service.settings') as mock_settings:
            mock_settings.llm_provider = "anthropic"
            mock_settings.anthropic_api_key = "test-key"
            mock_settings.anthropic_model = "claude-3-haiku-20240307"
            
            with patch('src.insights.llm_service.anthropic.Anthropic'):
                service = LLMInsightService()
                
                # Mock responses for topic analysis
                mock_responses = [
                    '{"topics": ["bug"], "confidence": 0.95, "reasoning": "Clear bug report about crashes"}',
                    '{"topics": ["feature-request", "ui-feedback"], "confidence": 0.9, "reasoning": "Requesting UI feature"}',
                    '{"topics": ["performance", "general"], "confidence": 0.85, "reasoning": "Performance praise and general feedback"}'
                ]
                
                with patch.object(service, '_call_llm', side_effect=mock_responses):
                    bug_content = sample_reddit_data[0]["selftext"]
                    feature_content = sample_reddit_data[1]["selftext"]
                    positive_content = sample_reddit_data[2]["selftext"]
                    
                    bug_topics = service._analyze_topics(bug_content)
                    feature_topics = service._analyze_topics(feature_content)
                    positive_topics = service._analyze_topics(positive_content)
                    
                    # Verify expected topics
                    assert TopicTag.BUG in bug_topics.topics
                    assert TopicTag.FEATURE_REQUEST in feature_topics.topics
                    assert TopicTag.UI_FEEDBACK in feature_topics.topics
                    assert TopicTag.PERFORMANCE in positive_topics.topics
    
    def test_pipeline_error_handling(self, mock_agent_dependencies):
        """Test that the pipeline handles errors gracefully."""
        mocks = mock_agent_dependencies
        
        # Simulate Reddit API failure
        mocks['reddit'].ingest_posts_since.side_effect = Exception("Reddit API Error")
        
        agent = ProductInsightAgent()
        
        # Should not crash, should return empty list
        result = agent.ingest_data()
        assert result == []
        
        # Simulate LLM API failure
        mocks['reddit'].ingest_posts_since.side_effect = None
        mocks['reddit'].ingest_posts_since.return_value = []
        mocks['llm'].analyze_feedback_items.side_effect = Exception("LLM API Error")
        
        # Should not crash, should return empty list
        result = agent.analyze_feedback()
        assert result == []
    
    def test_database_operations(self, mock_agent_dependencies):
        """Test that database operations work correctly."""
        mocks = mock_agent_dependencies
        
        # Test that storage methods are called with correct parameters
        
        # Mock some feedback items
        from src.models import FeedbackItem
        feedback_items = [
            FeedbackItem(
                source="reddit",
                source_id="test1",
                source_url="https://test.com/1",
                author="user1",
                title="Test 1",
                content="Test content 1",
                timestamp=datetime.utcnow(),
                raw_data={}
            )
        ]
        
        # Mock data processor to return items
        with patch('src.main.DataProcessor') as mock_processor:
            mock_processor_instance = Mock()
            mock_processor_instance.process_feedback_items.return_value = feedback_items
            mock_processor.return_value = mock_processor_instance
            
            mocks['reddit'].ingest_posts_since.return_value = [{"test": "data"}]
            
            agent = ProductInsightAgent()
            
            # Run ingestion
            agent.ingest_data()
            
            # Verify storage was called
            mocks['storage'].store_feedback_items.assert_called_once_with(feedback_items)
    
    def test_configuration_loading(self):
        """Test that configuration is loaded correctly from environment."""
        from src.config import get_settings
        
        # Test with environment variables
        with patch.dict(os.environ, {
            'REDDIT_SUBREDDIT': 'test_subreddit',
            'LLM_PROVIDER': 'google',
            'MAX_POSTS_PER_RUN': '50'
        }):
            settings = get_settings()
            assert settings.reddit_subreddit == 'test_subreddit'
            assert settings.llm_provider == 'google'
            assert settings.max_posts_per_run == 50