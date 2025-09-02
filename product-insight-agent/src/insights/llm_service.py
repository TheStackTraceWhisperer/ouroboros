"""
LLM-based insight analysis service.

This module handles sentiment analysis and topic extraction using
various LLM providers (Anthropic Claude, Google Gemini).
"""

import json
import logging
from typing import List, Optional

import anthropic
import google.generativeai as genai

from ..config import settings
from ..models import (
    FeedbackItem, InsightAnalysis, SentimentAnalysis, TopicAnalysis,
    SentimentType, TopicTag
)

logger = logging.getLogger(__name__)


class LLMInsightService:
    """Service for analyzing feedback using Large Language Models."""
    
    def __init__(self):
        """Initialize the LLM insight service."""
        self.provider = settings.llm_provider.lower()
        
        if self.provider == "anthropic":
            if not settings.anthropic_api_key:
                raise ValueError("Anthropic API key is required when using anthropic provider")
            self.anthropic_client = anthropic.Anthropic(api_key=settings.anthropic_api_key)
            self.model = settings.anthropic_model
            
        elif self.provider == "google":
            if not settings.google_api_key:
                raise ValueError("Google API key is required when using google provider")
            genai.configure(api_key=settings.google_api_key)
            self.model = settings.google_model
            
        else:
            raise ValueError(f"Unsupported LLM provider: {self.provider}")
    
    def analyze_feedback_items(self, feedback_items: List[FeedbackItem]) -> List[InsightAnalysis]:
        """
        Analyze a list of feedback items for sentiment and topics.
        
        Args:
            feedback_items: List of FeedbackItem objects to analyze
            
        Returns:
            List of InsightAnalysis objects with sentiment and topic analysis
        """
        logger.info(f"Analyzing {len(feedback_items)} feedback items using {self.provider}")
        
        analyses = []
        for item in feedback_items:
            try:
                analysis = self.analyze_single_item(item)
                if analysis:
                    analyses.append(analysis)
            except Exception as e:
                logger.warning(f"Failed to analyze feedback item {item.id}: {e}")
                continue
                
        logger.info(f"Successfully analyzed {len(analyses)} feedback items")
        return analyses
    
    def analyze_single_item(self, item: FeedbackItem) -> Optional[InsightAnalysis]:
        """
        Analyze a single feedback item.
        
        Args:
            item: FeedbackItem to analyze
            
        Returns:
            InsightAnalysis object or None if analysis fails
        """
        try:
            # Prepare the content for analysis
            content_to_analyze = self._prepare_content(item)
            
            # Perform sentiment analysis
            sentiment_analysis = self._analyze_sentiment(content_to_analyze)
            
            # Perform topic analysis
            topic_analysis = self._analyze_topics(content_to_analyze)
            
            return InsightAnalysis(
                feedback_id=item.id,
                sentiment_analysis=sentiment_analysis,
                topic_analysis=topic_analysis
            )
            
        except Exception as e:
            logger.error(f"Error analyzing feedback item {item.id}: {e}")
            return None
    
    def _prepare_content(self, item: FeedbackItem) -> str:
        """
        Prepare content for LLM analysis.
        
        Args:
            item: FeedbackItem to prepare
            
        Returns:
            Prepared content string for analysis
        """
        content_parts = []
        
        if item.title:
            content_parts.append(f"Title: {item.title}")
            
        content_parts.append(f"Content: {item.content}")
        
        return "\n".join(content_parts)
    
    def _analyze_sentiment(self, content: str) -> SentimentAnalysis:
        """
        Analyze sentiment of the given content.
        
        Args:
            content: Text content to analyze
            
        Returns:
            SentimentAnalysis object
        """
        prompt = f"""
Analyze the sentiment of the following user feedback. Classify it as positive, negative, or neutral.

Consider the overall tone, emotional indicators, and expressed satisfaction/dissatisfaction.

User Feedback:
{content}

Respond with a JSON object in this exact format:
{{
    "sentiment": "positive|negative|neutral",
    "confidence": 0.85,
    "reasoning": "Brief explanation of the sentiment classification"
}}
"""
        
        try:
            response = self._call_llm(prompt)
            result = json.loads(response)
            
            return SentimentAnalysis(
                sentiment=SentimentType(result["sentiment"]),
                confidence=float(result["confidence"]),
                reasoning=result.get("reasoning")
            )
            
        except Exception as e:
            logger.warning(f"Error in sentiment analysis: {e}")
            # Return neutral sentiment with low confidence as fallback
            return SentimentAnalysis(
                sentiment=SentimentType.NEUTRAL,
                confidence=0.1,
                reasoning="Analysis failed, defaulting to neutral"
            )
    
    def _analyze_topics(self, content: str) -> TopicAnalysis:
        """
        Extract topics from the given content.
        
        Args:
            content: Text content to analyze
            
        Returns:
            TopicAnalysis object
        """
        topic_descriptions = {
            "bug": "Technical issues, errors, crashes, or malfunctioning features",
            "feature-request": "Requests for new features or functionality",
            "ui-feedback": "Comments about user interface, design, or user experience",
            "performance": "Issues or comments about speed, responsiveness, or efficiency",
            "documentation": "Feedback about documentation, help, or instructional content",
            "support": "Support requests, questions, or help-seeking behavior",
            "general": "General feedback that doesn't fit other categories"
        }
        
        topics_list = "\n".join([f"- {tag}: {desc}" for tag, desc in topic_descriptions.items()])
        
        prompt = f"""
Analyze the following user feedback and identify the primary topics being discussed.
Select 1-3 most relevant topics from the following categories:

{topics_list}

User Feedback:
{content}

Respond with a JSON object in this exact format:
{{
    "topics": ["topic1", "topic2"],
    "confidence": 0.85,
    "reasoning": "Brief explanation of why these topics were selected"
}}
"""
        
        try:
            response = self._call_llm(prompt)
            result = json.loads(response)
            
            # Validate topics
            valid_topics = []
            for topic in result["topics"]:
                try:
                    valid_topics.append(TopicTag(topic))
                except ValueError:
                    logger.warning(f"Invalid topic returned by LLM: {topic}")
            
            if not valid_topics:
                valid_topics = [TopicTag.GENERAL]
                
            return TopicAnalysis(
                topics=valid_topics,
                confidence=float(result["confidence"]),
                reasoning=result.get("reasoning")
            )
            
        except Exception as e:
            logger.warning(f"Error in topic analysis: {e}")
            # Return general topic with low confidence as fallback
            return TopicAnalysis(
                topics=[TopicTag.GENERAL],
                confidence=0.1,
                reasoning="Analysis failed, defaulting to general topic"
            )
    
    def _call_llm(self, prompt: str) -> str:
        """
        Call the configured LLM with the given prompt.
        
        Args:
            prompt: Prompt to send to the LLM
            
        Returns:
            Response text from the LLM
        """
        if self.provider == "anthropic":
            return self._call_anthropic(prompt)
        elif self.provider == "google":
            return self._call_google(prompt)
        else:
            raise ValueError(f"Unsupported provider: {self.provider}")
    
    def _call_anthropic(self, prompt: str) -> str:
        """
        Call Anthropic Claude API.
        
        Args:
            prompt: Prompt to send to Claude
            
        Returns:
            Response text from Claude
        """
        try:
            message = self.anthropic_client.messages.create(
                model=self.model,
                max_tokens=1000,
                temperature=0.1,
                messages=[
                    {"role": "user", "content": prompt}
                ]
            )
            return message.content[0].text
            
        except Exception as e:
            logger.error(f"Error calling Anthropic API: {e}")
            raise
    
    def _call_google(self, prompt: str) -> str:
        """
        Call Google Gemini API.
        
        Args:
            prompt: Prompt to send to Gemini
            
        Returns:
            Response text from Gemini
        """
        try:
            model = genai.GenerativeModel(self.model)
            response = model.generate_content(
                prompt,
                generation_config=genai.types.GenerationConfig(
                    temperature=0.1,
                    max_output_tokens=1000,
                )
            )
            return response.text
            
        except Exception as e:
            logger.error(f"Error calling Google API: {e}")
            raise
    
    def test_connection(self) -> bool:
        """
        Test the LLM API connection.
        
        Returns:
            True if connection is successful, False otherwise
        """
        try:
            test_prompt = "Respond with the word 'success' if you can see this message."
            response = self._call_llm(test_prompt)
            return "success" in response.lower()
            
        except Exception as e:
            logger.error(f"LLM connection test failed: {e}")
            return False