from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import time
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.services.ai_service import ai_service
from app.api.learning import unit_progress_snapshot
# from app.core.security import get_current_user # To be implemented in core

router = APIRouter(prefix="/users", tags=["User Management"])

def find_user(user_id_or_name: str, db: Session):
    user = None
    if str(user_id_or_name).isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    if not user:
        user = db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()
    return user

def user_response(user: models.User) -> schemas.UserResponseSchema:
    # Manual mapping to exclude sensory_mode if it exists in the model but not in the schema
    return schemas.UserResponseSchema(
        id=user.id,
        username=user.username,
        email=user.email,
        role=user.role,
        difficulty=user.difficulty,
        ai_persona=user.ai_persona,
        semester_status=user.semester_status,
        interests=user.interests,
        active_units=user.active_units_list
    )

@router.get("/{user_id}", response_model=schemas.UserResponseSchema)
def get_user(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        user = models.User(username=user_id, role="Student")
        db.add(user)
        db.commit()
        db.refresh(user)

    return user_response(user)

@router.get("/{user_id}/dashboard", response_model=schemas.DashboardResponse)
def get_dashboard(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        user = models.User(username=user_id, role="Student")
        db.add(user)
        db.commit()
        db.refresh(user)

    active_units = db.query(models.Unit).filter(models.Unit.owner_id == user.id, models.Unit.is_active == True).all()
    unit_names = [u.name for u in active_units]

    # Trace logic: Find the last learning progress
    last_progress = db.query(models.UserSyllabusProgress).filter(
        models.UserSyllabusProgress.user_id == user.id,
        models.UserSyllabusProgress.status == "In_Progress"
    ).order_by(models.UserSyllabusProgress.last_studied_at.desc()).first()

    last_point = "Start your journey"
    if last_progress:
        # Resolve node name based on type
        if last_progress.node_type == "objective":
            obj = db.query(models.LearningObjective).filter(models.LearningObjective.id == last_progress.node_id).first()
            if obj: last_point = f"Resume: {obj.description[:50]}..."

    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    total_quizzes = len(quizzes)
    average_pnl = sum([q.pnl for q in quizzes]) / total_quizzes if total_quizzes > 0 else 0.0
    chat_sessions = db.query(models.ChatSession).filter(
        models.ChatSession.owner_id == user.id
    ).order_by(models.ChatSession.timestamp.asc()).all()
    chat_history = [
        schemas.ChatMessageResponse(
            role=message.get("role", ""),
            content=message.get("content", ""),
            timestamp=str(message.get("timestamp", "")),
        )
        for session in chat_sessions
        for message in (session.transcript_json or [])
    ]

    return schemas.DashboardResponse(
        username=user.username,
        email=user.email,
        role=user.role,
        semester_status=user.semester_status,
        difficulty=user.difficulty,
        ai_persona=user.ai_persona,
        active_units=unit_names,
        units=active_units,
        average_pnl=round(average_pnl, 2),
        total_quizzes=total_quizzes,
        quiz_history=[schemas.QuizHistoryResponse(unit_name=q.unit_name, pnl=q.pnl, timestamp=q.timestamp) for q in quizzes],
        chat_history=chat_history,
        last_point=last_point,
        unit_progress=[unit_progress_snapshot(unit, user, db) for unit in active_units],
    )

@router.get("/{user_id}/timetable", response_model=schemas.TimetableResponse)
def get_ai_timetable(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    one_week_ago = time.time() - (7 * 24 * 60 * 60)
    existing_timetable = db.query(models.Timetable).filter(models.Timetable.owner_id == user.id, models.Timetable.timestamp > one_week_ago).order_by(models.Timetable.timestamp.desc()).first()

    if existing_timetable:
        return schemas.TimetableResponse(weekly_plan=existing_timetable.weekly_plan_json, ai_brief=existing_timetable.ai_brief)

    last_timetable = db.query(models.Timetable).filter(models.Timetable.owner_id == user.id).order_by(models.Timetable.timestamp.desc()).first()
    previous_plan = last_timetable.weekly_plan_json if last_timetable else None
    quiz_history = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    active_units = [u.name for u in user.units if u.is_active]
    recent_sessions = db.query(models.ChatSession).filter(models.ChatSession.owner_id == user.id).order_by(models.ChatSession.id.desc()).limit(10).all()
    chat_titles = [s.title for s in recent_sessions]

    user_info = {"username": user.username, "semester_status": user.semester_status}
    new_timetable_data = ai_service.generate_timetable(user_info, quiz_history, active_units, chat_titles, previous_plan)

    if not new_timetable_data:
        raise HTTPException(status_code=500, detail="The AI is still drafting your plan. Try again in a moment.")

    new_db_timetable = models.Timetable(owner_id=user.id, weekly_plan_json=new_timetable_data["weekly_plan"], ai_brief=new_timetable_data["ai_brief"], timestamp=time.time())
    db.add(new_db_timetable)
    db.commit()

    return schemas.TimetableResponse(weekly_plan=new_timetable_data["weekly_plan"], ai_brief=new_timetable_data["ai_brief"])

@router.put("/{user_id}", response_model=schemas.UserResponseSchema)
def update_user(user_id: str, user_update: schemas.UserUpdate, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    update_data = user_update.dict(exclude_unset=True)
    for field in ("username", "email"):
        if field in update_data:
            value = update_data[field].strip() if update_data[field] else None
            if field == "username" and not value:
                raise HTTPException(status_code=422, detail="Username cannot be empty")
            conflict = db.query(models.User).filter(
                getattr(models.User, field) == value,
                models.User.id != user.id,
            ).first()
            if conflict:
                raise HTTPException(status_code=409, detail=f"{field.title()} is already in use")
            update_data[field] = value

    if "active_units" in update_data:
        new_unit_names = update_data.pop("active_units")
        db.query(models.Unit).filter(models.Unit.owner_id == user.id).update({"is_active": False})
        for name in new_unit_names:
            existing_unit = db.query(models.Unit).filter(models.Unit.owner_id == user.id, models.Unit.name == name).first()
            if existing_unit:
                existing_unit.is_active = True
            else:
                db.add(models.Unit(name=name, owner_id=user.id, is_active=True))

    for key, value in update_data.items():
        setattr(user, key, value)

    db.commit()
    db.refresh(user)

    return user_response(user)


@router.delete("/{user_id}/units/{unit_id}")
def delete_user_unit(user_id: str, unit_id: int, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    unit = db.query(models.Unit).filter(
        models.Unit.id == unit_id,
        models.Unit.owner_id == (user.id if user else -1),
    ).first()
    if not user or not unit:
        raise HTTPException(status_code=404, detail="User unit not found")
    db.delete(unit)
    db.commit()
    return {"status": "deleted", "unit_id": unit_id}


@router.get("/{user_id}/connect/messages", response_model=List[schemas.ConnectionMessageResponse])
def get_connection_messages(user_id: str, with_user_id: int, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return db.query(models.ConnectionMessage).filter(
        ((models.ConnectionMessage.sender_id == user.id) & (models.ConnectionMessage.recipient_id == with_user_id))
        | ((models.ConnectionMessage.sender_id == with_user_id) & (models.ConnectionMessage.recipient_id == user.id))
    ).order_by(models.ConnectionMessage.created_at.asc()).all()


@router.post("/{user_id}/connect/messages", response_model=schemas.ConnectionMessageResponse)
def send_connection_message(
    user_id: str,
    message: schemas.ConnectionMessageCreate,
    db: Session = Depends(get_db),
):
    user = find_user(user_id, db)
    recipient = db.query(models.User).filter(models.User.id == message.recipient_id).first()
    if not user or not recipient:
        raise HTTPException(status_code=404, detail="User or recipient not found")
    if not message.content.strip():
        raise HTTPException(status_code=422, detail="Message cannot be empty")
    saved = models.ConnectionMessage(
        sender_id=user.id,
        recipient_id=recipient.id,
        content=message.content.strip(),
    )
    db.add(saved)
    db.commit()
    db.refresh(saved)
    return saved
