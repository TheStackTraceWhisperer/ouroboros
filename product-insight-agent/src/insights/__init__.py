"""Insights module for the Product Insight Agent."""

from .llm_service import LLMInsightService
from .goal_service import GoalGenerationService

__all__ = ["LLMInsightService", "GoalGenerationService"]
