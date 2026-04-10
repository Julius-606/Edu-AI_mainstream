# IDENTITY: backend/migrate_data.py
# VERSION: 1.1.0
# ⚙️ GEAR 1.3: Data Migration

import json
from database import SessionLocal, engine, Base
import models
from datetime import datetime, timedelta
import random

Base.metadata.create_all(bind=engine)

def migrate():
    db = SessionLocal()
    
    try:
        with open('../config.json', 'r') as f:
            data = json.load(f)
            
        print("📥 config.json loaded. Preparing to migrate...")

        username = data.get("user_name", "Future Doc")
        user = db.query(models.User).filter(models.User.username == username).first()
        
        if not user:
            user = models.User(
                username=username,
                role="Student",
                sensory_mode="Standard",
                difficulty=data.get("difficulty", "Asian Parent Expectations (Extreme)"),
                semester_status=data.get("semester_status", "Year 4 - Redemption Arc"),
                interests=data.get("interests", [])
            )
            db.add(user)
            db.commit()
            db.refresh(user)
            print(f"✅ User '{username}' created in DB with ID: {user.id}")
        else:
            print(f"⚠️ User '{username}' already exists. Skipping creation.")

        # Migrate Units
        current_units = data.get("current_units", [])
        for unit_name in current_units:
            existing_unit = db.query(models.Unit).filter(
                models.Unit.owner_id == user.id, 
                models.Unit.name == unit_name
            ).first()
            
            if not existing_unit:
                new_unit = models.Unit(name=unit_name, is_active=True, owner_id=user.id)
                db.add(new_unit)
                
        # Generate some mock performance data for the charts
        print("📊 Generating mock performance logs...")
        subjects = ["Biochemistry II", "General Surgery", "Internal Medicine I"]
        base_date = datetime.now() - timedelta(days=30)
        
        for subject in subjects:
            for i in range(5):
                log_date = base_date + timedelta(days=i*6)
                grade = random.uniform(50.0, 95.0)
                new_log = models.PerformanceLog(
                    subject=subject,
                    grade=grade,
                    timestamp=log_date.strftime("%Y-%m-%d"),
                    owner_id=user.id
                )
                db.add(new_log)

        db.commit()
        print("✅ Account funded! Units and mock performance data migrated successfully. 📈")

    except FileNotFoundError:
        print("❌ Error: config.json not found. Ensure it is one directory above this script (../config.json).")
    except Exception as e:
        print(f"❌ Critical Error during migration: {e}")
    finally:
        db.close()

if __name__ == "__main__":
    migrate()