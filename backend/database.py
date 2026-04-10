# IDENTITY: backend/database.py
# VERSION: 1.1.0
# ⚙️ GEAR 1.2: The Local Database (SQLite)
# This is our base currency. It handles the local ledger of all our data.

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# SQLite database file will be created in the local directory (survives KPLC blackouts)
SQLALCHEMY_DATABASE_URL = "sqlite:///./edu_ai_vault.db"

# Setting up the engine. connect_args are needed for SQLite to allow multiple threads
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)

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