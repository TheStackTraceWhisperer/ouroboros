"""Main entry point for the Product Insight Agent."""

import logging
import os
import sys
from datetime import datetime, timedelta
from typing import List

import click
from dotenv import load_dotenv

from .config import settings
from .models import FeedbackItem, InsightAnalysis, DailySummary, GoalProposal, TrendAnalysis
from .ingestion import RedditConnector
from .processing import DataProcessor
from .insights import LLMInsightService, GoalGenerationService
from .reporting import ReportingService
from .storage import StorageService

# Load environment variables
load_dotenv()

# Setup logging
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper()),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('product-insight-agent.log')
    ]
)
logger = logging.getLogger(__name__)


class ProductInsightAgent:
    """Main Product Insight Agent orchestrator."""
    
    def __init__(self):
        """Initialize the agent with all services."""
        logger.info("Initializing Product Insight Agent")
        
        self.reddit_connector = RedditConnector()
        self.data_processor = DataProcessor()
        self.llm_service = LLMInsightService()
        self.storage_service = StorageService()
        self.reporting_service = ReportingService(self.llm_service)
        self.goal_service = GoalGenerationService(self.llm_service)
        
        # Ensure data directories exist
        os.makedirs(settings.data_dir, exist_ok=True)
        os.makedirs(settings.reports_dir, exist_ok=True)
    
    def test_connections(self) -> bool:
        """Test all external service connections."""
        logger.info("Testing service connections...")
        
        success = True
        
        # Test Reddit connection
        if not self.reddit_connector.test_connection():
            logger.error("Reddit connection failed")
            success = False
        
        # Test LLM connection
        if not self.llm_service.test_connection():
            logger.error("LLM connection failed")
            success = False
        
        # Test database connection
        if not self.storage_service.test_connection():
            logger.error("Database connection failed")
            success = False
        
        if success:
            logger.info("All service connections successful")
        
        return success
    
    def ingest_data(self) -> List[FeedbackItem]:
        """Ingest new feedback data from Reddit."""
        logger.info("Starting data ingestion")
        
        try:
            # Get unprocessed feedback from the last 24 hours
            since = datetime.utcnow() - timedelta(days=settings.days_to_analyze)
            raw_feedback = self.reddit_connector.ingest_posts_since(
                since=since,
                limit=settings.max_posts_per_run
            )
            
            if not raw_feedback:
                logger.info("No new feedback found")
                return []
            
            # Process the raw feedback
            processed_feedback = self.data_processor.process_feedback_items(raw_feedback)
            
            # Store in database
            if processed_feedback:
                self.storage_service.store_feedback_items(processed_feedback)
                logger.info(f"Ingested and stored {len(processed_feedback)} feedback items")
            
            return processed_feedback
            
        except Exception as e:
            logger.error(f"Data ingestion failed: {e}")
            return []
    
    def analyze_feedback(self, feedback_items: List[FeedbackItem] = None) -> List[InsightAnalysis]:
        """Analyze feedback for sentiment and topics."""
        logger.info("Starting feedback analysis")
        
        try:
            if feedback_items is None:
                # Get unanalyzed feedback from database
                feedback_items = self.storage_service.get_unanalyzed_feedback_items()
            
            if not feedback_items:
                logger.info("No feedback items to analyze")
                return []
            
            # Perform LLM analysis
            analyses = self.llm_service.analyze_feedback_items(feedback_items)
            
            # Store analyses in database
            if analyses:
                self.storage_service.store_insight_analyses(analyses)
                logger.info(f"Analyzed and stored {len(analyses)} feedback items")
            
            return analyses
            
        except Exception as e:
            logger.error(f"Feedback analysis failed: {e}")
            return []
    
    def generate_report(self, target_date: datetime = None) -> DailySummary:
        """Generate daily summary report."""
        if target_date is None:
            target_date = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
        
        logger.info(f"Generating report for {target_date.date()}")
        
        try:
            # Get feedback and analyses for the target date
            start_date = target_date
            end_date = target_date + timedelta(days=1)
            
            feedback_items = self.storage_service.get_feedback_items_by_date_range(
                start_date, end_date
            )
            analyses = self.storage_service.get_analyses_by_date_range(
                start_date, end_date
            )
            
            if not feedback_items:
                logger.info(f"No feedback data for {target_date.date()}")
                # Create empty summary
                summary = DailySummary(
                    date=target_date,
                    total_feedback_items=0,
                    sentiment_breakdown={},
                    topic_breakdown={},
                    key_insights=["No feedback data available for this date."],
                    summary_text="No user feedback was collected on this date."
                )
            else:
                # Generate comprehensive summary
                summary = self.reporting_service.generate_daily_summary(
                    feedback_items, analyses, target_date
                )
            
            # Store summary in database
            self.storage_service.store_daily_summary(summary)
            
            # Save summary to file
            self._save_report_to_file(summary)
            
            logger.info(f"Generated and stored daily summary for {target_date.date()}")
            return summary
            
        except Exception as e:
            logger.error(f"Report generation failed: {e}")
            raise
    
    def run_full_pipeline(self) -> None:
        """Run the complete data pipeline."""
        logger.info("Starting full pipeline execution")
        
        try:
            # Step 1: Test connections
            if not self.test_connections():
                logger.error("Connection tests failed, aborting pipeline")
                return
            
            # Step 2: Ingest new data
            feedback_items = self.ingest_data()
            
            # Step 3: Analyze feedback (both new and previously unanalyzed)
            analyses = self.analyze_feedback()
            
            # Step 4: Generate daily report
            summary = self.generate_report()
            
            logger.info("Full pipeline execution completed successfully")
            
            # Print summary statistics
            print(f"\n=== Pipeline Execution Summary ===")
            print(f"Date: {datetime.utcnow().date()}")
            print(f"New feedback items ingested: {len(feedback_items)}")
            print(f"Feedback items analyzed: {len(analyses)}")
            print(f"Total feedback in daily summary: {summary.total_feedback_items}")
            print(f"Report saved to: {self._get_report_filename(summary.date)}")
            print("=" * 35)
            
        except Exception as e:
            logger.error(f"Pipeline execution failed: {e}")
            raise
    
    def _save_report_to_file(self, summary: DailySummary) -> None:
        """Save summary report to file."""
        filename = self._get_report_filename(summary.date)
        
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(f"# Daily Feedback Summary - {summary.date.date()}\n\n")
                f.write(f"**Generated:** {summary.generated_at.isoformat()}\n\n")
                f.write(f"## Overview\n\n")
                f.write(f"- **Total Feedback Items:** {summary.total_feedback_items}\n")
                
                if summary.sentiment_breakdown:
                    f.write(f"\n## Sentiment Breakdown\n\n")
                    for sentiment, count in summary.sentiment_breakdown.items():
                        f.write(f"- **{sentiment.value.title()}:** {count}\n")
                
                if summary.topic_breakdown:
                    f.write(f"\n## Topic Breakdown\n\n")
                    for topic, count in sorted(summary.topic_breakdown.items(), 
                                             key=lambda x: x[1], reverse=True):
                        if count > 0:
                            topic_name = topic.value.replace('-', ' ').title()
                            f.write(f"- **{topic_name}:** {count}\n")
                
                if summary.key_insights:
                    f.write(f"\n## Key Insights\n\n")
                    for insight in summary.key_insights:
                        f.write(f"- {insight}\n")
                
                f.write(f"\n## Summary\n\n")
                f.write(summary.summary_text)
                
            logger.info(f"Report saved to {filename}")
            
        except Exception as e:
            logger.error(f"Failed to save report to file: {e}")
    
    def _get_report_filename(self, date: datetime) -> str:
        """Get filename for report."""
        date_str = date.strftime("%Y-%m-%d")
        return os.path.join(settings.reports_dir, f"daily-summary-{date_str}.md")
    
    def generate_goals_from_trends(self, days: int = 7) -> List[GoalProposal]:
        """
        Analyze recent data for trends and generate goal proposals.
        
        Args:
            days: Number of days to analyze for trends
            
        Returns:
            List of generated goal proposals
        """
        logger.info(f"Generating goals from trends over last {days} days")
        
        try:
            # Get recent data
            cutoff_date = datetime.utcnow() - timedelta(days=days)
            
            # Get recent summaries
            summaries = []
            for i in range(days):
                date = cutoff_date + timedelta(days=i)
                summary = self.storage_service.get_daily_summary_by_date(date.date())
                if summary:
                    summaries.append(summary)
            
            if len(summaries) < 2:
                logger.warning("Insufficient data for trend analysis")
                return []
            
            # Get recent feedback and analyses
            feedback_items = self.storage_service.get_feedback_items_by_date_range(
                cutoff_date, datetime.utcnow()
            )
            analyses = self.storage_service.get_analyses_by_date_range(
                cutoff_date, datetime.utcnow()
            )
            
            if not feedback_items or not analyses:
                logger.warning("No recent feedback or analyses found")
                return []
            
            # Detect trends
            trends = self.reporting_service.detect_significant_trends(
                summaries, feedback_items, analyses, days
            )
            
            if not trends:
                logger.info("No significant trends detected")
                return []
            
            logger.info(f"Detected {len(trends)} significant trends")
            
            # Generate goals from trends
            goals = self.goal_service.generate_goals_from_trends(
                trends, feedback_items, analyses
            )
            
            if goals:
                # Store goals in database
                success = self.storage_service.store_goal_proposals(goals)
                if success:
                    logger.info(f"Successfully generated and stored {len(goals)} goal proposals")
                else:
                    logger.error("Failed to store goal proposals")
            
            return goals
            
        except Exception as e:
            logger.error(f"Goal generation failed: {e}")
            return []
    
    def run_enhanced_pipeline(self) -> None:
        """Run the complete pipeline including goal generation."""
        logger.info("Starting enhanced pipeline execution with goal generation")
        
        try:
            # Step 1: Test connections
            if not self.test_connections():
                logger.error("Connection tests failed, aborting pipeline")
                return
            
            # Step 2: Ingest new data
            feedback_items = self.ingest_data()
            
            # Step 3: Analyze feedback
            analyses = self.analyze_feedback()
            
            # Step 4: Generate daily report
            summary = self.generate_report()
            
            # Step 5: Generate goals from trends (if enough data)
            goals = self.generate_goals_from_trends()
            
            logger.info("Enhanced pipeline execution completed successfully")
            
            # Print summary statistics
            print(f"\n=== Enhanced Pipeline Execution Summary ===")
            print(f"Date: {datetime.utcnow().date()}")
            print(f"New feedback items ingested: {len(feedback_items)}")
            print(f"Feedback items analyzed: {len(analyses)}")
            print(f"Daily summary generated: {'Yes' if summary else 'No'}")
            print(f"Goal proposals generated: {len(goals)}")
            
            if goals:
                print(f"\n--- Generated Goals ---")
                for i, goal in enumerate(goals, 1):
                    print(f"{i}. {goal.title} (Priority: {goal.priority}/5)")
            
        except Exception as e:
            logger.error(f"Enhanced pipeline execution failed: {e}")
            raise


