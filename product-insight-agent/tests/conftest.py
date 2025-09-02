"""Test configuration and fixtures for the Product Insight Agent."""

import pytest
from datetime import datetime
from uuid import uuid4

from src.models import FeedbackItem, SentimentType, TopicTag


@pytest.fixture
def sample_feedback_item():
    """Create a sample FeedbackItem for testing."""
    return FeedbackItem(
        id=uuid4(),
        source="reddit",
        source_id="test123",
        source_url="https://reddit.com/r/test/comments/test123",
        author="testuser",
        title="Test feedback title",
        content="This is a test feedback content with some meaningful text for analysis.",
        timestamp=datetime.utcnow(),
        raw_data={"score": 5, "upvote_ratio": 0.8}
    )


@pytest.fixture
def sample_feedback_items():
    """Create multiple sample FeedbackItem objects for testing."""
    items = []
    for i in range(3):
        items.append(FeedbackItem(
            id=uuid4(),
            source="reddit",
            source_id=f"test{i}",
            source_url=f"https://reddit.com/r/test/comments/test{i}",
            author=f"testuser{i}",
            title=f"Test feedback title {i}",
            content=f"This is test feedback content {i} with meaningful text.",
            timestamp=datetime.utcnow(),
            raw_data={"score": i + 1, "upvote_ratio": 0.8}
        ))
    return items