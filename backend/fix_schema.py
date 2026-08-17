import os
from sqlalchemy import create_engine, text
from dotenv import load_dotenv
from app.db.session import engine, Base
from app.models import database_models # Import models to ensure they are registered

load_dotenv()

def fix_schema():
    print("🚀 Synchronizing Database Schema for 5-Level Ingestion...")

    # We use a raw connection to drop tables with dependencies
    with engine.connect() as conn:
        print("🗑️  Dropping old hierarchy tables...")
        # Order matters for foreign keys
        tables_to_drop = [
            "learning_objectives",
            "subtopics",
            "topics",
            "modules",
            "units"
        ]

        for table in tables_to_drop:
            try:
                conn.execute(text(f"DROP TABLE IF EXISTS {table} CASCADE"))
                print(f"✅ Dropped {table}")
            except Exception as e:
                print(f"⚠️  Could not drop {table}: {e}")

        conn.commit()

    print("🏗️  Recreating tables with new schema...")
    Base.metadata.create_all(bind=engine)
    print("✅ Schema fixed successfully!")

if __name__ == "__main__":
    fix_schema()
