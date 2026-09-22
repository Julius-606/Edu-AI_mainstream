# ⚡ Neon DB (PostgreSQL Serverless) Guide

## Overview
Trace utilizes [Neon.tech](https://neon.tech) serverless PostgreSQL for scalable, cloud-native storage with scale-to-zero efficiency.

## Connection String Configuration

In your `.env` or environment variables:
```env
NEON_DATABASE_URL=postgresql://user:password@ep-cool-flower-123456.us-east-2.aws.neon.tech/trace_vault?sslmode=require
```

### Key Connection Features in Trace:
1. **URI Normalization:** Both `backend/database.py` and `backend/app/db/session.py` detect legacy `postgres://` prefixes and convert them to `postgresql://` for modern SQLAlchemy 2.0+ compatibility.
2. **Mandatory SSL:** Appends `?sslmode=require` or `&sslmode=require` if missing, satisfying Neon's encrypted connection policy.
3. **Serverless Disconnect Recovery:**
   - `pool_pre_ping=True`: Verifies connections before checkout, preventing `ConnectionClosedError` when Neon scales back up from zero.
   - `pool_recycle=300`: Automatically recycles connections every 5 minutes to avoid hanging idle sockets.

## Initializing Tables in Neon

Run the table creation utility:
```bash
cd backend
python -c "from app.db.session import engine, Base; from app.models.database_models import *; Base.metadata.create_all(bind=engine); print('All Neon DB tables verified!')"
```
