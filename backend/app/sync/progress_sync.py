import logging
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from app.models import database_models as models

logger = logging.getLogger("edu_ai.sync.progress")

def fetch_user_progress(db: Session, user_id: int) -> List[Dict[str, Any]]:
    """
    Fetch syllabus progress nodes for a student.
    """
    try:
        progress = db.query(models.UserSyllabusProgress).filter(
            models.UserSyllabusProgress.user_id == user_id
        ).all()
        return [
            {
                "node_id": p.node_id,
                "node_type": p.node_type,
                "status": p.status,
                "last_studied_at": p.last_studied_at
            }
            for p in progress
        ]
    except Exception as err:
        logger.error(f"Error fetching syllabus progress for user {user_id}: {err}")
        return []

def sync_user_progress(db: Session, user_id: int, progress_payload: List[Any]) -> Dict[str, Any]:
    """
    Synchronizes syllabus progress nodes and cascades subtopic completion.
    """
    synced_count = 0
    errors = 0
    
    for p_item in progress_payload:
        try:
            node_id = getattr(p_item, "node_id", None)
            node_type = getattr(p_item, "node_type", "subtopic")
            status = getattr(p_item, "status", "In_Progress")
            last_studied_at = getattr(p_item, "last_studied_at", None)
            
            if node_id is None:
                continue

            existing = db.query(models.UserSyllabusProgress).filter(
                models.UserSyllabusProgress.user_id == user_id,
                models.UserSyllabusProgress.node_id == node_id,
                models.UserSyllabusProgress.node_type == node_type
            ).first()
            
            is_completed_status = (status == "Completed")
            
            if existing:
                existing.status = status
                existing.last_studied_at = last_studied_at
            else:
                db.add(models.UserSyllabusProgress(
                    user_id=user_id,
                    node_id=node_id,
                    node_type=node_type,
                    status=status,
                    last_studied_at=last_studied_at
                ))
            synced_count += 1
            
            # Cascade Subtopic table completion status
            if node_type == "subtopic":
                sub = db.query(models.Subtopic).filter(models.Subtopic.id == node_id).first()
                if sub:
                    sub.is_completed = is_completed_status
        except Exception as err:
            logger.warning(f"Error syncing progress item: {err}")
            errors += 1
            
    try:
        db.commit()
    except Exception as err:
        db.rollback()
        logger.error(f"Failed to commit syllabus progress: {err}")
        
    return {"synced": synced_count, "errors": errors}
