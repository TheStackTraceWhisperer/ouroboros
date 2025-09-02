"""Tests for data models."""

import pytest
from datetime import datetime
from uuid import uuid4

from src.models import FeedbackItem, SentimentType, TopicTag, SentimentAnalysis, TopicAnalysis


class TestFeedbackItem:
    """Test cases for FeedbackItem model."""
    
    def test_feedback_item_creation(self, sample_feedback_item):
        """Test creating a FeedbackItem."""
        item = sample_feedback_item
        
        assert item.source == "reddit"
        assert item.source_id == "test123"
        assert item.author == "testuser"
        assert item.content.startswith("This is a test feedback")
        assert isinstance(item.timestamp, datetime)
        assert item.raw_data["score"] == 5
    
    def test_feedback_item_json_serialization(self, sample_feedback_item):
        """Test JSON serialization of FeedbackItem."""
        item = sample_feedback_item
        
        # Should be able to convert to dict and back
        item_dict = item.model_dump()
        assert isinstance(item_dict, dict)
        assert item_dict["source"] == "reddit"
        
        # Should be able to create from dict
        new_item = FeedbackItem(**item_dict)
        assert new_item.id == item.id
        assert new_item.content == item.content


class TestSentimentAnalysis:
    """Test cases for SentimentAnalysis model."""
    
    def test_sentiment_analysis_creation(self):
        """Test creating a SentimentAnalysis."""
        analysis = SentimentAnalysis(
            sentiment=SentimentType.POSITIVE,
            confidence=0.85,
            reasoning="The text expresses satisfaction with the product."
        )
        
        assert analysis.sentiment == SentimentType.POSITIVE
        assert analysis.confidence == 0.85
        assert "satisfaction" in analysis.reasoning
    
    def test_sentiment_confidence_validation(self):
        """Test confidence score validation."""
        # Valid confidence
        analysis = SentimentAnalysis(
            sentiment=SentimentType.NEUTRAL,
            confidence=0.5
        )
        assert analysis.confidence == 0.5
        
        # Invalid confidence should raise error
        with pytest.raises(ValueError):
            SentimentAnalysis(
                sentiment=SentimentType.POSITIVE,
                confidence=1.5  # > 1.0
            )


class TestTopicAnalysis:
    """Test cases for TopicAnalysis model."""
    
    def test_topic_analysis_creation(self):
        """Test creating a TopicAnalysis."""
        analysis = TopicAnalysis(
            topics=[TopicTag.BUG, TopicTag.PERFORMANCE],
            confidence=0.9,
            reasoning="The text mentions crashes and slow performance."
        )
        
        assert len(analysis.topics) == 2
        assert TopicTag.BUG in analysis.topics
        assert TopicTag.PERFORMANCE in analysis.topics
        assert analysis.confidence == 0.9
    
    def test_empty_topics_list(self):
        """Test topic analysis with empty topics list."""
        analysis = TopicAnalysis(
            topics=[],
            confidence=0.1,
            reasoning="No clear topics identified."
        )
        
        assert len(analysis.topics) == 0
        assert analysis.confidence == 0.1