from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import Optional
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.services.ai_service import ai_service
import time

router = APIRouter(prefix="/learning", tags=["Learning Trace"])

def resolve_user(user_id: str, db: Session):
    return db.query(models.User).filter(
        (models.User.id == (int(user_id) if user_id.isdigit() else -1))
        | (models.User.username == user_id)
    ).first()

def unit_progress_snapshot(unit: models.Unit, user: models.User, db: Session):
    subtopics = [
        subtopic
        for module in unit.modules
        for topic in module.topics
        for subtopic in topic.subtopics
    ]
    node_ids = [subtopic.id for subtopic in subtopics]
    completed = {
        progress.node_id
        for progress in db.query(models.UserSyllabusProgress).filter(
            models.UserSyllabusProgress.user_id == user.id,
            models.UserSyllabusProgress.node_type == "subtopic",
            models.UserSyllabusProgress.node_id.in_(node_ids or [-1]),
            models.UserSyllabusProgress.status == "Completed",
        ).all()
    }
    completed_count = len(completed)
    return {
        "unit_id": unit.id,
        "unit_name": unit.name,
        "completed_subtopics": completed_count,
        "total_subtopics": len(subtopics),
        "percentage": round((completed_count / len(subtopics) * 100) if subtopics else 0.0, 2),
    }

@router.get("/progress/unit/{unit_id}")
def get_unit_progress(unit_id: int, user_id: str, db: Session = Depends(get_db)):
    user = resolve_user(user_id, db)
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if not user or not unit:
        raise HTTPException(status_code=404, detail="User or unit not found")
    return unit_progress_snapshot(unit, user, db)

@router.post("/content")
def save_learning_content(request: schemas.LearningContentCreate, db: Session = Depends(get_db)):
    user = resolve_user(request.user_id, db)
    objective = db.query(models.LearningObjective).filter(
        models.LearningObjective.id == request.objective_id
    ).first()
    if not user or not objective:
        raise HTTPException(status_code=404, detail="User or learning objective not found")
    saved = models.LearningContent(
        objective_id=objective.id,
        user_id=user.id,
        content=request.content,
    )
    db.add(saved)
    db.commit()
    db.refresh(saved)
    return {"id": saved.id, "objective_id": saved.objective_id, "content": saved.content}

@router.get("/session/{subtopic_id}")
async def get_learning_session(subtopic_id: int, user_id: str, db: Session = Depends(get_db), student_message: Optional[str] = Query(None)):
    # 1. Find the subtopic and its objectives
    subtopic = db.query(models.Subtopic).filter(models.Subtopic.id == subtopic_id).first()
    if not subtopic:
        raise HTTPException(status_code=404, detail="Subtopic not found")

    objectives = subtopic.learning_objectives
    if not objectives:
        raise HTTPException(status_code=400, detail="No learning objectives for this subtopic")

    # 2. Find where the user is
    # Using the user_id (username or ID)
    user = resolve_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    current_progress = db.query(models.UserSyllabusProgress).filter(
        models.UserSyllabusProgress.user_id == user.id,
        models.UserSyllabusProgress.node_type == "objective",
        models.UserSyllabusProgress.status == "In_Progress",
        models.UserSyllabusProgress.node_id.in_([objective.id for objective in objectives])
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
    content = await ai_service.generate_learning_content(
        current_objective.description,
        user.username,
        student_message=student_message,
        session_context=f"Subtopic: {subtopic.name}."
    )

    return {
        "subtopic_name": subtopic.name,
        "objective_id": current_objective.id,
        "objective_description": current_objective.description,
        "content": content,
        "is_last": current_objective.id == objectives[-1].id,
        "is_first": current_objective.id == objectives[0].id
    }

@router.post("/next/{subtopic_id}")
async def next_objective(subtopic_id: int, user_id: str, db: Session = Depends(get_db), student_message: Optional[str] = Query(None)):
    user = resolve_user(user_id, db)
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
        models.UserSyllabusProgress.status == "In_Progress",
        models.UserSyllabusProgress.node_id.in_([objective.id for objective in objectives])
    ).first()

    if not current_progress:
        # Check if the subtopic is already completed or if all objectives are completed for this user
        completed_progress = db.query(models.UserSyllabusProgress).filter(
            models.UserSyllabusProgress.user_id == user.id,
            models.UserSyllabusProgress.node_type == "objective",
            models.UserSyllabusProgress.status == "Completed",
            models.UserSyllabusProgress.node_id.in_([o.id for o in objectives])
        ).all()
        completed_ids = {p.node_id for p in completed_progress}
        if completed_ids and all(o.id in completed_ids for o in objectives):
            return {"status": "subtopic_completed", "trigger_quiz": True}

        return await get_learning_session(subtopic_id, user_id, db, student_message=student_message)

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
        return await get_learning_session(subtopic_id, user_id, db, student_message=student_message)
    else:
        # End of subtopic - Mark subtopic completed and return subtopic_completed
        subtopic_progress = db.query(models.UserSyllabusProgress).filter(
            models.UserSyllabusProgress.user_id == user.id,
            models.UserSyllabusProgress.node_id == subtopic.id,
            models.UserSyllabusProgress.node_type == "subtopic"
        ).first()

        if not subtopic_progress:
            subtopic_progress = models.UserSyllabusProgress(
                user_id=user.id,
                node_id=subtopic.id,
                node_type="subtopic",
                status="Completed",
                last_studied_at=time.time()
            )
            db.add(subtopic_progress)
        else:
            subtopic_progress.status = "Completed"
            subtopic_progress.last_studied_at = time.time()

        db.commit()
        return {"status": "subtopic_completed", "trigger_quiz": True}


@router.post("/previous/{subtopic_id}")
async def previous_objective(subtopic_id: int, user_id: str, db: Session = Depends(get_db)):
    user = resolve_user(user_id, db)
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
        models.UserSyllabusProgress.status == "In_Progress",
        models.UserSyllabusProgress.node_id.in_([objective.id for objective in objectives])
    ).first()

    if not current_progress:
        return await get_learning_session(subtopic_id, user_id, db)

    current_index = next((index for index, obj in enumerate(objectives) if obj.id == current_progress.node_id), -1)
    if current_index <= 0:
        return await get_learning_session(subtopic_id, user_id, db)

    previous_obj = objectives[current_index - 1]
    current_progress.node_id = previous_obj.id
    current_progress.last_studied_at = time.time()
    db.commit()
    return await get_learning_session(subtopic_id, user_id, db)
