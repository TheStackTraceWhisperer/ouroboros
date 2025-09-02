"""
Reddit data ingestion connector using PRAW.

This module handles fetching posts and comments from Reddit using the PRAW library,
converting them into standardized FeedbackItem objects.
"""

import logging
from datetime import datetime
from typing import List, Optional

import praw
from praw.models import Submission

from ..config import settings
from ..models import FeedbackItem

logger = logging.getLogger(__name__)


class RedditConnector:
    """Connector for ingesting data from Reddit using PRAW."""

    def __init__(self):
        """Initialize Reddit connector with API credentials."""
        self.reddit = praw.Reddit(
            client_id=settings.reddit_client_id,
            client_secret=settings.reddit_client_secret,
            user_agent=settings.reddit_user_agent,
        )
        self.subreddit_name = settings.reddit_subreddit

    def ingest_recent_posts(self, limit: Optional[int] = None) -> List[FeedbackItem]:
        """
        Ingest recent posts from the configured subreddit.

        Args:
            limit: Maximum number of posts to fetch. If None, uses settings.max_posts_per_run

        Returns:
            List of FeedbackItem objects created from Reddit posts
        """
        if limit is None:
            limit = settings.max_posts_per_run

        logger.info(
            f"Ingesting up to {limit} recent posts from r/{self.subreddit_name}"
        )

        try:
            subreddit = self.reddit.subreddit(self.subreddit_name)
            feedback_items = []

            # Fetch recent posts (combination of hot and new)
            posts = list(subreddit.hot(limit=limit // 2)) + list(
                subreddit.new(limit=limit // 2)
            )

            for post in posts[:limit]:
                feedback_item = self._convert_post_to_feedback_item(post)
                if feedback_item:
                    feedback_items.append(feedback_item)

            logger.info(f"Successfully ingested {len(feedback_items)} posts")
            return feedback_items

        except Exception as e:
            logger.error(f"Error ingesting Reddit posts: {e}")
            raise

    def ingest_posts_since(
        self, since: datetime, limit: Optional[int] = None
    ) -> List[FeedbackItem]:
        """
        Ingest posts created since a specific datetime.

        Args:
            since: Datetime to fetch posts after
            limit: Maximum number of posts to fetch

        Returns:
            List of FeedbackItem objects created from recent Reddit posts
        """
        if limit is None:
            limit = settings.max_posts_per_run

        logger.info(f"Ingesting posts from r/{self.subreddit_name} since {since}")

        try:
            subreddit = self.reddit.subreddit(self.subreddit_name)
            feedback_items = []

            # Search for recent posts
            # Note: Reddit's API doesn't provide perfect time-based filtering,
            # so we fetch recent posts and filter by timestamp
            posts = subreddit.new(
                limit=limit * 2
            )  # Fetch more to account for filtering

            for post in posts:
                post_time = datetime.fromtimestamp(post.created_utc)
                if post_time >= since:
                    feedback_item = self._convert_post_to_feedback_item(post)
                    if feedback_item:
                        feedback_items.append(feedback_item)

                    if len(feedback_items) >= limit:
                        break
                else:
                    # Posts are ordered by recency, so we can break early
                    break

            logger.info(
                f"Successfully ingested {len(feedback_items)} posts since {since}"
            )
            return feedback_items

        except Exception as e:
            logger.error(f"Error ingesting Reddit posts since {since}: {e}")
            raise

    def _convert_post_to_feedback_item(
        self, post: Submission
    ) -> Optional[FeedbackItem]:
        """
        Convert a Reddit submission to a FeedbackItem.

        Args:
            post: Reddit submission object

        Returns:
            FeedbackItem object or None if conversion fails
        """
        try:
            # Skip removed or deleted posts
            if post.selftext in ("[removed]", "[deleted]") or not post.selftext.strip():
                return None

            return FeedbackItem(
                source="reddit",
                source_id=post.id,
                source_url=f"https://reddit.com{post.permalink}",
                author=str(post.author) if post.author else "deleted",
                title=post.title,
                content=post.selftext,
                timestamp=datetime.fromtimestamp(post.created_utc),
                raw_data={
                    "score": post.score,
                    "upvote_ratio": post.upvote_ratio,
                    "num_comments": post.num_comments,
                    "subreddit": str(post.subreddit),
                    "flair": post.link_flair_text,
                    "is_self": post.is_self,
                    "permalink": post.permalink,
                    "url": post.url if not post.is_self else None,
                },
            )

        except Exception as e:
            logger.warning(f"Failed to convert post {post.id} to FeedbackItem: {e}")
            return None

    def test_connection(self) -> bool:
        """
        Test the Reddit API connection.

        Returns:
            True if connection is successful, False otherwise
        """
        try:
            # Try to access the configured subreddit
            subreddit = self.reddit.subreddit(self.subreddit_name)
            # This will raise an exception if the subreddit doesn't exist or we lack access
            subreddit.display_name
            logger.info(f"Successfully connected to r/{self.subreddit_name}")
            return True

        except Exception as e:
            logger.error(f"Failed to connect to Reddit: {e}")
            return False
