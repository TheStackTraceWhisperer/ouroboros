"""Tests for data processing functionality."""

import pytest
from src.processing import DataProcessor


class TestDataProcessor:
    """Test cases for DataProcessor."""
    
    def setup_method(self):
        """Set up test instance."""
        self.processor = DataProcessor()
    
    def test_clean_text_basic(self):
        """Test basic text cleaning."""
        text = "This is a test with    extra    spaces."
        cleaned = self.processor._clean_text(text)
        
        assert "extra    spaces" not in cleaned
        assert "extra spaces" in cleaned
    
    def test_clean_text_urls(self):
        """Test URL replacement in text."""
        text = "Check out this link: https://example.com for more info."
        cleaned = self.processor._clean_text(text)
        
        assert "https://example.com" not in cleaned
        assert "[URL]" in cleaned
    
    def test_clean_text_mentions(self):
        """Test mention normalization."""
        text = "Hey @johndoe and @janedoe, what do you think?"
        cleaned = self.processor._clean_text(text)
        
        assert "@johndoe" not in cleaned
        assert "@janedoe" not in cleaned
        assert "@user" in cleaned
    
    def test_clean_text_markdown(self):
        """Test markdown formatting removal."""
        text = "This is **bold** and *italic* and ~~strikethrough~~ text."
        cleaned = self.processor._clean_text(text)
        
        assert "**bold**" not in cleaned
        assert "*italic*" not in cleaned
        assert "~~strikethrough~~" not in cleaned
        assert "bold" in cleaned
        assert "italic" in cleaned
        assert "strikethrough" in cleaned
    
    def test_normalize_author(self):
        """Test author name normalization."""
        # Normal author
        assert self.processor._normalize_author("TestUser") == "testuser"
        
        # Deleted author
        assert self.processor._normalize_author("deleted") == "anonymous"
        assert self.processor._normalize_author("[deleted]") == "anonymous"
        
        # Empty author
        assert self.processor._normalize_author("") == "anonymous"
    
    def test_is_valid_feedback(self, sample_feedback_item):
        """Test feedback validation."""
        # Valid feedback
        assert self.processor._is_valid_feedback(sample_feedback_item)
        
        # Invalid - too short content
        sample_feedback_item.content = "short"
        assert not self.processor._is_valid_feedback(sample_feedback_item)
        
        # Invalid - spam content
        sample_feedback_item.content = "BUY NOW! Limited time offer! Click here to make money!"
        assert not self.processor._is_valid_feedback(sample_feedback_item)
    
    def test_process_feedback_items(self, sample_feedback_items):
        """Test processing multiple feedback items."""
        processed = self.processor.process_feedback_items(sample_feedback_items)
        
        assert len(processed) <= len(sample_feedback_items)
        
        for item in processed:
            assert len(item.content.strip()) >= 10
            assert item.author != "deleted"
    
    def test_process_single_item(self, sample_feedback_item):
        """Test processing a single feedback item."""
        # Add some messy content to test cleaning
        sample_feedback_item.content = "This is **bold** text with https://example.com and @user mention."
        sample_feedback_item.author = "TestUser123"
        
        processed = self.processor.process_single_item(sample_feedback_item)
        
        assert processed is not None
        assert "**bold**" not in processed.content
        assert "[URL]" in processed.content
        assert "@user" in processed.content
        assert processed.author == "testuser123"
    
    def test_processing_stats(self, sample_feedback_items):
        """Test processing statistics calculation."""
        # Process items (some might be filtered)
        processed = self.processor.process_feedback_items(sample_feedback_items)
        
        stats = self.processor.get_processing_stats(sample_feedback_items, processed)
        
        assert stats["original_count"] == len(sample_feedback_items)
        assert stats["processed_count"] == len(processed)
        assert stats["filtered_count"] >= 0
        assert 0 <= stats["processing_rate"] <= 1
        assert "reddit" in stats["sources"]