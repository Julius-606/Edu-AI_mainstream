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
    user = find_user(request.userId, db)

    for op in request.operations:
        try:
            if op.entityType == "subtopic_progress":
                subtopic = db.query(models.Subtopic).filter(models.Subtopic.id == op.entityId).first()
                if subtopic:
                    is_completed = op.payload.get("is_completed", op.payload.get("isCompleted", True))
                    subtopic.is_completed = bool(is_completed)

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
                                obj.is_completed = True
                                obj_progress = db.query(models.UserSyllabusProgress).filter(
                                    models.UserSyllabusProgress.user_id == user.id,
                                    models.UserSyllabusProgress.node_id == obj.id,
                                    models.UserSyllabusProgress.node_type == "objective"
                                ).first()
                                if obj_progress:
                                    obj_progress.status = "Completed"
                                else:
                                    db.add(models.UserSyllabusProgress(
                                        user_id=user.id,
                                        node_id=obj.id,
                                        node_type="objective",
                                        status="Completed",
                                        last_studied_at=time.time()
                                    ))

                applied_ids.append(op.operationId)
            else:
                applied_ids.append(op.operationId)

        except Exception as e:
            logger.error(f"Error processing sync operation {op.operationId}: {e}")

    db.commit()
    return SyncResponseSchema(appliedOperationIds=applied_ids)
