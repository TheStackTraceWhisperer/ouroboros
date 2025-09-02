"""
SQLAlchemy database models for the Product Insight Agent.

This module defines the database schema using SQLAlchemy ORM models
that correspond to the Pydantic models used throughout the application.
"""

import json
from datetime import datetime, date
from typing import List, Dict, Any
from uuid import UUID

from sqlalchemy import (
    Column,
    String,
    Text,
    DateTime,
    Date,
    Integer,
    Float,
    JSON,
    Enum as SQLEnum,
    ForeignKey,
)
from sqlalchemy.dialects.postgresql import UUID as PostgreSQL_UUID
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from sqlalchemy.types import TypeDecorator, CHAR

from ..config import settings
from ..models import (
    FeedbackItem,
    InsightAnalysis,
    DailySummary,
    SentimentType,
    TopicTag,
    SentimentAnalysis,
    TopicAnalysis,
    GoalProposal,
    GoalProposalStatus,
    TrendAnalysis,
    TrendType,
)

# Create base class with schema support
if "postgresql" in settings.database_url and settings.database_schema != "public":
    Base = declarative_base()
    Base.metadata.schema = settings.database_schema
else:
    Base = declarative_base()


class GUID(TypeDecorator):
    """Platform-independent GUID type.

    Uses PostgreSQL's UUID type when available, otherwise uses CHAR(36).
    """

    impl = CHAR
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            return dialect.type_descriptor(PostgreSQL_UUID())
        else:
            return dialect.type_descriptor(CHAR(36))

    def process_bind_param(self, value, dialect):
        if value is None:
            return value
        elif dialect.name == "postgresql":
            return str(value)
        else:
            if not isinstance(value, UUID):
                return str(value)
            return str(value)

    def process_result_value(self, value, dialect):
        if value is None:
            return value
        else:
            if not isinstance(value, UUID):
                return UUID(value)
            return value


class FeedbackItemDB(Base):
    """Database model for feedback items."""

    __tablename__ = "feedback_items"

    id = Column(GUID(), primary_key=True)
    source = Column(String(50), nullable=False, index=True)
    source_id = Column(String(100), nullable=False)
    source_url = Column(Text)
    author = Column(String(200), nullable=False)
    title = Column(Text)
    content = Column(Text, nullable=False)
    timestamp = Column(DateTime, nullable=False, index=True)
    ingested_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    raw_data = Column(JSON)

    # Computed fields
    processed_at = Column(DateTime)
    sentiment = Column(SQLEnum(SentimentType))
    topics = Column(JSON)  # List of TopicTag values
    confidence_score = Column(Float)

    # Relationships
    analyses = relationship("InsightAnalysisDB", back_populates="feedback_item")

    def to_pydantic(self) -> FeedbackItem:
        """Convert to Pydantic model."""
        topics_list = []
        if self.topics:
            topics_list = [
                TopicTag(topic)
                for topic in self.topics
                if topic in [t.value for t in TopicTag]
            ]

        return FeedbackItem(
            id=self.id,
            source=self.source,
            source_id=self.source_id,
            source_url=self.source_url,
            author=self.author,
            title=self.title,
            content=self.content,
            timestamp=self.timestamp,
            ingested_at=self.ingested_at,
            raw_data=self.raw_data or {},
            processed_at=self.processed_at,
            sentiment=self.sentiment,
            topics=topics_list,
            confidence_score=self.confidence_score,
        )

    @classmethod
    def from_pydantic(cls, item: FeedbackItem) -> "FeedbackItemDB":
        """Create from Pydantic model."""
        return cls(
            id=item.id,
            source=item.source,
            source_id=item.source_id,
            source_url=item.source_url,
            author=item.author,
            title=item.title,
            content=item.content,
            timestamp=item.timestamp,
            ingested_at=item.ingested_at,
            raw_data=item.raw_data,
            processed_at=item.processed_at,
            sentiment=item.sentiment,
            topics=[topic.value for topic in item.topics] if item.topics else None,
            confidence_score=item.confidence_score,
        )


