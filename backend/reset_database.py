
import os
import sys
from sqlalchemy import text

# Add current directory to path if needed
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

try:
    from database import engine, Base
    import models
except ImportError:
    # Fallback for different execution contexts
    from .database import engine, Base
    from . import models

import argparse

def reset_database(force=False):
    print("⚠️  DATABASE RESET TOOL (Neon/PostgreSQL) ⚠️")
    print(f"Connecting to: {engine.url.render_as_string(hide_password=True)}")
    
    if not force:
        confirm = input("Are you sure you want to WIPE all data? This cannot be undone! (yes/no): ")
        if confirm.lower() != 'yes':
            print("❌ Aborted.")
            return

    try:
        print("🚀 Dropping all tables...")
        Base.metadata.drop_all(bind=engine)
        print("✅ Tables dropped.")

        print("🏗️  Recreating tables from models...")
        Base.metadata.create_all(bind=engine)
        print("✅ Schema recreated successfully.")
        
    except Exception as e:
        print(f"❌ Error during reset: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--force", action="store_true", help="Skip confirmation")
    args = parser.parse_args()
    reset_database(force=args.force)


