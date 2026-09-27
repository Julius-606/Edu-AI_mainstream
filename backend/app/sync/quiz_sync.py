import logging
from typing import List, Dict, Any
from sqlalchemy.orm import Session
from app.models import database_models as models

logger = logging.getLogger("edu_ai.sync.quiz")

def fetch_user_quizzes(db: Session, user_id: int) -> List[Dict[str, Any]]:
    """
    Fetch quiz history records for a student.
    """
    try:
        quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user_id).all()
        return [
            {
                "id": q.id,
                "unit_name": q.unit_name,
                "score": q.score,
                "total": q.total,
                "pnl": q.pnl,
                "timestamp": q.timestamp
            }
            for q in quizzes
        ]
    except Exception as err:
        logger.error(f"Error fetching quiz history for user {user_id}: {err}")
        return []

def sync_user_quizzes(db: Session, user_id: int, quizzes_payload: List[Any]) -> Dict[str, Any]:
    """
    Synchronizes quiz attempts from client.
    """
    synced_count = 0
    for q_item in quizzes_payload:
        try:
            unit_name = getattr(q_item, "unit_name", "General Assessment")
            score = getattr(q_item, "score", 0)
            total = getattr(q_item, "total", 5)
            pnl = getattr(q_item, "pnl", 0.0)
            timestamp = str(getattr(q_item, "timestamp", ""))
            
            # Avoid duplicate records with identical timestamp and unit
            existing = db.query(models.QuizHistory).filter(
                models.QuizHistory.owner_id == user_id,
                models.QuizHistory.unit_name == unit_name,
                models.QuizHistory.timestamp == timestamp
            ).first()
            
            if not existing:
                db.add(models.QuizHistory(
                    unit_name=unit_name,
                    score=score,
                    total=total,
                    pnl=pnl,
                    timestamp=timestamp,
                    owner_id=user_id
                ))
                synced_count += 1
        except Exception as err:
            logger.warning(f"Error syncing quiz record: {err}")
            
    try:
        db.commit()
    except Exception as err:
        db.rollback()
        logger.error(f"Failed to commit quiz sync: {err}")
        
    return {"synced": synced_count}
