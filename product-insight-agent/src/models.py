"""
Data models for the Product Insight Agent.

This module defines the core data structures used throughout the agent,
including the FeedbackItem and analysis results.
"""

from datetime import datetime
from enum import Enum
from typing import List, Optional
from uuid import UUID, uuid4

from pydantic import BaseModel, Field


class SentimentType(str, Enum):
    """Sentiment classification options."""

    POSITIVE = "positive"
    NEGATIVE = "negative"
    NEUTRAL = "neutral"


class TopicTag(str, Enum):
    """Primary topic categories for feedback classification."""

    BUG = "bug"
    FEATURE_REQUEST = "feature-request"
    UI_FEEDBACK = "ui-feedback"
    PERFORMANCE = "performance"
    DOCUMENTATION = "documentation"
    SUPPORT = "support"
    GENERAL = "general"


class FeedbackItem(BaseModel):
    """
    Standardized structure for normalized user feedback data.

    This is the core data structure that all ingested feedback
    gets converted into for consistent processing.
    """

    id: UUID = Field(default_factory=uuid4)
    source: str = Field(..., description="Source platform (e.g., 'reddit', 'twitter')")
    source_id: str = Field(..., description="Original ID from source platform")
    source_url: Optional[str] = Field(None, description="URL to original post/comment")
    author: str = Field(..., description="Username/author identifier")
    title: Optional[str] = Field(None, description="Post title if applicable")
    content: str = Field(..., description="Main text content")
    timestamp: datetime = Field(
        ..., description="When the feedback was originally posted"
    )
    ingested_at: datetime = Field(default_factory=datetime.utcnow)

    # Raw data preservation
    raw_data: dict = Field(
        default_factory=dict, description="Original raw data from API"
    )

    # Computed fields (populated during analysis)
    processed_at: Optional[datetime] = None
    sentiment: Optional[SentimentType] = None
    topics: List[TopicTag] = Field(default_factory=list)
    confidence_score: Optional[float] = Field(None, ge=0.0, le=1.0)

    class Config:
        """Pydantic configuration."""

        use_enum_values = True
        json_encoders = {datetime: lambda v: v.isoformat(), UUID: lambda v: str(v)}


class SentimentAnalysis(BaseModel):
    """Result of sentiment analysis."""

    sentiment: SentimentType
    confidence: float = Field(..., ge=0.0, le=1.0)
    reasoning: Optional[str] = None


class TopicAnalysis(BaseModel):
    """Result of topic extraction."""

    topics: List[TopicTag]
    confidence: float = Field(..., ge=0.0, le=1.0)
    reasoning: Optional[str] = None


class InsightAnalysis(BaseModel):
    """Combined analysis results for a feedback item."""

    feedback_id: UUID
    sentiment_analysis: SentimentAnalysis
    topic_analysis: TopicAnalysis
    analyzed_at: datetime = Field(default_factory=datetime.utcnow)


class DailySummary(BaseModel):
    """Daily summary report structure."""

    id: UUID = Field(default_factory=uuid4)
    date: datetime
    total_feedback_items: int
    sentiment_breakdown: dict[SentimentType, int]
    topic_breakdown: dict[TopicTag, int]
    key_insights: List[str]
    summary_text: str
    generated_at: datetime = Field(default_factory=datetime.utcnow)


class GoalProposalStatus(str, Enum):
    """Status options for goal proposals."""

    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"


class TrendType(str, Enum):
    """Types of trends that can be detected."""

    SENTIMENT_SHIFT = "sentiment_shift"
    TOPIC_CLUSTER = "topic_cluster"
    VOLUME_SPIKE = "volume_spike"
    RECURRING_ISSUE = "recurring_issue"


class TrendAnalysis(BaseModel):
    """Analysis results for detected trends."""

    trend_type: TrendType
    confidence: float = Field(..., ge=0.0, le=1.0)
    affected_feedback_count: int
    primary_topics: List[TopicTag]
    sentiment_distribution: dict[SentimentType, int]
    key_indicators: List[str]
    time_period: str
    severity_score: float = Field(..., ge=0.0, le=1.0)


class GoalProposal(BaseModel):
    """A proposed goal generated from trend analysis."""

    id: UUID = Field(default_factory=uuid4)
    title: str = Field(..., min_length=10, max_length=200)
    description: str = Field(..., min_length=50)
    status: GoalProposalStatus = Field(default=GoalProposalStatus.PENDING)
    priority: int = Field(
        ..., ge=1, le=5, description="Priority from 1 (low) to 5 (high)"
    )

    # Source trend information
    source_trend: TrendAnalysis
    supporting_feedback_ids: List[UUID]

    # Metadata
    created_at: datetime = Field(default_factory=datetime.utcnow)
    created_by: str = Field(default="product_insight_agent")
    tags: List[str] = Field(default_factory=list)
    estimated_effort: Optional[str] = None
    potential_impact: Optional[str] = None
    summary_text: str
