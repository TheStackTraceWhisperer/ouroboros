"""
Goal generation service for creating structured goals from trend analysis.

This module uses LLM to synthesize trend analysis results and user feedback
into well-structured, actionable goal proposals.
"""

import json
import logging
from typing import List, Optional

from ..models import (
    TrendAnalysis,
    GoalProposal,
    GoalProposalStatus,
    FeedbackItem,
    InsightAnalysis,
    TrendType,
    TopicTag,
    SentimentType,
)
from ..insights import LLMInsightService

logger = logging.getLogger(__name__)


class GoalGenerationService:
    """Service for generating goal proposals from trend analysis."""

    def __init__(self, llm_service: LLMInsightService):
        """Initialize the goal generation service."""
        self.llm_service = llm_service

    def generate_goals_from_trends(
        self,
        trends: List[TrendAnalysis],
        supporting_feedback: List[FeedbackItem],
        supporting_analyses: List[InsightAnalysis],
    ) -> List[GoalProposal]:
        """
        Generate goal proposals from detected trends.

        Args:
            trends: List of significant trends detected
            supporting_feedback: Raw feedback items that support the trends
            supporting_analyses: Analysis results for the feedback

        Returns:
            List of generated goal proposals
        """
        logger.info(
            f"Generating goals from {len(trends)} trends with {len(supporting_feedback)} supporting feedback items"
        )

        goals = []

        for trend in trends:
            try:
                goal = self.generate_single_goal(
                    trend, supporting_feedback, supporting_analyses
                )
                if goal:
                    goals.append(goal)
            except Exception as e:
                logger.warning(
                    f"Failed to generate goal for trend {trend.trend_type}: {e}"
                )
                continue

        logger.info(f"Successfully generated {len(goals)} goal proposals")
        return goals

    def generate_single_goal(
        self,
        trend: TrendAnalysis,
        supporting_feedback: List[FeedbackItem],
        supporting_analyses: List[InsightAnalysis],
    ) -> Optional[GoalProposal]:
        """
        Generate a single goal proposal from a trend analysis.

        Args:
            trend: The trend analysis to base the goal on
            supporting_feedback: Feedback items related to this trend
            supporting_analyses: Analysis results for the feedback

        Returns:
            Generated goal proposal or None if generation fails
        """
        # Filter feedback relevant to this trend
        relevant_feedback = self._filter_relevant_feedback(
            trend, supporting_feedback, supporting_analyses
        )

        if not relevant_feedback:
            logger.warning(f"No relevant feedback found for trend {trend.trend_type}")
            return None

        # Generate goal using LLM
        goal_data = self._generate_goal_with_llm(trend, relevant_feedback)

        if not goal_data:
            return None

        # Calculate priority based on trend characteristics
        priority = self._calculate_priority(trend)

        # Create goal proposal
        supporting_feedback_ids = [item.id for item in relevant_feedback]

        # Generate summary text from title and description
        summary_text = f"{goal_data['title']} - {goal_data['description'][:100]}..."

        goal = GoalProposal(
            title=goal_data["title"],
            description=goal_data["description"],
            status=GoalProposalStatus.PENDING,
            priority=priority,
            source_trend=trend,
            supporting_feedback_ids=supporting_feedback_ids,
            tags=goal_data.get("tags", []),
            estimated_effort=goal_data.get("estimated_effort"),
            potential_impact=goal_data.get("potential_impact"),
            summary_text=summary_text,
        )

        return goal

    def _filter_relevant_feedback(
        self,
        trend: TrendAnalysis,
        supporting_feedback: List[FeedbackItem],
        supporting_analyses: List[InsightAnalysis],
    ) -> List[FeedbackItem]:
        """Filter feedback items relevant to the given trend."""
        relevant_items = []

        # Create a mapping of feedback ID to analysis
        analysis_map = {
            analysis.feedback_id: analysis for analysis in supporting_analyses
        }

        for item in supporting_feedback:
            analysis = analysis_map.get(item.id)
            if not analysis:
                continue

            # Check if this feedback is relevant to the trend
            is_relevant = False

            # Check topic relevance
            for topic in trend.primary_topics:
                if topic in analysis.topic_analysis.topics:
                    is_relevant = True
                    break

            # For sentiment shifts, also include negative sentiment
            if trend.trend_type == TrendType.SENTIMENT_SHIFT:
                if analysis.sentiment_analysis.sentiment == SentimentType.NEGATIVE:
                    is_relevant = True

            # For volume spikes, include recent items
            if trend.trend_type == TrendType.VOLUME_SPIKE:
                # Include items from the last day (simplified)
                is_relevant = True

            if is_relevant:
                relevant_items.append(item)

        # Limit to most recent and relevant items (max 10 for LLM context)
        relevant_items.sort(key=lambda x: x.timestamp, reverse=True)
        return relevant_items[:10]

    def _generate_goal_with_llm(
        self, trend: TrendAnalysis, relevant_feedback: List[FeedbackItem]
    ) -> Optional[dict]:
        """Use LLM to generate goal content based on trend and feedback."""

        # Prepare context for LLM
        context = self._prepare_llm_context(trend, relevant_feedback)

        prompt = f"""
You are a product manager analyzing user feedback trends to create actionable goals.

Based on the following trend analysis and user feedback, generate a well-structured goal proposal.

{context}

Create a goal proposal that addresses this trend. The goal should be:
- Specific and actionable
- Clearly address the underlying user needs
- Have measurable outcomes when possible
- Be feasible for a development team

Respond with a JSON object in this exact format:
{{
    "title": "Clear, concise goal title (10-200 characters)",
    "description": "Detailed description of the goal, why it's needed, and what success looks like (50+ characters)",
    "tags": ["tag1", "tag2"],
    "estimated_effort": "Brief effort estimate (e.g., 'Small', 'Medium', 'Large', '1-2 weeks', '1 month')",
    "potential_impact": "Expected positive impact on users and metrics"
}}
"""

        try:
            response = self.llm_service._call_llm(prompt)
            goal_data = json.loads(response)

            # Validate required fields
            if not goal_data.get("title") or not goal_data.get("description"):
                logger.warning("LLM response missing required fields")
                return None

            # Validate field lengths
            if len(goal_data["title"]) < 10 or len(goal_data["title"]) > 200:
                logger.warning(f"Goal title length invalid: {len(goal_data['title'])}")
                return None

            if len(goal_data["description"]) < 50:
                logger.warning(
                    f"Goal description too short: {len(goal_data['description'])}"
                )
                return None

            return goal_data

        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse LLM response as JSON: {e}")
            return None
        except Exception as e:
            logger.error(f"Error generating goal with LLM: {e}")
            return None

    def _prepare_llm_context(
        self, trend: TrendAnalysis, relevant_feedback: List[FeedbackItem]
    ) -> str:
        """Prepare context string for LLM prompt."""

        # Trend summary
        trend_desc = f"""
TREND ANALYSIS:
- Type: {trend.trend_type.value.replace('_', ' ').title()}
- Confidence: {trend.confidence:.1%}
- Affected Feedback: {trend.affected_feedback_count} items
- Primary Topics: {', '.join([topic.value.replace('-', ' ').title() for topic in trend.primary_topics])}
- Time Period: {trend.time_period}
- Severity Score: {trend.severity_score:.2f}
- Key Indicators:
{chr(10).join([f"  • {indicator}" for indicator in trend.key_indicators])}

SENTIMENT DISTRIBUTION:
{chr(10).join([f"  • {sentiment.value.title()}: {count} items" for sentiment, count in trend.sentiment_distribution.items()])}
"""

        # Sample feedback
        feedback_samples = []
        for i, item in enumerate(relevant_feedback[:5], 1):  # Limit to 5 samples
            sample = f"Sample {i}:"
            if item.title:
                sample += f"\n  Title: {item.title}"
            sample += f"\n  Content: {item.content[:200]}{'...' if len(item.content) > 200 else ''}"
            sample += f"\n  Author: {item.author}"
            feedback_samples.append(sample)

        feedback_context = "\n\n".join(feedback_samples)

        return f"{trend_desc}\n\nRELEVANT USER FEEDBACK:\n{feedback_context}"

    def _calculate_priority(self, trend: TrendAnalysis) -> int:
        """
        Calculate priority level (1-5) based on trend characteristics.

        Args:
            trend: The trend analysis

        Returns:
            Priority level from 1 (low) to 5 (high)
        """
        base_priority = 3  # Default medium priority

        # Adjust based on severity score
        if trend.severity_score >= 0.9:
            base_priority = 5
        elif trend.severity_score >= 0.8:
            base_priority = 4
        elif trend.severity_score >= 0.6:
            base_priority = 3
        elif trend.severity_score >= 0.4:
            base_priority = 2
        else:
            base_priority = 1

        # Adjust based on trend type
        if trend.trend_type == TrendType.SENTIMENT_SHIFT:
            base_priority = min(5, base_priority + 1)  # Sentiment shifts are urgent
        elif trend.trend_type == TrendType.RECURRING_ISSUE:
            base_priority = min(5, base_priority + 1)  # Recurring issues need attention
        elif trend.trend_type == TrendType.VOLUME_SPIKE:
            # Volume spikes might be temporary, but could indicate urgent issues
            base_priority = max(1, min(5, base_priority))

        # Adjust based on affected feedback count
        if trend.affected_feedback_count >= 20:
            base_priority = min(5, base_priority + 1)
        elif trend.affected_feedback_count <= 3:
            base_priority = max(1, base_priority - 1)

        # Adjust based on topics
        high_priority_topics = {TopicTag.BUG, TopicTag.PERFORMANCE}
        if any(topic in high_priority_topics for topic in trend.primary_topics):
            base_priority = min(5, base_priority + 1)

        return max(1, min(5, base_priority))

    def test_goal_generation(
        self, sample_trend: TrendAnalysis, sample_feedback: List[FeedbackItem]
    ) -> bool:
        """
        Test goal generation with sample data.

        Args:
            sample_trend: Sample trend for testing
            sample_feedback: Sample feedback items

        Returns:
            True if test succeeds, False otherwise
        """
        try:
            goal = self.generate_single_goal(sample_trend, sample_feedback, [])
            return goal is not None
        except Exception as e:
            logger.error(f"Goal generation test failed: {e}")
            return False
