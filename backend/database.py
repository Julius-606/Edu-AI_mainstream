
# IDENTITY: backend/database.py
# VERSION: 1.2.0
# ⚙️ GEAR 1.2: The Cloud-Ready Database (PostgreSQL/SQLite)

import os
import logging
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

logger = logging.getLogger("trace_db_legacy")
load_dotenv()

# Use DATABASE_URL from environment (Neon), fallback to local SQLite for development
raw_db_url = os.getenv("DATABASE_URL", "sqlite:///./edu_ai_vault.db")

# Fix for Neon/Heroku: SQLAlchemy requires 'postgresql://' instead of 'postgres://'
if raw_db_url.startswith("postgres://"):
    raw_db_url = raw_db_url.replace("postgres://", "postgresql://", 1)

engine_args = {}

if "postgresql" in raw_db_url:
    if "sslmode" not in raw_db_url:
        separator = "&" if "?" in raw_db_url else "?"
        raw_db_url += f"{separator}sslmode=require"

    engine_args["connect_args"] = {
        "connect_timeout": 10,
        "keepalives": 1,
        "keepalives_idle": 30,
        "keepalives_interval": 10,
        "keepalives_count": 5
    }
    engine_args["pool_size"] = 5
    engine_args["max_overflow"] = 10
elif "sqlite" in raw_db_url:
    engine_args["connect_args"] = {"check_same_thread": False}

SQLALCHEMY_DATABASE_URL = raw_db_url

def create_configured_engine(url, args):
    return create_engine(
        url,
        pool_pre_ping=True,
        pool_recycle=300,
        **args
    )

try:
    engine = create_configured_engine(SQLALCHEMY_DATABASE_URL, engine_args)
    with engine.connect() as conn:
        pass
except Exception as e:
    logger.warning(f"Connection failed ({e}). Falling back to local SQLite 'edu_ai_vault.db'.")
    SQLALCHEMY_DATABASE_URL = "sqlite:///./edu_ai_vault.db"
    engine = create_configured_engine(SQLALCHEMY_DATABASE_URL, {"connect_args": {"check_same_thread": False}})

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


 