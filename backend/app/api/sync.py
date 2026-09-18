from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import time
import logging
from app.db.session import get_db
from app.models import database_models as models

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/sync", tags=["Synchronization"])

class SyncOperationSchema(BaseModel):
    operationId: str
    entityType: str
    entityId: int
    payload: Dict[str, Any] = {}

class SyncRequestSchema(BaseModel):
    userId: str
    operations: List[SyncOperationSchema] = []

class SyncResponseSchema(BaseModel):
    appliedOperationIds: List[str] = []
    failedOperationIds: List[str] = []

@router.get("/{user_id}/restore")
async def restore_user_data(user_id: str, db: Session = Depends(get_db)):
    """Return a complete account snapshot for rebuilding a local database."""
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    units = db.query(models.Unit).filter(models.Unit.owner_id == user.id).all()
    progress = db.query(models.UserSyllabusProgress).filter(
        models.UserSyllabusProgress.user_id == user.id
    ).all()
    quizzes = db.query(models.QuizHistory).filter(
        models.QuizHistory.owner_id == user.id
    ).order_by(models.QuizHistory.id.desc()).all()
    timetables = db.query(models.Timetable).filter(
        models.Timetable.owner_id == user.id
    ).order_by(models.Timetable.timestamp.desc()).all()

    return {
        "user": {
            "id": user.id,
            "username": user.username,
            "email": user.email,
            "role": user.role,
            "semester_status": user.semester_status,
            "difficulty": user.difficulty,
            "ai_persona": user.ai_persona,
        },
        "units": [
            {
                "id": unit.id,
                "name": unit.name,
                "is_active": unit.is_active,
                "category": unit.category,
                "course": unit.course,
                "unit_group": unit.unit_group,
                "modules": [
                    {
                        "id": module.id,
                        "name": module.name,
                        "topics": [
                            {
                                "id": topic.id,
                                "name": topic.name,
                                "subtopics": [
                                    {
                                        "id": subtopic.id,
                                        "name": subtopic.name,
                                        "learning_objectives": [
                                            {"id": objective.id, "description": objective.description}
                                            for objective in subtopic.learning_objectives
                                        ],
                                    }
                                    for subtopic in topic.subtopics
                                ],
                            }
                            for topic in module.topics
                        ],
                    }
                    for module in unit.modules
                ],
            }
            for unit in units
        ],
        "progress": [
            {
                "node_id": item.node_id,
                "node_type": item.node_type,
                "status": item.status,
                "last_studied_at": item.last_studied_at,
            }
            for item in progress
        ],
        "quiz_history": [
            {
                "id": item.id,
                "unit_name": item.unit_name,
                "score": item.score,
                "total": item.total,
                "pnl": item.pnl,
                "timestamp": item.timestamp,
                "quiz_id": item.quiz_id,
            }
            for item in quizzes
        ],
        "timetables": [
            {
                "weekly_plan": item.weekly_plan_json,
                "ai_brief": item.ai_brief,
                "timestamp": item.timestamp,
            }
            for item in timetables
        ],
    }

def find_user(user_id_or_name: str, db: Session):
    user = None
    if str(user_id_or_name).isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    if not user:
        user = db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()
    return user

@router.post("", response_model=SyncResponseSchema)
@router.post("/", response_model=SyncResponseSchema)
async def sync_operations(request: SyncRequestSchema, db: Session = Depends(get_db)):
    applied_ids = []
    failed_ids = []
    user = find_user(request.userId, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    for op in request.operations:
        try:
            if op.entityType == "subtopic_progress":
                subtopic = db.query(models.Subtopic).filter(models.Subtopic.id == op.entityId).first()
                if subtopic:
                    is_completed = op.payload.get("is_completed", op.payload.get("isCompleted", True))
                    if user:
                        progress = db.query(models.UserSyllabusProgress).filter(
                            models.UserSyllabusProgress.user_id == user.id,
                            models.UserSyllabusProgress.node_id == subtopic.id,
                            models.UserSyllabusProgress.node_type == "subtopic"
                        ).first()

                        if not progress:
                            progress = models.UserSyllabusProgress(
                                user_id=user.id,
                                node_id=subtopic.id,
                                node_type="subtopic",
                                status="Completed" if is_completed else "In_Progress",
                                last_studied_at=time.time()
                            )
                            db.add(progress)
                        else:
                            progress.status = "Completed" if is_completed else "In_Progress"
                            progress.last_studied_at = time.time()

                        if is_completed:
                            for obj in subtopic.learning_objectives:
                                obj_progress = db.query(models.UserSyllabusProgress).filter(
                                    models.UserSyllabusProgress.user_id == user.id,
                                    models.UserSyllabusProgress.node_id == obj.id,
                                    models.UserSyllabusProgress.node_type == "objective"
                                ).first()
                                if obj_progress:
                                    obj_progress.status = "Completed"
                                    obj_progress.last_studied_at = time.time()
                                else:
                                    db.add(models.UserSyllabusProgress(
                                        user_id=user.id,
                                        node_id=obj.id,
                                        node_type="objective",
                                        status="Completed",
                                        last_studied_at=time.time()
                                    ))

                if subtopic:
                    applied_ids.append(op.operationId)
                else:
                    failed_ids.append(op.operationId)
            elif op.entityType == "objective_progress" and user:
                objective = db.query(models.LearningObjective).filter(
                    models.LearningObjective.id == op.entityId
                ).first()
                if objective:
                    is_completed = bool(op.payload.get("is_completed", True))
                    progress = db.query(models.UserSyllabusProgress).filter(
                        models.UserSyllabusProgress.user_id == user.id,
                        models.UserSyllabusProgress.node_id == objective.id,
                        models.UserSyllabusProgress.node_type == "objective",
                    ).first()
                    if not progress:
                        progress = models.UserSyllabusProgress(
                            user_id=user.id,
                            node_id=objective.id,
                            node_type="objective",
                        )
                        db.add(progress)
                    progress.status = "Completed" if is_completed else "In_Progress"
                    progress.last_studied_at = time.time()
                if objective:
                    applied_ids.append(op.operationId)
                else:
                    failed_ids.append(op.operationId)
            else:
                failed_ids.append(op.operationId)

        except Exception as e:
            logger.error(f"Error processing sync operation {op.operationId}: {e}")
            failed_ids.append(op.operationId)

    db.commit()
    return SyncResponseSchema(appliedOperationIds=applied_ids, failedOperationIds=failed_ids)
