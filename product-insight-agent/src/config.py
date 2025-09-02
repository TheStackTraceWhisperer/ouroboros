"""Configuration management for the Product Insight Agent."""

from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application configuration settings."""

    # Reddit API Configuration
    reddit_client_id: str = Field("test_client_id", env="REDDIT_CLIENT_ID")
    reddit_client_secret: str = Field(
        "test_client_secret", env="REDDIT_CLIENT_SECRET"
    )
    reddit_user_agent: str = Field(
        "ProductInsightAgent/1.0", env="REDDIT_USER_AGENT"
    )
    reddit_subreddit: str = Field("OurAppCommunity", env="REDDIT_SUBREDDIT")

    # LLM Configuration
    llm_provider: str = Field("anthropic", env="LLM_PROVIDER")
    anthropic_api_key: Optional[str] = Field(None, env="ANTHROPIC_API_KEY")
    anthropic_model: str = Field(
        "claude-3-haiku-20240307", env="ANTHROPIC_MODEL"
    )
    google_api_key: Optional[str] = Field(None, env="GOOGLE_API_KEY")
    google_model: str = Field("gemini-1.5-flash", env="GOOGLE_MODEL")

    # Database Configuration
    database_url: str = Field(
        "sqlite:///./product_insights.db", env="DATABASE_URL"
    )
    database_schema: str = Field("product_insights", env="DATABASE_SCHEMA")

    # Scheduling Configuration
    schedule_enabled: bool = Field(True, env="SCHEDULE_ENABLED")
    report_schedule_time: str = Field("09:00", env="REPORT_SCHEDULE_TIME")

    # Application Settings
    log_level: str = Field("INFO", env="LOG_LEVEL")
    max_posts_per_run: int = Field(100, env="MAX_POSTS_PER_RUN")
    days_to_analyze: int = Field(1, env="DAYS_TO_ANALYZE")

    # Storage paths
    data_dir: str = Field("./data", env="DATA_DIR")
    reports_dir: str = Field("./reports", env="REPORTS_DIR")

    class Config:
        """Pydantic configuration."""

        env_file = ".env"
        env_file_encoding = "utf-8"


def get_settings() -> Settings:
    """Get application settings instance."""
    return Settings()


# Global settings instance
settings = get_settings()
