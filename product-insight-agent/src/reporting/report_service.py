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
    
    def detect_significant_trends(self,
                                summaries: List[DailySummary],
                                feedback_items: List[FeedbackItem],
                                analyses: List[InsightAnalysis],
                                days: int = 7) -> List["TrendAnalysis"]:
        """
        Detect significant trends that warrant goal creation.
        
        Args:
            summaries: Recent daily summaries
            feedback_items: Recent feedback items
            analyses: Recent analysis results
            days: Number of days to analyze
            
        Returns:
            List of significant trend analyses
        """
        from ..models import TrendAnalysis, TrendType
        
        trends = []
        
        # 1. Detect sentiment shifts
        sentiment_trends = self._detect_sentiment_shifts(summaries, feedback_items, analyses)
        trends.extend(sentiment_trends)
        
        # 2. Detect topic clusters
        topic_trends = self._detect_topic_clusters(summaries, feedback_items, analyses)
        trends.extend(topic_trends)
        
        # 3. Detect volume spikes
        volume_trends = self._detect_volume_spikes(summaries, feedback_items)
        trends.extend(volume_trends)
        
        # 4. Detect recurring issues
        recurring_trends = self._detect_recurring_issues(summaries, feedback_items, analyses)
        trends.extend(recurring_trends)
        
        # Filter by significance threshold
        significant_trends = [trend for trend in trends if trend.severity_score >= 0.6]
        
        # Sort by severity score descending
        significant_trends.sort(key=lambda t: t.severity_score, reverse=True)
        
        logger.info(f"Detected {len(significant_trends)} significant trends from {len(trends)} total trends")
        
        return significant_trends
    
    def _detect_sentiment_shifts(self,
                               summaries: List[DailySummary],
                               feedback_items: List[FeedbackItem],
                               analyses: List[InsightAnalysis]) -> List["TrendAnalysis"]:
        """Detect significant changes in sentiment patterns."""
        from ..models import TrendAnalysis, TrendType
        
        if len(summaries) < 3:
            return []
        
        trends = []
        
        # Calculate recent vs historical sentiment ratios
        recent_summaries = summaries[-2:]  # Last 2 days
        historical_summaries = summaries[:-2]  # Earlier days
        
        recent_negative = sum(s.sentiment_breakdown.get(SentimentType.NEGATIVE, 0) for s in recent_summaries)
        recent_total = sum(s.total_feedback_items for s in recent_summaries)
        
        historical_negative = sum(s.sentiment_breakdown.get(SentimentType.NEGATIVE, 0) for s in historical_summaries)
        historical_total = sum(s.total_feedback_items for s in historical_summaries)
        
        if recent_total > 0 and historical_total > 0:
            recent_negative_ratio = recent_negative / recent_total
            historical_negative_ratio = historical_negative / historical_total
            
            # Detect significant increase in negative sentiment
            if recent_negative_ratio > historical_negative_ratio + 0.2 and recent_negative >= 5:
                # Find associated topics and feedback
                recent_feedback = [item for item in feedback_items 
                                 if any(analysis.feedback_id == item.id and 
                                       analysis.sentiment_analysis.sentiment == SentimentType.NEGATIVE 
                                       for analysis in analyses)]
                
                if recent_feedback:
                    # Determine primary topics
                    topic_counts = {}
                    for analysis in analyses:
                        if analysis.sentiment_analysis.sentiment == SentimentType.NEGATIVE:
                            for topic in analysis.topic_analysis.topics:
                                topic_counts[topic] = topic_counts.get(topic, 0) + 1
                    
                    primary_topics = sorted(topic_counts.keys(), key=lambda t: topic_counts[t], reverse=True)[:3]
                    
                    # Create sentiment distribution
                    sentiment_dist = {
                        SentimentType.NEGATIVE: recent_negative,
                        SentimentType.POSITIVE: sum(s.sentiment_breakdown.get(SentimentType.POSITIVE, 0) for s in recent_summaries),
                        SentimentType.NEUTRAL: sum(s.sentiment_breakdown.get(SentimentType.NEUTRAL, 0) for s in recent_summaries)
                    }
                    
                    trends.append(TrendAnalysis(
                        trend_type=TrendType.SENTIMENT_SHIFT,
                        confidence=min(0.9, (recent_negative_ratio - historical_negative_ratio) * 3),
                        affected_feedback_count=recent_negative,
                        primary_topics=primary_topics,
                        sentiment_distribution=sentiment_dist,
                        key_indicators=[
                            f"Negative sentiment increased from {historical_negative_ratio:.1%} to {recent_negative_ratio:.1%}",
                            f"Affected {recent_negative} pieces of feedback",
                            f"Primary issues: {', '.join([topic.value.replace('-', ' ').title() for topic in primary_topics[:2]])}"
                        ],
                        time_period=f"Last {len(recent_summaries)} days",
                        severity_score=min(1.0, (recent_negative_ratio - historical_negative_ratio) * 2 + (recent_negative / 20))
                    ))
        
        return trends
    
    def _detect_topic_clusters(self,
                             summaries: List[DailySummary],
                             feedback_items: List[FeedbackItem],
                             analyses: List[InsightAnalysis]) -> List["TrendAnalysis"]:
        """Detect clusters of feedback around specific topics."""
        from ..models import TrendAnalysis, TrendType
        
        trends = []
        
        # Count topic occurrences
        topic_counts = {}
        topic_feedback = {}
        
        for analysis in analyses:
            for topic in analysis.topic_analysis.topics:
                topic_counts[topic] = topic_counts.get(topic, 0) + 1
                if topic not in topic_feedback:
                    topic_feedback[topic] = []
                topic_feedback[topic].append(analysis.feedback_id)
        
        # Identify topics with significant volume
        total_feedback = len(analyses)
        for topic, count in topic_counts.items():
            if count >= 5 and count / total_feedback >= 0.15:  # At least 5 items and 15% of total
                # Calculate sentiment distribution for this topic
                topic_analyses = [a for a in analyses if topic in a.topic_analysis.topics]
                sentiment_dist = {
                    SentimentType.POSITIVE: len([a for a in topic_analyses if a.sentiment_analysis.sentiment == SentimentType.POSITIVE]),
                    SentimentType.NEGATIVE: len([a for a in topic_analyses if a.sentiment_analysis.sentiment == SentimentType.NEGATIVE]),
                    SentimentType.NEUTRAL: len([a for a in topic_analyses if a.sentiment_analysis.sentiment == SentimentType.NEUTRAL])
                }
                
                # Calculate severity based on volume and negative sentiment ratio
                negative_ratio = sentiment_dist[SentimentType.NEGATIVE] / count
                volume_score = min(1.0, count / 20)  # Normalize to 20 as high volume
                sentiment_score = negative_ratio if topic != TopicTag.GENERAL else negative_ratio * 0.8
                
                severity_score = (volume_score * 0.6) + (sentiment_score * 0.4)
                
                if severity_score >= 0.5:  # Only significant clusters
                    trends.append(TrendAnalysis(
                        trend_type=TrendType.TOPIC_CLUSTER,
                        confidence=min(0.95, volume_score + 0.3),
                        affected_feedback_count=count,
                        primary_topics=[topic],
                        sentiment_distribution=sentiment_dist,
                        key_indicators=[
                            f"High volume of feedback about {topic.value.replace('-', ' ').title()}",
                            f"{count} items ({count/total_feedback:.1%} of total feedback)",
                            f"{negative_ratio:.1%} negative sentiment ratio"
                        ],
                        time_period=f"Last {len(summaries)} days",
                        severity_score=severity_score
                    ))
        
        return trends
    
    def _detect_volume_spikes(self,
                            summaries: List[DailySummary],
                            feedback_items: List[FeedbackItem]) -> List["TrendAnalysis"]:
        """Detect unusual spikes in feedback volume."""
        from ..models import TrendAnalysis, TrendType
        
        if len(summaries) < 3:
            return []
        
        trends = []
        
        # Calculate volume metrics
        volumes = [s.total_feedback_items for s in summaries]
        avg_volume = sum(volumes) / len(volumes)
        recent_volume = summaries[-1].total_feedback_items
        
        # Detect significant spike (more than 2x average)
        if recent_volume > avg_volume * 2 and recent_volume >= 10:
            # Get recent feedback
            recent_feedback = feedback_items[-recent_volume:] if len(feedback_items) >= recent_volume else feedback_items
            
            # Basic sentiment distribution
            sentiment_dist = {
                SentimentType.POSITIVE: 0,
                SentimentType.NEGATIVE: 0,
                SentimentType.NEUTRAL: recent_volume  # Default to neutral if no analysis available
            }
            
            severity_score = min(1.0, (recent_volume / avg_volume) / 5)  # Normalize spike ratio
            
            trends.append(TrendAnalysis(
                trend_type=TrendType.VOLUME_SPIKE,
                confidence=0.9,
                affected_feedback_count=recent_volume,
                primary_topics=[TopicTag.GENERAL],  # Would need analysis to determine specific topics
                sentiment_distribution=sentiment_dist,
                key_indicators=[
                    f"Feedback volume spike: {recent_volume} items vs {avg_volume:.1f} average",
                    f"Spike ratio: {recent_volume/avg_volume:.1f}x normal volume",
                    "May indicate significant event or issue"
                ],
                time_period="Last day",
                severity_score=severity_score
            ))
        
        return trends
    
    def _detect_recurring_issues(self,
                               summaries: List[DailySummary],
                               feedback_items: List[FeedbackItem],
                               analyses: List[InsightAnalysis]) -> List["TrendAnalysis"]:
        """Detect issues that keep recurring over time."""
        from ..models import TrendAnalysis, TrendType
        
        if len(summaries) < 5:  # Need at least 5 days to detect patterns
            return []
        
        trends = []
        
        # Look for topics that appear consistently with negative sentiment
        daily_negative_topics = {}
        
        for summary in summaries:
            day_key = summary.date.date()
            daily_negative_topics[day_key] = {}
            
            # This is a simplified version - in reality we'd need day-specific analysis data
            for topic, count in summary.topic_breakdown.items():
                if count > 0:
                    # Estimate negative count (would be better with actual data)
                    negative_sentiment_count = summary.sentiment_breakdown.get(SentimentType.NEGATIVE, 0)
                    topic_negative_estimate = min(count, int(negative_sentiment_count * (count / sum(summary.topic_breakdown.values()))))
                    
                    if topic_negative_estimate > 0:
                        daily_negative_topics[day_key][topic] = topic_negative_estimate
        
        # Find topics that appear negatively on multiple days
        topic_occurrence_days = {}
        topic_total_counts = {}
        
        for day_topics in daily_negative_topics.values():
            for topic, count in day_topics.items():
                if topic not in topic_occurrence_days:
                    topic_occurrence_days[topic] = 0
                    topic_total_counts[topic] = 0
                topic_occurrence_days[topic] += 1
                topic_total_counts[topic] += count
        
        # Identify recurring issues (appear on >50% of days with negative sentiment)
        min_days = len(summaries) * 0.5
        for topic, occurrence_days in topic_occurrence_days.items():
            if occurrence_days >= min_days and topic_total_counts[topic] >= 3:
                # Calculate severity based on consistency and impact
                consistency_score = occurrence_days / len(summaries)
                impact_score = min(1.0, topic_total_counts[topic] / 10)
                severity_score = (consistency_score * 0.7) + (impact_score * 0.3)
                
                if severity_score >= 0.6:
                    sentiment_dist = {
                        SentimentType.NEGATIVE: topic_total_counts[topic],
                        SentimentType.POSITIVE: 0,
                        SentimentType.NEUTRAL: 0
                    }
                    
                    trends.append(TrendAnalysis(
                        trend_type=TrendType.RECURRING_ISSUE,
                        confidence=0.8,
                        affected_feedback_count=topic_total_counts[topic],
                        primary_topics=[topic],
                        sentiment_distribution=sentiment_dist,
                        key_indicators=[
                            f"Recurring issue with {topic.value.replace('-', ' ').title()}",
                            f"Appeared on {occurrence_days}/{len(summaries)} days",
                            f"Total affected feedback: {topic_total_counts[topic]} items",
                            "Indicates persistent problem requiring attention"
                        ],
                        time_period=f"Last {len(summaries)} days",
                        severity_score=severity_score
                    ))
        
        return trends