class InsightAnalysisDB(Base):
    """Database model for insight analyses."""

    __tablename__ = "insight_analyses"

    id = Column(GUID(), primary_key=True)
    feedback_id = Column(
        GUID(),
        ForeignKey(f"{FeedbackItemDB.__tablename__}.id"),
        nullable=False,
        index=True,
    )
    analyzed_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)

    # Sentiment analysis fields
    sentiment = Column(SQLEnum(SentimentType), nullable=False)
    sentiment_confidence = Column(Float, nullable=False)
    sentiment_reasoning = Column(Text)

    # Topic analysis fields
    topics = Column(JSON, nullable=False)  # List of TopicTag values
    topic_confidence = Column(Float, nullable=False)
    topic_reasoning = Column(Text)

    # Relationships
    feedback_item = relationship("FeedbackItemDB", back_populates="analyses")

    def to_pydantic(self) -> InsightAnalysis:
        """Convert to Pydantic model."""
        topics_list = []
        if self.topics:
            topics_list = [
                TopicTag(topic)
                for topic in self.topics
                if topic in [t.value for t in TopicTag]
            ]

        sentiment_analysis = SentimentAnalysis(
            sentiment=self.sentiment,
            confidence=self.sentiment_confidence,
            reasoning=self.sentiment_reasoning,
        )

        topic_analysis = TopicAnalysis(
            topics=topics_list,
            confidence=self.topic_confidence,
            reasoning=self.topic_reasoning,
        )

        return InsightAnalysis(
            feedback_id=self.feedback_id,
            sentiment_analysis=sentiment_analysis,
            topic_analysis=topic_analysis,
            analyzed_at=self.analyzed_at,
        )

    @classmethod
    def from_pydantic(cls, analysis: InsightAnalysis) -> "InsightAnalysisDB":
        """Create from Pydantic model."""
        return cls(
            feedback_id=analysis.feedback_id,
            analyzed_at=analysis.analyzed_at,
            sentiment=analysis.sentiment_analysis.sentiment,
            sentiment_confidence=analysis.sentiment_analysis.confidence,
            sentiment_reasoning=analysis.sentiment_analysis.reasoning,
            topics=[topic.value for topic in analysis.topic_analysis.topics],
            topic_confidence=analysis.topic_analysis.confidence,
            topic_reasoning=analysis.topic_analysis.reasoning,
        )

    def update_from_pydantic(self, analysis: InsightAnalysis):
        """Update from Pydantic model."""
        self.analyzed_at = analysis.analyzed_at
        self.sentiment = analysis.sentiment_analysis.sentiment
        self.sentiment_confidence = analysis.sentiment_analysis.confidence
        self.sentiment_reasoning = analysis.sentiment_analysis.reasoning
        self.topics = [topic.value for topic in analysis.topic_analysis.topics]
        self.topic_confidence = analysis.topic_analysis.confidence
        self.topic_reasoning = analysis.topic_analysis.reasoning


class DailySummaryDB(Base):
    """Database model for daily summaries."""

    __tablename__ = "daily_summaries"

    id = Column(GUID(), primary_key=True)
    date = Column(Date, nullable=False, unique=True, index=True)
    total_feedback_items = Column(Integer, nullable=False)
    sentiment_breakdown = Column(JSON, nullable=False)
    topic_breakdown = Column(JSON, nullable=False)
    key_insights = Column(JSON, nullable=False)
    summary_text = Column(Text, nullable=False)
    generated_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def to_pydantic(self) -> DailySummary:
        """Convert to Pydantic model."""
        # Convert date to datetime for consistency
        date_datetime = datetime.combine(self.date, datetime.min.time())

        # Parse sentiment breakdown
        sentiment_breakdown = {}
        for sentiment_str, count in self.sentiment_breakdown.items():
            try:
                sentiment_breakdown[SentimentType(sentiment_str)] = count
            except ValueError:
                continue

        # Parse topic breakdown
        topic_breakdown = {}
        for topic_str, count in self.topic_breakdown.items():
            try:
                topic_breakdown[TopicTag(topic_str)] = count
            except ValueError:
                continue

        return DailySummary(
            id=self.id,
            date=date_datetime,
            total_feedback_items=self.total_feedback_items,
            sentiment_breakdown=sentiment_breakdown,
            topic_breakdown=topic_breakdown,
            key_insights=self.key_insights,
            summary_text=self.summary_text,
            generated_at=self.generated_at,
        )

    @classmethod
    def from_pydantic(cls, summary: DailySummary) -> "DailySummaryDB":
        """Create from Pydantic model."""
        # Convert sentiment breakdown to JSON-serializable format
        sentiment_breakdown = {
            sentiment.value: count
            for sentiment, count in summary.sentiment_breakdown.items()
        }

        # Convert topic breakdown to JSON-serializable format
        topic_breakdown = {
            topic.value: count for topic, count in summary.topic_breakdown.items()
        }

        return cls(
            id=summary.id,
            date=summary.date.date(),
            total_feedback_items=summary.total_feedback_items,
            sentiment_breakdown=sentiment_breakdown,
            topic_breakdown=topic_breakdown,
            key_insights=summary.key_insights,
            summary_text=summary.summary_text,
            generated_at=summary.generated_at,
        )

    def update_from_pydantic(self, summary: DailySummary):
        """Update from Pydantic model."""
        sentiment_breakdown = {
            sentiment.value: count
            for sentiment, count in summary.sentiment_breakdown.items()
        }

        topic_breakdown = {
            topic.value: count for topic, count in summary.topic_breakdown.items()
        }

        self.total_feedback_items = summary.total_feedback_items
        self.sentiment_breakdown = sentiment_breakdown
        self.topic_breakdown = topic_breakdown
        self.key_insights = summary.key_insights
        self.summary_text = summary.summary_text
        self.generated_at = summary.generated_at


