"""Deployment-aware environment loading for local runs and Hugging Face Spaces."""

import os
from pathlib import Path
from dotenv import load_dotenv


def running_on_huggingface() -> bool:
    """Detect the environment variables injected by Hugging Face Spaces."""
    return bool(
        os.getenv("SPACE_ID")
        or os.getenv("HF_SPACE_ID")
        or os.getenv("SPACE_HOST")
    )


def load_runtime_environment() -> bool:
    """Load local .env values only outside hosted Hugging Face Spaces."""
    if running_on_huggingface():
        return False
    load_dotenv(Path(__file__).resolve().parents[2] / ".env")
    return True