@click.group()
def cli():
    """Product Insight Agent CLI."""
    pass


@cli.command()
def test():
    """Test all service connections."""
    agent = ProductInsightAgent()
    success = agent.test_connections()
    sys.exit(0 if success else 1)


@cli.command()
def ingest():
    """Ingest new feedback data."""
    agent = ProductInsightAgent()
    feedback_items = agent.ingest_data()
    print(f"Ingested {len(feedback_items)} feedback items")


@cli.command()
def analyze():
    """Analyze unprocessed feedback."""
    agent = ProductInsightAgent()
    analyses = agent.analyze_feedback()
    print(f"Analyzed {len(analyses)} feedback items")


@cli.command()
@click.option('--date', type=click.DateTime(['%Y-%m-%d']), help='Date for report (YYYY-MM-DD)')
def report(date):
    """Generate daily summary report."""
    agent = ProductInsightAgent()
    target_date = datetime.fromisoformat(date.isoformat()) if date else None
    summary = agent.generate_report(target_date)
    print(f"Generated report for {summary.date.date()}")
    print(f"Total feedback items: {summary.total_feedback_items}")


@cli.command()
def pipeline():
    """Run the complete data pipeline."""
    agent = ProductInsightAgent()
    agent.run_full_pipeline()


@cli.command()
def stats():
    """Show database statistics."""
    agent = ProductInsightAgent()
    stats = agent.storage_service.get_statistics()
    
    print("\n=== Database Statistics ===")
    for key, value in stats.items():
        if isinstance(value, float):
            print(f"{key.replace('_', ' ').title()}: {value:.1f}")
        else:
            print(f"{key.replace('_', ' ').title()}: {value}")
    print("=" * 28)


