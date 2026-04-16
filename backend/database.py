# IDENTITY: backend/database.py
# VERSION: 1.2.0
# ⚙️ GEAR 1.2: The Cloud-Ready Database (PostgreSQL/SQLite)

import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# Use DATABASE_URL from environment (Neon), fallback to local SQLite for development
SQLALCHEMY_DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./edu_ai_vault.db")

# Fix for Neon/Heroku: SQLAlchemy requires 'postgresql://' instead of 'postgres://'
if SQLALCHEMY_DATABASE_URL.startswith("postgres://"):
    SQLALCHEMY_DATABASE_URL = SQLALCHEMY_DATABASE_URL.replace("postgres://", "postgresql://", 1)

# connect_args={"check_same_thread": False} is ONLY required for SQLite
engine_args = {}
if SQLALCHEMY_DATABASE_URL.startswith("sqlite"):
    engine_args["connect_args"] = {"check_same_thread": False}

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