class GoalProposalDB(Base):
    """Database model for goal proposals."""

    __tablename__ = "goal_proposals"

    id = Column(GUID(), primary_key=True)
    title = Column(String(200), nullable=False)
    description = Column(Text, nullable=False)
    status = Column(
        SQLEnum(GoalProposalStatus),
        nullable=False,
        default=GoalProposalStatus.PENDING,
        index=True,
    )
    priority = Column(Integer, nullable=False)

    # Source trend information
    trend_type = Column(SQLEnum(TrendType), nullable=False)
    trend_confidence = Column(Float, nullable=False)
    affected_feedback_count = Column(Integer, nullable=False)
    primary_topics = Column(JSON, nullable=False)  # List of TopicTag values
    sentiment_distribution = Column(JSON, nullable=False)  # Dict of sentiment counts
    key_indicators = Column(JSON, nullable=False)  # List of key indicators
    time_period = Column(String(100), nullable=False)
    severity_score = Column(Float, nullable=False)

    # Supporting data
    supporting_feedback_ids = Column(JSON, nullable=False)  # List of UUID strings

    # Metadata
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
    created_by = Column(String(100), nullable=False, default="product_insight_agent")
    tags = Column(JSON)  # List of tag strings
    estimated_effort = Column(Text)
    potential_impact = Column(Text)

    def to_pydantic(self) -> GoalProposal:
        """Convert to Pydantic model."""
        # Parse primary topics
        primary_topics = []
        if self.primary_topics:
            primary_topics = [
                TopicTag(topic)
                for topic in self.primary_topics
                if topic in [t.value for t in TopicTag]
            ]

        # Parse sentiment distribution
        sentiment_distribution = {}
        for sentiment_str, count in self.sentiment_distribution.items():
            try:
                sentiment_distribution[SentimentType(sentiment_str)] = count
            except ValueError:
                continue

        # Create TrendAnalysis
        trend_analysis = TrendAnalysis(
            trend_type=self.trend_type,
            confidence=self.trend_confidence,
            affected_feedback_count=self.affected_feedback_count,
            primary_topics=primary_topics,
            sentiment_distribution=sentiment_distribution,
            key_indicators=self.key_indicators or [],
            time_period=self.time_period,
            severity_score=self.severity_score,
        )

        # Parse supporting feedback IDs
        supporting_feedback_ids = []
        if self.supporting_feedback_ids:
            supporting_feedback_ids = [
                UUID(id_str) for id_str in self.supporting_feedback_ids
            ]

        return GoalProposal(
            id=self.id,
            title=self.title,
            description=self.description,
            status=self.status,
            priority=self.priority,
            source_trend=trend_analysis,
            supporting_feedback_ids=supporting_feedback_ids,
            created_at=self.created_at,
            created_by=self.created_by,
            tags=self.tags or [],
            estimated_effort=self.estimated_effort,
            potential_impact=self.potential_impact,
        )

    @classmethod
    def from_pydantic(cls, proposal: GoalProposal) -> "GoalProposalDB":
        """Create from Pydantic model."""
        # Convert sentiment distribution to JSON-serializable format
        sentiment_distribution = {
            sentiment.value: count
            for sentiment, count in proposal.source_trend.sentiment_distribution.items()
        }

        return cls(
            id=proposal.id,
            title=proposal.title,
            description=proposal.description,
            status=proposal.status,
            priority=proposal.priority,
            trend_type=proposal.source_trend.trend_type,
            trend_confidence=proposal.source_trend.confidence,
            affected_feedback_count=proposal.source_trend.affected_feedback_count,
            primary_topics=[
                topic.value for topic in proposal.source_trend.primary_topics
            ],
            sentiment_distribution=sentiment_distribution,
            key_indicators=proposal.source_trend.key_indicators,
            time_period=proposal.source_trend.time_period,
            severity_score=proposal.source_trend.severity_score,
            supporting_feedback_ids=[
                str(feedback_id) for feedback_id in proposal.supporting_feedback_ids
            ],
            created_at=proposal.created_at,
            created_by=proposal.created_by,
            tags=proposal.tags,
            estimated_effort=proposal.estimated_effort,
            potential_impact=proposal.potential_impact,
        )

    def update_from_pydantic(self, proposal: GoalProposal):
        """Update from Pydantic model."""
        sentiment_distribution = {
            sentiment.value: count
            for sentiment, count in proposal.source_trend.sentiment_distribution.items()
        }

        self.title = proposal.title
        self.description = proposal.description
        self.status = proposal.status
        self.priority = proposal.priority
        self.trend_type = proposal.source_trend.trend_type
        self.trend_confidence = proposal.source_trend.confidence
        self.affected_feedback_count = proposal.source_trend.affected_feedback_count
        self.primary_topics = [
            topic.value for topic in proposal.source_trend.primary_topics
        ]
        self.sentiment_distribution = sentiment_distribution
        self.key_indicators = proposal.source_trend.key_indicators
        self.time_period = proposal.source_trend.time_period
        self.severity_score = proposal.source_trend.severity_score
        self.supporting_feedback_ids = [
            str(feedback_id) for feedback_id in proposal.supporting_feedback_ids
        ]
        self.tags = proposal.tags
        self.estimated_effort = proposal.estimated_effort
        self.potential_impact = proposal.potential_impact