@cli.command()
@click.option('--days', default=7, help='Number of days to analyze for trends')
def generate_goals(days):
    """Generate goal proposals from trend analysis."""
    agent = ProductInsightAgent()
    goals = agent.generate_goals_from_trends(days)
    
    if goals:
        print(f"\n=== Generated {len(goals)} Goal Proposals ===")
        for i, goal in enumerate(goals, 1):
            print(f"\n{i}. {goal.title}")
            print(f"   Priority: {goal.priority}/5")
            print(f"   Status: {goal.status.value}")
            print(f"   Trend: {goal.source_trend.trend_type.value.replace('_', ' ').title()}")
            print(f"   Affected Feedback: {goal.source_trend.affected_feedback_count}")
            print(f"   Description: {goal.description[:100]}...")
    else:
        print("No goal proposals generated.")


@cli.command()
def enhanced_pipeline():
    """Run the complete pipeline including goal generation."""
    agent = ProductInsightAgent()
    agent.run_enhanced_pipeline()


@cli.command()
@click.option('--status', type=click.Choice(['pending', 'approved', 'rejected', 'in_progress', 'completed']),
              help='Filter goals by status')
@click.option('--limit', default=20, help='Maximum number of goals to show')
def list_goals(status, limit):
    """List existing goal proposals."""
    agent = ProductInsightAgent()
    
    from .models import GoalProposalStatus
    status_filter = None
    if status:
        status_filter = GoalProposalStatus(status)
    
    goals = agent.storage_service.get_goal_proposals(status=status_filter, limit=limit)
    
    if goals:
        print(f"\n=== Found {len(goals)} Goal Proposals ===")
        for i, goal in enumerate(goals, 1):
            print(f"\n{i}. {goal.title}")
            print(f"   ID: {goal.id}")
            print(f"   Priority: {goal.priority}/5")
            print(f"   Status: {goal.status.value}")
            print(f"   Created: {goal.created_at.strftime('%Y-%m-%d %H:%M')}")
            print(f"   Trend: {goal.source_trend.trend_type.value.replace('_', ' ').title()}")
    else:
        print("No goal proposals found.")


if __name__ == "__main__":
    cli()