
# IDENTITY: backend/database.py
# VERSION: 1.3.0
# ⚙️ GEAR 1.2: The Cloud-Ready Database (Neon PostgreSQL/SQLite)

import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

load_dotenv()

# Use DATABASE_URL or NEON_DATABASE_URL from environment (Neon), fallback to local SQLite for development
SQLALCHEMY_DATABASE_URL = os.getenv("NEON_DATABASE_URL") or os.getenv("DATABASE_URL", "sqlite:///./edu_ai_vault.db")

# Fix for Neon/Heroku: SQLAlchemy requires 'postgresql://' instead of 'postgres://'
if SQLALCHEMY_DATABASE_URL.startswith("postgres://"):
    SQLALCHEMY_DATABASE_URL = SQLALCHEMY_DATABASE_URL.replace("postgres://", "postgresql://", 1)

# Ensure Neon serverless PostgreSQL has sslmode=require
if "postgresql" in SQLALCHEMY_DATABASE_URL and "sslmode" not in SQLALCHEMY_DATABASE_URL:
    if "?" in SQLALCHEMY_DATABASE_URL:
        SQLALCHEMY_DATABASE_URL += "&sslmode=require"
    else:
        SQLALCHEMY_DATABASE_URL += "?sslmode=require"

# connect_args={"check_same_thread": False} is ONLY required for SQLite
engine_args = {}
if SQLALCHEMY_DATABASE_URL.startswith("sqlite"):
    engine_args["connect_args"] = {"check_same_thread": False}
else:
    # Optimized for Neon serverless idle disconnect resilience
    engine_args["pool_pre_ping"] = True
    engine_args["pool_recycle"] = 300

# Setting up the engine
engine = create_engine(SQLALCHEMY_DATABASE_URL, **engine_args)

# SessionLocal is the actual database session we use to query and save data
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class for our database models
Base = declarative_base()

# Dependency to get the DB session in our FastAPI routes
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()



