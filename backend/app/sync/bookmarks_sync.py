import logging
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import text, inspect
from app.models import database_models as models

logger = logging.getLogger("edu_ai.sync.bookmarks")

def ensure_bookmarks_schema(db: Session):
    """
    Auto-healing migration for bookmarks table. Ensures target, context, and all fields exist.
    Uses dedicated autocommit connection to avoid aborting active transactions.
    """
    try:
        bind = db.get_bind()
        is_postgres = "postgresql" in str(bind.url)
        
        # Connect in autocommit mode so DDL executes independently without aborting caller transaction
        with bind.connect().execution_options(isolation_level="AUTOCOMMIT") as conn:
            # 1. Ensure bookmarks table exists
            if is_postgres:
                conn.execute(text("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id SERIAL PRIMARY KEY,
                        type VARCHAR(50) DEFAULT 'general',
                        title VARCHAR(200) DEFAULT 'Saved Item',
                        target VARCHAR(500) DEFAULT '',
                        context TEXT,
                        notes TEXT,
                        timestamp DOUBLE PRECISION DEFAULT 0,
                        owner_id INTEGER
                    );
                """))
            else:
                conn.execute(text("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        type VARCHAR(50) DEFAULT 'general',
                        title VARCHAR(200) DEFAULT 'Saved Item',
                        target VARCHAR(500) DEFAULT '',
                        context TEXT,
                        notes TEXT,
                        timestamp FLOAT DEFAULT 0,
                        owner_id INTEGER
                    );
                """))

            # 2. Proactively add columns directly
            alter_statements = [
                ("type", "VARCHAR(50) DEFAULT 'general'"),
                ("title", "VARCHAR(200) DEFAULT 'Saved Item'"),
                ("target", "VARCHAR(500) DEFAULT ''"),
                ("context", "TEXT"),
                ("notes", "TEXT"),
                ("timestamp", "DOUBLE PRECISION DEFAULT 0" if is_postgres else "FLOAT DEFAULT 0"),
                ("owner_id", "INTEGER")
            ]

            for col_name, col_def in alter_statements:
                try:
                    if is_postgres:
                        conn.execute(text(f"ALTER TABLE bookmarks ADD COLUMN IF NOT EXISTS {col_name} {col_def};"))
                    else:
                        conn.execute(text(f"ALTER TABLE bookmarks ADD COLUMN {col_name} {col_def};"))
                except Exception as col_err:
                    err_msg = str(col_err).lower()
                    if "duplicate column name" in err_msg or "already exists" in err_msg:
                        pass
                    else:
                        logger.warning(f"Could not ensure column {col_name} in bookmarks: {col_err}")
    except Exception as err:
        logger.warning(f"Error during proactive bookmarks schema verification: {err}")
        try:
            db.rollback()
        except Exception:
            pass

def fetch_user_bookmarks(db: Session, user_id: int) -> List[Dict[str, Any]]:
    """
    Fault-tolerant retrieval of user bookmarks that dynamically adapts to table columns.
    """
    ensure_bookmarks_schema(db)
    
    try:
        inspector = inspect(db.get_bind())
        existing_cols = {c["name"].lower() for c in inspector.get_columns("bookmarks")}
        
        required_cols = {"id", "type", "title", "target", "context", "timestamp", "owner_id"}
        if required_cols.issubset(existing_cols):
            try:
                bookmarks = db.query(models.Bookmark).filter(models.Bookmark.owner_id == user_id).all()
                return [
                    {
                        "id": b.id,
                        "type": getattr(b, "type", "general") or "general",
                        "title": getattr(b, "title", "Saved Item") or "Saved Item",
                        "target": getattr(b, "target", "") or "",
                        "context": getattr(b, "context", "") or "",
                        "timestamp": getattr(b, "timestamp", 0.0) or 0.0
                    }
                    for b in bookmarks
                ]
            except Exception as query_err:
                logger.warning(f"Standard bookmark query failed: {query_err}. Falling back to safe query.")
                try:
                    db.rollback()
                except Exception:
                    pass
                # Fall through to raw SQL fallback
        
        # Table is missing some columns or query failed: construct safe dynamic query with fallbacks
        cols_selected = []
        for col in ["id", "type", "title", "target", "context", "timestamp"]:
            if col in existing_cols:
                cols_selected.append(f"bookmarks.{col}")
            else:
                default_val = "''" if col in ["type", "title", "target", "context"] else "0"
                cols_selected.append(f"{default_val} AS {col}")
        
        sql = f"SELECT {', '.join(cols_selected)} FROM bookmarks WHERE owner_id = :uid"
        rows = db.execute(text(sql), {"uid": user_id}).fetchall()
        return [
            {
                "id": r[0],
                "type": r[1] or "general",
                "title": r[2] or "Saved Item",
                "target": r[3] or "",
                "context": r[4] or "",
                "timestamp": float(r[5]) if r[5] is not None else 0.0
            }
            for r in rows
        ]
    except Exception as err:
        logger.error(f"Error fetching bookmarks for user {user_id}: {err}")
        try:
            db.rollback()
        except Exception:
            pass
        return []

def sync_user_bookmarks(db: Session, user_id: int, bookmarks_payload: List[Any]) -> Dict[str, Any]:
    """
    Synchronizes bookmark records from client payload into database with per-item isolation.
    """
    ensure_bookmarks_schema(db)
    synced_count = 0
    errors = 0
    
    for b_item in bookmarks_payload:
        try:
            b_target = getattr(b_item, "target", "") or ""
            b_type = getattr(b_item, "type", "general") or "general"
            b_title = getattr(b_item, "title", "Saved Item") or "Saved Item"
            b_context = getattr(b_item, "context", "") or ""
            b_timestamp = getattr(b_item, "timestamp", 0.0) or 0.0
            
            existing_b = None
            try:
                existing_b = db.query(models.Bookmark).filter(
                    models.Bookmark.owner_id == user_id,
                    models.Bookmark.target == b_target,
                    models.Bookmark.type == b_type
                ).first()
            except Exception:
                db.rollback()
            
            if not existing_b:
                new_bm = models.Bookmark(
                    type=b_type,
                    title=b_title,
                    target=b_target,
                    context=b_context,
                    timestamp=b_timestamp,
                    owner_id=user_id
                )
                db.add(new_bm)
                db.commit()
                synced_count += 1
            else:
                existing_b.title = b_title
                existing_b.context = b_context
                existing_b.timestamp = b_timestamp
                db.commit()
        except Exception as err:
            try:
                db.rollback()
            except Exception:
                pass
            logger.warning(f"Error syncing individual bookmark: {err}")
            errors += 1
            
    return {"synced": synced_count, "errors": errors}
