"""
Database migration script to create goal_proposals table.

This script creates the goal_proposals table to store generated goal proposals
based on trend analysis results.

Usage:
    python migrations/001_create_goal_proposals_table.py
"""

import sys
import os
from datetime import datetime

# Add the parent directory to the path to import our modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sqlalchemy import create_engine, text
from src.config import settings
from src.storage.database_models import Base, GoalProposalDB


def run_migration():
    """Run the migration to create goal_proposals table."""
    print("Starting database migration: Create goal_proposals table")
    
    # Create database engine
    engine = create_engine(settings.database_url)
    
    try:
        # Create the goal_proposals table
        GoalProposalDB.__table__.create(engine, checkfirst=True)
        print("✓ Successfully created goal_proposals table")
        
        # Verify table was created
        with engine.connect() as conn:
            result = conn.execute(text("""
                SELECT COUNT(*) as table_count 
                FROM information_schema.tables 
                WHERE table_name = 'goal_proposals'
            """))
            table_count = result.fetchone()[0]
            
            if table_count > 0:
                print("✓ Table verification successful")
            else:
                print("⚠ Warning: Table creation may not have succeeded")
        
        print("Migration completed successfully!")
        
    except Exception as e:
        print(f"❌ Migration failed: {e}")
        raise
    

def rollback_migration():
    """Rollback the migration by dropping the goal_proposals table."""
    print("Rolling back migration: Drop goal_proposals table")
    
    engine = create_engine(settings.database_url)
    
    try:
        # Drop the goal_proposals table
        GoalProposalDB.__table__.drop(engine, checkfirst=True)
        print("✓ Successfully dropped goal_proposals table")
        print("Rollback completed successfully!")
        
    except Exception as e:
        print(f"❌ Rollback failed: {e}")
        raise


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Database migration for goal_proposals table")
    parser.add_argument("--rollback", action="store_true", help="Rollback the migration")
    args = parser.parse_args()
    
    if args.rollback:
        rollback_migration()
    else:
        run_migration()