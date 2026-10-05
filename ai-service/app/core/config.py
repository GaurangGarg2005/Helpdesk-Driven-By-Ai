"""
Application configuration using Pydantic BaseSettings.
Loads values from environment variables and .env file.
"""

from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    APP_NAME: str = "HelpDeskAI AI Service"
    DEBUG: bool = False
    API_KEY: str = "helpdesk-ai-internal-key-change-me"
    GEMINI_API_KEY: str = ""

    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
        "case_sensitive": True,
    }


@lru_cache()
def get_settings() -> Settings:
    """Cached settings singleton to avoid re-reading .env on every request."""
    return Settings()
