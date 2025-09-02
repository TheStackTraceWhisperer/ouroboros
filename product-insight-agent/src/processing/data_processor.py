"""
Data processing service for normalizing and cleaning feedback data.

This module handles the processing of raw feedback data into standardized
FeedbackItem objects, including cleaning, validation, and normalization.
"""

import logging
import re
from typing import List, Optional
from datetime import datetime

from ..models import FeedbackItem

logger = logging.getLogger(__name__)


class DataProcessor:
    """Service for processing and normalizing feedback data."""

    def __init__(self):
        """Initialize the data processor."""
        # Common patterns for cleaning text
        self.url_pattern = re.compile(
            r"http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+"
        )
        self.mention_pattern = re.compile(r"@\w+")
        self.hashtag_pattern = re.compile(r"#\w+")
        self.excessive_whitespace = re.compile(r"\s+")

    def process_feedback_items(
        self, feedback_items: List[FeedbackItem]
    ) -> List[FeedbackItem]:
        """
        Process a list of feedback items.

        Args:
            feedback_items: List of raw FeedbackItem objects

        Returns:
            List of processed and cleaned FeedbackItem objects
        """
        logger.info(f"Processing {len(feedback_items)} feedback items")

        processed_items = []
        for item in feedback_items:
            try:
                processed_item = self.process_single_item(item)
                if processed_item and self._is_valid_feedback(processed_item):
                    processed_items.append(processed_item)
                else:
                    logger.debug(f"Filtered out invalid feedback item: {item.id}")
            except Exception as e:
                logger.warning(f"Error processing feedback item {item.id}: {e}")
                continue

        logger.info(f"Successfully processed {len(processed_items)} feedback items")
        return processed_items

    def process_single_item(self, item: FeedbackItem) -> Optional[FeedbackItem]:
        """
        Process a single feedback item.

        Args:
            item: Raw FeedbackItem object

        Returns:
            Processed FeedbackItem object or None if processing fails
        """
        try:
            # Create a copy to avoid modifying the original
            processed_item = item.model_copy(deep=True)

            # Clean and normalize content
            processed_item.content = self._clean_text(processed_item.content)

            # Clean title if present
            if processed_item.title:
                processed_item.title = self._clean_text(processed_item.title)

            # Normalize author name
            processed_item.author = self._normalize_author(processed_item.author)

            # Validate and clean timestamps
            if not self._is_valid_timestamp(processed_item.timestamp):
                logger.warning(f"Invalid timestamp for item {item.id}")
                return None

            return processed_item

        except Exception as e:
            logger.error(f"Error processing item {item.id}: {e}")
            return None

    def _clean_text(self, text: str) -> str:
        """
        Clean and normalize text content.

        Args:
            text: Raw text content

        Returns:
            Cleaned and normalized text
        """
        if not text:
            return ""

        # Remove URLs (but keep the content readable)
        text = self.url_pattern.sub("[URL]", text)

        # Clean up mentions and hashtags for better analysis
        # Keep them but normalize the format
        text = self.mention_pattern.sub(lambda m: f"@user", text)
        text = self.hashtag_pattern.sub(lambda m: f"#{m.group()[1:].lower()}", text)

        # Normalize whitespace
        text = self.excessive_whitespace.sub(" ", text)

        # Strip leading/trailing whitespace
        text = text.strip()

        # Remove markdown formatting that might interfere with analysis
        text = re.sub(r"\*\*(.*?)\*\*", r"\1", text)  # Bold
        text = re.sub(r"\*(.*?)\*", r"\1", text)  # Italic
        text = re.sub(r"~~(.*?)~~", r"\1", text)  # Strikethrough
        text = re.sub(r"`(.*?)`", r"\1", text)  # Inline code

        # Clean up code blocks
        text = re.sub(r"```.*?```", "[CODE_BLOCK]", text, flags=re.DOTALL)

        # Clean up excessive punctuation
        text = re.sub(r"[!]{2,}", "!", text)
        text = re.sub(r"[?]{2,}", "?", text)
        text = re.sub(r"[.]{3,}", "...", text)

        return text

    def _normalize_author(self, author: str) -> str:
        """
        Normalize author names for consistency.

        Args:
            author: Raw author name

        Returns:
            Normalized author name
        """
        if not author or author.lower() in (
            "deleted",
            "removed",
            "[deleted]",
            "[removed]",
        ):
            return "anonymous"

        # Remove leading/trailing whitespace and convert to lowercase
        return author.strip().lower()

    def _is_valid_feedback(self, item: FeedbackItem) -> bool:
        """
        Validate if a feedback item contains meaningful content.

        Args:
            item: Processed FeedbackItem object

        Returns:
            True if the feedback item is valid, False otherwise
        """
        # Check minimum content length
        if not item.content or len(item.content.strip()) < 10:
            return False

        # Check for spam indicators
        spam_indicators = [
            "buy now",
            "click here",
            "limited time",
            "act now",
            "make money",
            "work from home",
            "free trial",
        ]

        content_lower = item.content.lower()
        if any(indicator in content_lower for indicator in spam_indicators):
            return False

        # Check if content is just URLs or mentions
        cleaned_content = re.sub(r"@\w+|\[URL\]|#\w+", "", item.content).strip()
        if len(cleaned_content) < 5:
            return False

        return True

    def _is_valid_timestamp(self, timestamp: datetime) -> bool:
        """
        Validate if a timestamp is reasonable.

        Args:
            timestamp: Timestamp to validate

        Returns:
            True if timestamp is valid, False otherwise
        """
        if not timestamp:
            return False

        now = datetime.utcnow()

        # Timestamp shouldn't be in the future
        if timestamp > now:
            return False

        # Timestamp shouldn't be too old (e.g., more than 5 years)
        five_years_ago = now.replace(year=now.year - 5)
        if timestamp < five_years_ago:
            return False

        return True

    def get_processing_stats(
        self, original_items: List[FeedbackItem], processed_items: List[FeedbackItem]
    ) -> dict:
        """
        Get statistics about the processing operation.

        Args:
            original_items: List of original feedback items
            processed_items: List of processed feedback items

        Returns:
            Dictionary containing processing statistics
        """
        return {
            "original_count": len(original_items),
            "processed_count": len(processed_items),
            "filtered_count": len(original_items) - len(processed_items),
            "processing_rate": (
                len(processed_items) / len(original_items) if original_items else 0
            ),
            "sources": list(set(item.source for item in processed_items)),
            "date_range": (
                {
                    "earliest": min(
                        (item.timestamp for item in processed_items), default=None
                    ),
                    "latest": max(
                        (item.timestamp for item in processed_items), default=None
                    ),
                }
                if processed_items
                else None
            ),
        }
