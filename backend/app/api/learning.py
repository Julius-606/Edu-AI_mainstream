from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.services.ai_service import ai_service
import time

router = APIRouter(prefix="/learning", tags=["Learning Trace"])

@router.get("/session/{subtopic_id}")
async def get_learning_session(subtopic_id: int, user_id: str, db: Session = Depends(get_db)):
    # 1. Find the subtopic and its objectives
    subtopic = db.query(models.Subtopic).filter(models.Subtopic.id == subtopic_id).first()
    if not subtopic:
        raise HTTPException(status_code=404, detail="Subtopic not found")

    objectives = subtopic.learning_objectives
    if not objectives:
        raise HTTPException(status_code=400, detail="No learning objectives for this subtopic")

    # 2. Find where the user is
    # Using the user_id (username or ID)
    user = db.query(models.User).filter((models.User.id == (int(user_id) if user_id.isdigit() else -1)) | (models.User.username == user_id)).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    current_progress = db.query(models.UserSyllabusProgress).filter(
        models.UserSyllabusProgress.user_id == user.id,
        models.UserSyllabusProgress.node_type == "objective",
        models.UserSyllabusProgress.status == "In_Progress"
    ).first()

    current_objective = None
    if current_progress:
        current_objective = db.query(models.LearningObjective).filter(models.LearningObjective.id == current_progress.node_id).first()

    if not current_objective:
        current_objective = objectives[0]
        # Initialize progress
        new_progress = models.UserSyllabusProgress(
            user_id=user.id,
            node_id=current_objective.id,
            node_type="objective",
            status="In_Progress",
            last_studied_at=time.time()
        )
        db.add(new_progress)
        db.commit()

    # 3. Generate AI content for this objective
    content = await ai_service.generate_learning_content(current_objective.description, user.username)

    return {
        "subtopic_name": subtopic.name,
        "objective_id": current_objective.id,
        "objective_description": current_objective.description,
        "content": content,
        "is_last": current_objective.id == objectives[-1].id,
        "is_first": current_objective.id == objectives[0].id
    }

@router.post("/next/{subtopic_id}")
async def next_objective(subtopic_id: int, user_id: str, db: Session = Depends(get_db)):
    user = db.query(models.User).filter((models.User.id == (int(user_id) if user_id.isdigit() else -1)) | (models.User.username == user_id)).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    subtopic = db.query(models.Subtopic).filter(models.Subtopic.id == subtopic_id).first()
    if not subtopic:
        raise HTTPException(status_code=404, detail="Subtopic not found")

    objectives = subtopic.learning_objectives
    if not objectives:
        return {"status": "subtopic_completed", "trigger_quiz": True}

    current_progress = db.query(models.UserSyllabusProgress).filter(
        models.UserSyllabusProgress.user_id == user.id,
        models.UserSyllabusProgress.node_type == "objective",
        models.UserSyllabusProgress.status == "In_Progress"
    ).first()

    if not current_progress:
        return await get_learning_session(subtopic_id, user_id, db)

    # Mark current as completed
    current_progress.status = "Completed"

    # Find next
    current_index = -1
    for i, obj in enumerate(objectives):
        if obj.id == current_progress.node_id:
            current_index = i
            break

    if current_index < len(objectives) - 1:
        next_obj = objectives[current_index + 1]
        new_progress = models.UserSyllabusProgress(
            user_id=user.id,
            node_id=next_obj.id,
            node_type="objective",
            status="In_Progress",
            last_studied_at=time.time()
        )
        db.add(new_progress)
        db.commit()
        return await get_learning_session(subtopic_id, user_id, db)
    else:
        # End of subtopic - Trigger Quiz
        db.commit()
        return {"status": "subtopic_completed", "trigger_quiz": True}
