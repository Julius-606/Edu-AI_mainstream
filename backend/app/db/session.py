import os
import logging
from pathlib import Path
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger("DATABASE")

BACKEND_DIR = Path(__file__).resolve().parents[2]
LOCAL_DATABASE_URL = f"sqlite:///{BACKEND_DIR / 'edu_ai_vault.db'}"
configured_database_url = os.getenv("DATABASE_URL")
SQLALCHEMY_DATABASE_URL = configured_database_url or LOCAL_DATABASE_URL

if SQLALCHEMY_DATABASE_URL.startswith("postgres://"):
    SQLALCHEMY_DATABASE_URL = SQLALCHEMY_DATABASE_URL.replace("postgres://", "postgresql://", 1)

if "postgresql" in SQLALCHEMY_DATABASE_URL and "sslmode" not in SQLALCHEMY_DATABASE_URL:
    if "?" in SQLALCHEMY_DATABASE_URL:
        SQLALCHEMY_DATABASE_URL += "&sslmode=require"
    else:
        SQLALCHEMY_DATABASE_URL += "?sslmode=require"

engine_args = {}
if "sqlite" in SQLALCHEMY_DATABASE_URL:
    engine_args["connect_args"] = {"check_same_thread": False}
else:
    # Do not block application startup indefinitely when the hosted database is unavailable.
    engine_args["connect_args"] = {"connect_timeout": 5}

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    pool_pre_ping=True,
    pool_recycle=300,
    **engine_args
)

if configured_database_url:
    try:
        with engine.connect() as connection:
            connection.execute(text("SELECT 1"))
    except Exception as error:
        logger.warning(
            "Configured database is unavailable (%s). Falling back to local SQLite at %s.",
            error,
            BACKEND_DIR / "edu_ai_vault.db",
        )
        engine = create_engine(
            LOCAL_DATABASE_URL,
            connect_args={"check_same_thread": False},
        )

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
