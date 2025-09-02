"""
Reporting service for generating daily summaries and insights.

This module handles the generation of human-readable summary reports
based on analyzed feedback data.
"""

import json
import logging
from collections import Counter
from datetime import datetime, timedelta
from typing import List, Dict, Any

from ..config import settings
from ..models import (
    FeedbackItem, InsightAnalysis, DailySummary,
    SentimentType, TopicTag
)
from ..insights import LLMInsightService

logger = logging.getLogger(__name__)


class ReportingService:
    """Service for generating reports and summaries from analyzed feedback."""
    
    def __init__(self, llm_service: LLMInsightService):
        """
        Initialize the reporting service.
        
        Args:
            llm_service: LLM service for generating narrative summaries
        """
        self.llm_service = llm_service
    
    def generate_daily_summary(self, 
                             feedback_items: List[FeedbackItem],
                             analyses: List[InsightAnalysis],
                             target_date: datetime) -> DailySummary:
        """
        Generate a daily summary report.
        
        Args:
            feedback_items: List of feedback items for the day
            analyses: List of corresponding insight analyses
            target_date: Date for which to generate the summary
            
        Returns:
            DailySummary object containing the report
        """
        logger.info(f"Generating daily summary for {target_date.date()}")
        
        # Calculate sentiment breakdown
        sentiment_breakdown = self._calculate_sentiment_breakdown(analyses)
        
        # Calculate topic breakdown
        topic_breakdown = self._calculate_topic_breakdown(analyses)
        
        # Generate key insights
        key_insights = self._extract_key_insights(feedback_items, analyses)
        
        # Generate narrative summary using LLM
        summary_text = self._generate_narrative_summary(
            feedback_items, analyses, sentiment_breakdown, topic_breakdown, key_insights
        )
        
        return DailySummary(
            date=target_date,
            total_feedback_items=len(feedback_items),
            sentiment_breakdown=sentiment_breakdown,
            topic_breakdown=topic_breakdown,
            key_insights=key_insights,
            summary_text=summary_text
        )
    
    def _calculate_sentiment_breakdown(self, analyses: List[InsightAnalysis]) -> Dict[SentimentType, int]:
        """
        Calculate sentiment distribution from analyses.
        
        Args:
            analyses: List of insight analyses
            
        Returns:
            Dictionary mapping sentiment types to counts
        """
        sentiment_counts = Counter(analysis.sentiment_analysis.sentiment for analysis in analyses)
        
        return {
            SentimentType.POSITIVE: sentiment_counts.get(SentimentType.POSITIVE, 0),
            SentimentType.NEGATIVE: sentiment_counts.get(SentimentType.NEGATIVE, 0),
            SentimentType.NEUTRAL: sentiment_counts.get(SentimentType.NEUTRAL, 0),
        }
    
    def _calculate_topic_breakdown(self, analyses: List[InsightAnalysis]) -> Dict[TopicTag, int]:
        """
        Calculate topic distribution from analyses.
        
        Args:
            analyses: List of insight analyses
            
        Returns:
            Dictionary mapping topic tags to counts
        """
        # Flatten all topics from all analyses
        all_topics = []
        for analysis in analyses:
            all_topics.extend(analysis.topic_analysis.topics)
        
        topic_counts = Counter(all_topics)
        
        # Ensure all topic types are represented
        return {
            topic: topic_counts.get(topic, 0)
            for topic in TopicTag
        }
    
    def _extract_key_insights(self, 
                            feedback_items: List[FeedbackItem],
                            analyses: List[InsightAnalysis]) -> List[str]:
        """
        Extract key insights from the feedback and analyses.
        
        Args:
            feedback_items: List of feedback items
            analyses: List of insight analyses
            
        Returns:
            List of key insight strings
        """
        insights = []
        
        if not analyses:
            return ["No feedback data available for analysis."]
        
        # Sentiment insights
        sentiment_breakdown = self._calculate_sentiment_breakdown(analyses)
        total_feedback = sum(sentiment_breakdown.values())
        
        if total_feedback > 0:
            positive_pct = (sentiment_breakdown[SentimentType.POSITIVE] / total_feedback) * 100
            negative_pct = (sentiment_breakdown[SentimentType.NEGATIVE] / total_feedback) * 100
            
            if positive_pct > 60:
                insights.append(f"Predominantly positive feedback ({positive_pct:.1f}% positive)")
            elif negative_pct > 60:
                insights.append(f"High volume of negative feedback ({negative_pct:.1f}% negative)")
            else:
                insights.append("Mixed sentiment in user feedback")
        
        # Topic insights
        topic_breakdown = self._calculate_topic_breakdown(analyses)
        top_topics = sorted(topic_breakdown.items(), key=lambda x: x[1], reverse=True)[:3]
        
        if top_topics[0][1] > 0:
            top_topic_name = top_topics[0][0].value.replace('-', ' ').title()
            insights.append(f"Primary discussion topic: {top_topic_name} ({top_topics[0][1]} mentions)")
        
        # Volume insights
        if total_feedback > 50:
            insights.append(f"High feedback volume with {total_feedback} items")
        elif total_feedback < 5:
            insights.append("Low feedback volume - limited user engagement")
        
        # Source insights
        sources = list(set(item.source for item in feedback_items))
        if len(sources) > 1:
            insights.append(f"Feedback collected from {len(sources)} sources: {', '.join(sources)}")
        
        return insights[:5]  # Limit to top 5 insights
    
    def _generate_narrative_summary(self,
                                  feedback_items: List[FeedbackItem],
                                  analyses: List[InsightAnalysis],
                                  sentiment_breakdown: Dict[SentimentType, int],
                                  topic_breakdown: Dict[TopicTag, int],
                                  key_insights: List[str]) -> str:
        """
        Generate a narrative summary using the LLM.
        
        Args:
            feedback_items: List of feedback items
            analyses: List of insight analyses
            sentiment_breakdown: Sentiment distribution
            topic_breakdown: Topic distribution
            key_insights: List of key insights
            
        Returns:
            Human-readable narrative summary
        """
        try:
            # Prepare context for the LLM
            total_items = len(feedback_items)
            
            # Get sample feedback for context
            sample_feedback = []
            for i, item in enumerate(feedback_items[:5]):  # First 5 items
                sample_feedback.append(f"{i+1}. {item.title or 'No title'}: {item.content[:200]}...")
            
            # Format statistics
            sentiment_stats = "\n".join([
                f"- {sentiment.value.title()}: {count} ({count/total_items*100:.1f}%)"
                for sentiment, count in sentiment_breakdown.items() if count > 0
            ])
            
            topic_stats = "\n".join([
                f"- {topic.value.replace('-', ' ').title()}: {count}"
                for topic, count in sorted(topic_breakdown.items(), key=lambda x: x[1], reverse=True)
                if count > 0
            ])
            
            prompt = f"""
Generate a concise, professional daily summary report based on the following user feedback analysis:

STATISTICS:
Total Feedback Items: {total_items}

Sentiment Breakdown:
{sentiment_stats}

Topic Breakdown:
{topic_stats}

Key Insights:
{chr(10).join(f'- {insight}' for insight in key_insights)}

SAMPLE FEEDBACK:
{chr(10).join(sample_feedback)}

Please write a 2-3 paragraph executive summary that:
1. Highlights the overall sentiment and engagement level
2. Identifies the main topics and concerns raised by users
3. Provides actionable insights or recommendations
4. Maintains a professional, analytical tone

Focus on trends, patterns, and implications rather than just restating the numbers.
"""
            
            response = self.llm_service._call_llm(prompt)
            return response.strip()
            
        except Exception as e:
            logger.warning(f"Failed to generate narrative summary using LLM: {e}")
            
            # Fallback to template-based summary
            return self._generate_template_summary(
                len(feedback_items), sentiment_breakdown, topic_breakdown, key_insights
            )
    
    def _generate_template_summary(self,
                                 total_items: int,
                                 sentiment_breakdown: Dict[SentimentType, int],
                                 topic_breakdown: Dict[TopicTag, int],
                                 key_insights: List[str]) -> str:
        """
        Generate a fallback template-based summary.
        
        Args:
            total_items: Total number of feedback items
            sentiment_breakdown: Sentiment distribution
            topic_breakdown: Topic distribution
            key_insights: List of key insights
            
        Returns:
            Template-based summary text
        """
        if total_items == 0:
            return "No user feedback was collected during this period."
        
        # Calculate percentages
        positive_pct = (sentiment_breakdown[SentimentType.POSITIVE] / total_items) * 100
        negative_pct = (sentiment_breakdown[SentimentType.NEGATIVE] / total_items) * 100
        
        # Find top topic
        top_topic = max(topic_breakdown.items(), key=lambda x: x[1])
        top_topic_name = top_topic[0].value.replace('-', ' ').title()
        
        summary = f"""Daily Feedback Summary

We analyzed {total_items} pieces of user feedback today. The overall sentiment was {positive_pct:.1f}% positive, {negative_pct:.1f}% negative, with the remainder being neutral. 

The primary topic of discussion was {top_topic_name}, mentioned in {top_topic[1]} feedback items. """
        
        if key_insights:
            summary += f"Key insights include: {'; '.join(key_insights[:3])}."
        
        return summary.strip()
    
    def generate_trend_analysis(self,
                              summaries: List[DailySummary],
                              days: int = 7) -> Dict[str, Any]:
        """
        Generate trend analysis from multiple daily summaries.
        
        Args:
            summaries: List of daily summaries
            days: Number of days to analyze
            
        Returns:
            Dictionary containing trend analysis
        """
        if not summaries:
            return {"error": "No summary data available"}
        
        # Sort summaries by date
        summaries = sorted(summaries, key=lambda x: x.date)
        
        # Calculate trends
        total_feedback_trend = [s.total_feedback_items for s in summaries]
        
        sentiment_trends = {
            sentiment: [s.sentiment_breakdown.get(sentiment, 0) for s in summaries]
            for sentiment in SentimentType
        }
        
        topic_trends = {
            topic: [s.topic_breakdown.get(topic, 0) for s in summaries]
            for topic in TopicTag
        }
        
        return {
            "date_range": {
                "start": summaries[0].date.isoformat(),
                "end": summaries[-1].date.isoformat()
            },
            "total_feedback_trend": total_feedback_trend,
            "sentiment_trends": sentiment_trends,
            "topic_trends": topic_trends,
            "average_daily_feedback": sum(total_feedback_trend) / len(total_feedback_trend),
            "most_active_day": summaries[total_feedback_trend.index(max(total_feedback_trend))].date.isoformat(),
            "overall_sentiment": {
                "positive": sum(sentiment_trends[SentimentType.POSITIVE]),
                "negative": sum(sentiment_trends[SentimentType.NEGATIVE]),
                "neutral": sum(sentiment_trends[SentimentType.NEUTRAL])
            }
        }