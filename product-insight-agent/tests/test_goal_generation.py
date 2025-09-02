"""Tests for goal generation functionality."""

import pytest
from unittest.mock import Mock, patch
from datetime import datetime
from uuid import uuid4

from src.insights.goal_service import GoalGenerationService
from src.models import (
    TrendAnalysis, TrendType, TopicTag, SentimentType, FeedbackItem,
    InsightAnalysis, SentimentAnalysis, TopicAnalysis, GoalProposal
)


class TestGoalGenerationService:
    """Test cases for GoalGenerationService."""
    
    def setup_method(self):
        """Set up test instance."""
        self.mock_llm_service = Mock()
        self.service = GoalGenerationService(self.mock_llm_service)
    
    @pytest.fixture
    def sample_trend(self):
        """Create a sample trend analysis."""
        return TrendAnalysis(
            trend_type=TrendType.SENTIMENT_SHIFT,
            confidence=0.85,
            affected_feedback_count=12,
            primary_topics=[TopicTag.BUG, TopicTag.PERFORMANCE],
            sentiment_distribution={
                SentimentType.NEGATIVE: 10,
                SentimentType.NEUTRAL: 2,
                SentimentType.POSITIVE: 0
            },
            key_indicators=[
                "Negative sentiment increased from 20% to 80%",
                "Affected 12 pieces of feedback",
                "Primary issues: Bug Reports, Performance"
            ],
            time_period="Last 2 days",
            severity_score=0.9
        )
    
    @pytest.fixture
    def sample_feedback(self):
        """Create sample feedback items."""
        return [
            FeedbackItem(
                source="reddit",
                source_id="test1",
                source_url="https://reddit.com/test1",
                author="user1",
                title="App keeps crashing",
                content="The app crashes every time I try to load the dashboard. Very frustrating!",
                timestamp=datetime.utcnow(),
                raw_data={}
            ),
            FeedbackItem(
                source="reddit", 
                source_id="test2",
                source_url="https://reddit.com/test2",
                author="user2",
                title="Performance issues",
                content="The app is very slow, takes forever to load any page. Please fix this!",
                timestamp=datetime.utcnow(),
                raw_data={}
            )
        ]
    
    @pytest.fixture
    def sample_analyses(self, sample_feedback):
        """Create sample analyses for the feedback."""
        return [
            InsightAnalysis(
                feedback_id=sample_feedback[0].id,
                sentiment_analysis=SentimentAnalysis(
                    sentiment=SentimentType.NEGATIVE,
                    confidence=0.9,
                    reasoning="User reports crashes and frustration"
                ),
                topic_analysis=TopicAnalysis(
                    topics=[TopicTag.BUG],
                    confidence=0.95,
                    reasoning="Clear bug report"
                )
            ),
            InsightAnalysis(
                feedback_id=sample_feedback[1].id,
                sentiment_analysis=SentimentAnalysis(
                    sentiment=SentimentType.NEGATIVE,
                    confidence=0.85,
                    reasoning="User reports performance issues"
                ),
                topic_analysis=TopicAnalysis(
                    topics=[TopicTag.PERFORMANCE],
                    confidence=0.9,
                    reasoning="Performance complaint"
                )
            )
        ]
    
    def test_calculate_priority_high_severity(self, sample_trend):
        """Test priority calculation for high severity trends."""
        sample_trend.severity_score = 0.95
        sample_trend.affected_feedback_count = 25
        
        priority = self.service._calculate_priority(sample_trend)
        assert priority == 5  # Maximum priority
    
    def test_calculate_priority_medium_severity(self, sample_trend):
        """Test priority calculation for medium severity trends."""
        sample_trend.severity_score = 0.7
        sample_trend.affected_feedback_count = 8
        sample_trend.trend_type = TrendType.TOPIC_CLUSTER  # Non-sentiment shift to avoid boost
        sample_trend.primary_topics = [TopicTag.GENERAL]  # Non-bug topic
        
        priority = self.service._calculate_priority(sample_trend)
        assert 3 <= priority <= 4
    
    def test_calculate_priority_low_severity(self, sample_trend):
        """Test priority calculation for low severity trends."""
        sample_trend.severity_score = 0.3
        sample_trend.affected_feedback_count = 2
        
        priority = self.service._calculate_priority(sample_trend)
        assert priority <= 2
    
    def test_calculate_priority_bug_boost(self, sample_trend):
        """Test that bug-related trends get priority boost."""
        sample_trend.severity_score = 0.6
        sample_trend.primary_topics = [TopicTag.BUG]
        
        priority = self.service._calculate_priority(sample_trend)
        assert priority >= 3  # Should get boosted
    
    def test_filter_relevant_feedback(self, sample_trend, sample_feedback, sample_analyses):
        """Test filtering of relevant feedback for a trend."""
        relevant_feedback = self.service._filter_relevant_feedback(
            sample_trend, sample_feedback, sample_analyses
        )
        
        # Both feedback items should be relevant (bug and performance topics match trend)
        assert len(relevant_feedback) == 2
        assert all(item in sample_feedback for item in relevant_feedback)
    
    def test_filter_relevant_feedback_sentiment_shift(self, sample_feedback, sample_analyses):
        """Test filtering for sentiment shift trends."""
        sentiment_trend = TrendAnalysis(
            trend_type=TrendType.SENTIMENT_SHIFT,
            confidence=0.8,
            affected_feedback_count=10,
            primary_topics=[TopicTag.GENERAL],
            sentiment_distribution={SentimentType.NEGATIVE: 10},
            key_indicators=["Sentiment shift detected"],
            time_period="Last 3 days",
            severity_score=0.8
        )
        
        relevant_feedback = self.service._filter_relevant_feedback(
            sentiment_trend, sample_feedback, sample_analyses
        )
        
        # Should include negative sentiment feedback
        assert len(relevant_feedback) == 2
    
    def test_prepare_llm_context(self, sample_trend, sample_feedback):
        """Test LLM context preparation."""
        context = self.service._prepare_llm_context(sample_trend, sample_feedback)
        
        assert "TREND ANALYSIS:" in context
        assert "Sentiment Shift" in context
        assert "85.0%" in context  # confidence
        assert "RELEVANT USER FEEDBACK:" in context
        assert "App keeps crashing" in context
        assert "Performance issues" in context
    
    def test_generate_goal_with_llm_success(self, sample_trend, sample_feedback):
        """Test successful goal generation with LLM."""
        mock_response = """{
            "title": "Fix Critical App Stability Issues",
            "description": "Address the recurring app crashes and performance problems that are causing significant user frustration. This includes investigating crash reports, optimizing loading times, and implementing better error handling to prevent future stability issues.",
            "tags": ["stability", "performance", "critical"],
            "estimated_effort": "2-3 weeks",
            "potential_impact": "Significant improvement in user satisfaction and app retention rates"
        }"""
        
        self.mock_llm_service._call_llm.return_value = mock_response
        
        goal_data = self.service._generate_goal_with_llm(sample_trend, sample_feedback)
        
        assert goal_data is not None
        assert goal_data["title"] == "Fix Critical App Stability Issues"
        assert len(goal_data["description"]) >= 50
        assert "stability" in goal_data["tags"]
        assert goal_data["estimated_effort"] == "2-3 weeks"
    
    def test_generate_goal_with_llm_invalid_json(self, sample_trend, sample_feedback):
        """Test goal generation with invalid JSON response."""
        self.mock_llm_service._call_llm.return_value = "Invalid JSON response"
        
        goal_data = self.service._generate_goal_with_llm(sample_trend, sample_feedback)
        
        assert goal_data is None
    
    def test_generate_goal_with_llm_missing_fields(self, sample_trend, sample_feedback):
        """Test goal generation with missing required fields."""
        mock_response = """{
            "description": "Some description",
            "tags": ["tag1"]
        }"""
        
        self.mock_llm_service._call_llm.return_value = mock_response
        
        goal_data = self.service._generate_goal_with_llm(sample_trend, sample_feedback)
        
        assert goal_data is None
    
    def test_generate_goal_with_llm_title_too_short(self, sample_trend, sample_feedback):
        """Test goal generation with title that's too short."""
        mock_response = """{
            "title": "Fix bug",
            "description": "This is a longer description that meets the minimum length requirement for goal descriptions.",
            "tags": ["bug"]
        }"""
        
        self.mock_llm_service._call_llm.return_value = mock_response
        
        goal_data = self.service._generate_goal_with_llm(sample_trend, sample_feedback)
        
        assert goal_data is None
    
    def test_generate_single_goal_success(self, sample_trend, sample_feedback, sample_analyses):
        """Test successful single goal generation."""
        mock_response = """{
            "title": "Resolve Critical Performance and Crash Issues",
            "description": "Address the significant increase in negative feedback regarding app crashes and performance degradation. Implement comprehensive fixes for stability issues and optimize loading times to restore user confidence.",
            "tags": ["performance", "stability", "user-experience"],
            "estimated_effort": "3-4 weeks",
            "potential_impact": "Major improvement in user satisfaction and app stability metrics"
        }"""
        
        self.mock_llm_service._call_llm.return_value = mock_response
        
        goal = self.service.generate_single_goal(sample_trend, sample_feedback, sample_analyses)
        
        assert goal is not None
        assert isinstance(goal, GoalProposal)
        assert goal.title == "Resolve Critical Performance and Crash Issues"
        assert len(goal.description) >= 50
        assert goal.priority >= 3  # Should be high priority for this severe trend
        assert goal.source_trend == sample_trend
        assert len(goal.supporting_feedback_ids) == 2
        assert "performance" in goal.tags
    
    def test_generate_single_goal_no_relevant_feedback(self, sample_trend):
        """Test goal generation with no relevant feedback."""
        goal = self.service.generate_single_goal(sample_trend, [], [])
        
        assert goal is None
    
    def test_generate_goals_from_trends_success(self, sample_trend, sample_feedback, sample_analyses):
        """Test generating multiple goals from trends."""
        trends = [sample_trend]
        
        mock_response = """{
            "title": "Address User-Reported Stability Issues",
            "description": "Comprehensive fix for the recent surge in crash reports and performance complaints from users. This goal focuses on identifying root causes and implementing robust solutions.",
            "tags": ["stability", "performance"],
            "estimated_effort": "2-3 weeks",
            "potential_impact": "Reduced crash rates and improved user retention"
        }"""
        
        self.mock_llm_service._call_llm.return_value = mock_response
        
        goals = self.service.generate_goals_from_trends(trends, sample_feedback, sample_analyses)
        
        assert len(goals) == 1
        assert all(isinstance(goal, GoalProposal) for goal in goals)
        assert goals[0].title == "Address User-Reported Stability Issues"
    
    def test_generate_goals_from_trends_with_failures(self, sample_feedback, sample_analyses):
        """Test goal generation with some trends failing."""
        trend1 = TrendAnalysis(
            trend_type=TrendType.TOPIC_CLUSTER,
            confidence=0.8,
            affected_feedback_count=5,
            primary_topics=[TopicTag.BUG],
            sentiment_distribution={SentimentType.NEGATIVE: 5},
            key_indicators=["Bug cluster detected"],
            time_period="Last week",
            severity_score=0.7
        )
        
        trend2 = TrendAnalysis(
            trend_type=TrendType.VOLUME_SPIKE,
            confidence=0.9,
            affected_feedback_count=15,
            primary_topics=[TopicTag.GENERAL],
            sentiment_distribution={SentimentType.NEUTRAL: 15},
            key_indicators=["Volume spike detected"],
            time_period="Yesterday",
            severity_score=0.6
        )
        
        trends = [trend1, trend2]
        
        # Mock one success, one failure
        def mock_call_llm_side_effect(prompt):
            if "Bug cluster" in prompt:
                return """{
                    "title": "Fix Critical Bug Cluster",
                    "description": "Address the identified cluster of bug reports that are affecting multiple users and causing significant issues.",
                    "tags": ["bugs", "critical"],
                    "estimated_effort": "1-2 weeks",
                    "potential_impact": "Improved app stability and user satisfaction"
                }"""
            else:
                raise Exception("LLM API error")
        
        self.mock_llm_service._call_llm.side_effect = mock_call_llm_side_effect
        
        goals = self.service.generate_goals_from_trends(trends, sample_feedback, sample_analyses)
        
        # Should only get one goal (the successful one)
        assert len(goals) == 1
        assert goals[0].title == "Fix Critical Bug Cluster"