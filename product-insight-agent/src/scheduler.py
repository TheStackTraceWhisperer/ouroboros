"""
Scheduling service for automated execution of the Product Insight Agent.

This module handles the scheduled execution of the agent pipeline,
including daily report generation and data processing.
"""

import logging
import schedule
import time

from .config import settings
from .main import ProductInsightAgent

logger = logging.getLogger(__name__)


class SchedulingService:
    """Service for scheduling automated agent execution."""

    def __init__(self):
        """Initialize the scheduling service."""
        self.agent = ProductInsightAgent()
        self.is_running = False

    def setup_schedules(self):
        """Set up scheduled tasks."""
        if not settings.schedule_enabled:
            logger.info("Scheduling is disabled")
            return

        # Schedule daily report generation
        schedule.every().day.at(settings.report_schedule_time).do(
            self.run_daily_pipeline
        )

        # Schedule data ingestion every 4 hours
        schedule.every(4).hours.do(self.run_ingestion)

        # Schedule analysis every 2 hours (for unprocessed data)
        schedule.every(2).hours.do(self.run_analysis)

        logger.info(f"Scheduled daily reports at {settings.report_schedule_time}")
        logger.info("Scheduled data ingestion every 4 hours")
        logger.info("Scheduled analysis every 2 hours")

    def run_daily_pipeline(self):
        """Run the complete daily pipeline."""
        logger.info("Running scheduled daily pipeline")

        try:
            self.agent.run_full_pipeline()
            logger.info("Scheduled daily pipeline completed successfully")
        except Exception as e:
            logger.error(f"Scheduled daily pipeline failed: {e}")

    def run_ingestion(self):
        """Run data ingestion only."""
        logger.info("Running scheduled data ingestion")

        try:
            feedback_items = self.agent.ingest_data()
            logger.info(f"Scheduled ingestion completed: {len(feedback_items)} items")
        except Exception as e:
            logger.error(f"Scheduled ingestion failed: {e}")

    def run_analysis(self):
        """Run analysis only."""
        logger.info("Running scheduled analysis")

        try:
            analyses = self.agent.analyze_feedback()
            logger.info(f"Scheduled analysis completed: {len(analyses)} items")
        except Exception as e:
            logger.error(f"Scheduled analysis failed: {e}")

    def start(self):
        """Start the scheduling service."""
        logger.info("Starting scheduling service")

        # Test connections before starting
        if not self.agent.test_connections():
            logger.error("Connection tests failed, not starting scheduler")
            return False

        self.setup_schedules()
        self.is_running = True

        logger.info("Scheduling service started successfully")

        try:
            while self.is_running:
                schedule.run_pending()
                time.sleep(60)  # Check every minute

        except KeyboardInterrupt:
            logger.info("Scheduling service stopped by user")
        except Exception as e:
            logger.error(f"Scheduling service error: {e}")
        finally:
            self.is_running = False

        return True

    def stop(self):
        """Stop the scheduling service."""
        logger.info("Stopping scheduling service")
        self.is_running = False

    def run_once(self):
        """Run all pending scheduled tasks once."""
        logger.info("Running all pending scheduled tasks")
        schedule.run_all()


def main():
    """Main entry point for the scheduling service."""
    logging.basicConfig(
        level=getattr(logging, settings.log_level.upper()),
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )

    scheduler = SchedulingService()
    scheduler.start()


if __name__ == "__main__":
    main()
