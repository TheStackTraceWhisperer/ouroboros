"""
Database storage service for the Product Insight Agent.

This module handles the persistence of feedback data, analyses, and reports
using SQLAlchemy with support for multiple database backends.
"""

import logging
from datetime import datetime, timedelta
from typing import List, Optional
from uuid import UUID

from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.exc import SQLAlchemyError

from ..config import settings
from ..models import FeedbackItem, InsightAnalysis, DailySummary, GoalProposal
from .database_models import Base, FeedbackItemDB, InsightAnalysisDB, DailySummaryDB, GoalProposalDB

logger = logging.getLogger(__name__)


class StorageService:
    """Service for storing and retrieving feedback data and analyses."""
    
    def __init__(self):
        """Initialize the storage service."""
        self.engine = create_engine(settings.database_url)
        self.SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=self.engine)
        
        # Create tables if they don't exist
        self._ensure_schema_exists()
        Base.metadata.create_all(bind=self.engine)
    
    def _ensure_schema_exists(self):
        """Ensure the database schema exists (for PostgreSQL)."""
        if "postgresql" in settings.database_url and settings.database_schema != "public":
            try:
                with self.engine.connect() as conn:
                    conn.execute(text(f"CREATE SCHEMA IF NOT EXISTS {settings.database_schema}"))
                    conn.commit()
            except Exception as e:
                logger.warning(f"Could not create schema {settings.database_schema}: {e}")
    
    def store_feedback_items(self, feedback_items: List[FeedbackItem]) -> bool:
        """
        Store feedback items in the database.
        
        Args:
            feedback_items: List of FeedbackItem objects to store
            
        Returns:
            True if successful, False otherwise
        """
        if not feedback_items:
            return True
            
        logger.info(f"Storing {len(feedback_items)} feedback items")
        
        try:
            with self.SessionLocal() as session:
                for item in feedback_items:
                    # Check if item already exists
                    existing = session.query(FeedbackItemDB).filter_by(
                        source=item.source,
                        source_id=item.source_id
                    ).first()
                    
                    if not existing:
                        db_item = FeedbackItemDB.from_pydantic(item)
                        session.add(db_item)
                    else:
                        logger.debug(f"Feedback item {item.source}:{item.source_id} already exists")
                
                session.commit()
                logger.info(f"Successfully stored feedback items")
                return True
                
        except SQLAlchemyError as e:
            logger.error(f"Error storing feedback items: {e}")
            return False
    
    def store_insight_analyses(self, analyses: List[InsightAnalysis]) -> bool:
        """
        Store insight analyses in the database.
        
        Args:
            analyses: List of InsightAnalysis objects to store
            
        Returns:
            True if successful, False otherwise
        """
        if not analyses:
            return True
            
        logger.info(f"Storing {len(analyses)} insight analyses")
        
        try:
            with self.SessionLocal() as session:
                for analysis in analyses:
                    # Check if analysis already exists
                    existing = session.query(InsightAnalysisDB).filter_by(
                        feedback_id=analysis.feedback_id
                    ).first()
                    
                    if not existing:
                        db_analysis = InsightAnalysisDB.from_pydantic(analysis)
                        session.add(db_analysis)
                    else:
                        # Update existing analysis
                        existing.update_from_pydantic(analysis)
                
                session.commit()
                logger.info(f"Successfully stored insight analyses")
                return True
                
        except SQLAlchemyError as e:
            logger.error(f"Error storing insight analyses: {e}")
            return False
    
    def store_daily_summary(self, summary: DailySummary) -> bool:
        """
        Store a daily summary in the database.
        
        Args:
            summary: DailySummary object to store
            
        Returns:
            True if successful, False otherwise
        """
        logger.info(f"Storing daily summary for {summary.date.date()}")
        
        try:
            with self.SessionLocal() as session:
                # Check if summary already exists for this date
                existing = session.query(DailySummaryDB).filter_by(
                    date=summary.date.date()
                ).first()
                
                if not existing:
                    db_summary = DailySummaryDB.from_pydantic(summary)
                    session.add(db_summary)
                else:
                    # Update existing summary
                    existing.update_from_pydantic(summary)
                
                session.commit()
                logger.info(f"Successfully stored daily summary")
                return True
                
        except SQLAlchemyError as e:
            logger.error(f"Error storing daily summary: {e}")
            return False
    
    def get_feedback_items_by_date_range(self, 
                                       start_date: datetime, 
                                       end_date: datetime) -> List[FeedbackItem]:
        """
        Retrieve feedback items within a date range.
        
        Args:
            start_date: Start of date range
            end_date: End of date range
            
        Returns:
            List of FeedbackItem objects
        """
        logger.info(f"Retrieving feedback items from {start_date.date()} to {end_date.date()}")
        
        try:
            with self.SessionLocal() as session:
                db_items = session.query(FeedbackItemDB).filter(
                    FeedbackItemDB.timestamp >= start_date,
                    FeedbackItemDB.timestamp <= end_date
                ).all()
                
                return [item.to_pydantic() for item in db_items]
                
        except SQLAlchemyError as e:
            logger.error(f"Error retrieving feedback items: {e}")
            return []
    
    def get_unanalyzed_feedback_items(self) -> List[FeedbackItem]:
        """
        Retrieve feedback items that haven't been analyzed yet.
        
        Returns:
            List of unanalyzed FeedbackItem objects
        """
        logger.info("Retrieving unanalyzed feedback items")
        
        try:
            with self.SessionLocal() as session:
                # Get feedback items that don't have corresponding analyses
                db_items = session.query(FeedbackItemDB).filter(
                    ~FeedbackItemDB.id.in_(
                        session.query(InsightAnalysisDB.feedback_id)
                    )
                ).all()
                
                logger.info(f"Found {len(db_items)} unanalyzed feedback items")
                return [item.to_pydantic() for item in db_items]
                
        except SQLAlchemyError as e:
            logger.error(f"Error retrieving unanalyzed feedback items: {e}")
            return []
    
    def get_analyses_by_date_range(self, 
                                 start_date: datetime, 
                                 end_date: datetime) -> List[InsightAnalysis]:
        """
        Retrieve insight analyses within a date range.
        
        Args:
            start_date: Start of date range
            end_date: End of date range
            
        Returns:
            List of InsightAnalysis objects
        """
        logger.info(f"Retrieving analyses from {start_date.date()} to {end_date.date()}")
        
        try:
            with self.SessionLocal() as session:
                db_analyses = session.query(InsightAnalysisDB).filter(
                    InsightAnalysisDB.analyzed_at >= start_date,
                    InsightAnalysisDB.analyzed_at <= end_date
                ).all()
                
                return [analysis.to_pydantic() for analysis in db_analyses]
                
        except SQLAlchemyError as e:
            logger.error(f"Error retrieving analyses: {e}")
            return []
    
    def get_daily_summaries(self, days: int = 7) -> List[DailySummary]:
        """
        Retrieve recent daily summaries.
        
        Args:
            days: Number of days to retrieve summaries for
            
        Returns:
            List of DailySummary objects
        """
        end_date = datetime.utcnow().date()
        start_date = end_date - timedelta(days=days)
        
        logger.info(f"Retrieving daily summaries from {start_date} to {end_date}")
        
        try:
            with self.SessionLocal() as session:
                db_summaries = session.query(DailySummaryDB).filter(
                    DailySummaryDB.date >= start_date,
                    DailySummaryDB.date <= end_date
                ).order_by(DailySummaryDB.date.desc()).all()
                
                return [summary.to_pydantic() for summary in db_summaries]
                
        except SQLAlchemyError as e:
            logger.error(f"Error retrieving daily summaries: {e}")
            return []
    
    def get_feedback_item_by_id(self, feedback_id: UUID) -> Optional[FeedbackItem]:
        """
        Retrieve a specific feedback item by ID.
        
        Args:
            feedback_id: UUID of the feedback item
            
        Returns:
            FeedbackItem object or None if not found
        """
        try:
            with self.SessionLocal() as session:
                db_item = session.query(FeedbackItemDB).filter_by(id=feedback_id).first()
                return db_item.to_pydantic() if db_item else None
                
        except SQLAlchemyError as e:
            logger.error(f"Error retrieving feedback item {feedback_id}: {e}")
            return None
    
    def get_statistics(self) -> dict:
        """
        Get database statistics.
        
        Returns:
            Dictionary containing database statistics
        """
        try:
            with self.SessionLocal() as session:
                feedback_count = session.query(FeedbackItemDB).count()
                analysis_count = session.query(InsightAnalysisDB).count()
                summary_count = session.query(DailySummaryDB).count()
                
                # Get date ranges
                earliest_feedback = session.query(FeedbackItemDB.timestamp).order_by(
                    FeedbackItemDB.timestamp.asc()
                ).first()
                latest_feedback = session.query(FeedbackItemDB.timestamp).order_by(
                    FeedbackItemDB.timestamp.desc()
                ).first()
                
                return {
                    "total_feedback_items": feedback_count,
                    "total_analyses": analysis_count,
                    "total_summaries": summary_count,
                    "earliest_feedback": earliest_feedback[0] if earliest_feedback else None,
                    "latest_feedback": latest_feedback[0] if latest_feedback else None,
                    "analysis_coverage": (analysis_count / feedback_count * 100) if feedback_count > 0 else 0
                }
                
        except SQLAlchemyError as e:
            logger.error(f"Error retrieving statistics: {e}")
            return {}
    
    def test_connection(self) -> bool:
        """
        Test the database connection.
        
        Returns:
            True if connection is successful, False otherwise
        """
        try:
            with self.engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info("Database connection test successful")
            return True
            
        except Exception as e:
            logger.error(f"Database connection test failed: {e}")
            return False
    
    # Goal Proposal Operations
    
    def store_goal_proposals(self, proposals: List[GoalProposal]) -> bool:
        """
        Store goal proposals in the database.
        
        Args:
            proposals: List of GoalProposal objects to store
            
        Returns:
            True if successful, False otherwise
        """
        try:
            with self.SessionLocal() as session:
                for proposal in proposals:
                    db_proposal = GoalProposalDB.from_pydantic(proposal)
                    session.merge(db_proposal)
                session.commit()
                
            logger.info(f"Stored {len(proposals)} goal proposals")
            return True
            
        except SQLAlchemyError as e:
            logger.error(f"Failed to store goal proposals: {e}")
            return False
    
    def get_goal_proposals(self,
                         status: Optional["GoalProposalStatus"] = None,
                         limit: int = 100) -> List[GoalProposal]:
        """
        Retrieve goal proposals from the database.
        
        Args:
            status: Optional status filter
            limit: Maximum number of proposals to return
            
        Returns:
            List of GoalProposal objects
        """
        try:
            with self.SessionLocal() as session:
                query = session.query(GoalProposalDB)
                
                if status:
                    query = query.filter(GoalProposalDB.status == status)
                
                query = query.order_by(GoalProposalDB.created_at.desc())
                query = query.limit(limit)
                
                db_proposals = query.all()
                proposals = [db_proposal.to_pydantic() for db_proposal in db_proposals]
                
            logger.info(f"Retrieved {len(proposals)} goal proposals")
            return proposals
            
        except SQLAlchemyError as e:
            logger.error(f"Failed to retrieve goal proposals: {e}")
            return []
    
    def get_goal_proposal_by_id(self, proposal_id: UUID) -> Optional[GoalProposal]:
        """
        Retrieve a specific goal proposal by ID.
        
        Args:
            proposal_id: UUID of the proposal to retrieve
            
        Returns:
            GoalProposal object or None if not found
        """
        try:
            with self.SessionLocal() as session:
                db_proposal = session.query(GoalProposalDB).filter(
                    GoalProposalDB.id == proposal_id
                ).first()
                
                if db_proposal:
                    return db_proposal.to_pydantic()
                return None
                
        except SQLAlchemyError as e:
            logger.error(f"Failed to retrieve goal proposal {proposal_id}: {e}")
            return None
    
    def update_goal_proposal_status(self,
                                  proposal_id: UUID,
                                  new_status: "GoalProposalStatus") -> bool:
        """
        Update the status of a goal proposal.
        
        Args:
            proposal_id: UUID of the proposal to update
            new_status: New status to set
            
        Returns:
            True if successful, False otherwise
        """
        try:
            with self.SessionLocal() as session:
                db_proposal = session.query(GoalProposalDB).filter(
                    GoalProposalDB.id == proposal_id
                ).first()
                
                if db_proposal:
                    db_proposal.status = new_status
                    session.commit()
                    logger.info(f"Updated goal proposal {proposal_id} status to {new_status}")
                    return True
                else:
                    logger.warning(f"Goal proposal {proposal_id} not found")
                    return False
                    
        except SQLAlchemyError as e:
            logger.error(f"Failed to update goal proposal status: {e}")
            return False
    
    def get_recent_goal_proposals(self, days: int = 30) -> List[GoalProposal]:
        """
        Get goal proposals created in the last N days.
        
        Args:
            days: Number of days to look back
            
        Returns:
            List of recent GoalProposal objects
        """
        try:
            cutoff_date = datetime.utcnow() - timedelta(days=days)
            
            with self.SessionLocal() as session:
                db_proposals = session.query(GoalProposalDB).filter(
                    GoalProposalDB.created_at >= cutoff_date
                ).order_by(GoalProposalDB.created_at.desc()).all()
                
                proposals = [db_proposal.to_pydantic() for db_proposal in db_proposals]
                
            logger.info(f"Retrieved {len(proposals)} goal proposals from last {days} days")
            return proposals
            
        except SQLAlchemyError as e:
            logger.error(f"Failed to retrieve recent goal proposals: {e}")
            return []