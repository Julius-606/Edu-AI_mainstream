
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import Optional
import time
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.services.ai_service import ai_service

router = APIRouter(prefix="/ai", tags=["AI Intelligence"])

def find_user(user_id_or_name: str, db: Session):
    if str(user_id_or_name).isdigit():
        return db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    return db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()

@router.post("/chat", response_model=schemas.ChatResponse)
def ai_chat(request: schemas.ChatRequest, db: Session = Depends(get_db)):
    user = find_user(str(request.user_id), db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    user_msg = models.ChatMessage(role="user", content=request.prompt, owner_id=user.id)
    db.add(user_msg)
    db.commit()

    system_instruction = f"You are {user.ai_persona}. Level: {user.semester_status}."

    history_text = ""
    for msg in request.history:
        history_text += f"{msg.role}: {msg.content}\n"

    full_prompt = f"{history_text}User: {request.prompt}"
    response_text = ai_service.ask(prompt=full_prompt, system_instruction=system_instruction)

    if not response_text:
        raise HTTPException(status_code=500, detail="AI engine is currently unavailable.")

    ai_msg = models.ChatMessage(role="model", content=response_text, owner_id=user.id)
    db.add(ai_msg)
    db.commit()

    return schemas.ChatResponse(response=response_text)

@router.post("/quiz", response_model=schemas.QuizResponse)
def generate_quiz(
    request: schemas.QuizRequest,
    topic: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    user = find_user(str(request.user_id), db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    quiz_data = ai_service.generate_quiz(
        unit_name=request.unit_name,
        student_level=user.semester_status,
        topic=topic
    )
    if not quiz_data:
        raise HTTPException(status_code=500, detail="Failed to ignite the Quiz Engine.")
    return quiz_data

@router.get("/recommendations/{user_id}", response_model=schemas.RecommendationResponse)
def get_recommendations(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    quiz_history = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    active_units = [u.name for u in user.units if u.is_active]

    user_info = {
        "username": user.username,
        "ai_persona": user.ai_persona,
        "semester_status": user.semester_status
    }

    # Extract database syllabus progress
    progress_records = db.query(models.UserSyllabusProgress).filter(models.UserSyllabusProgress.user_id == user.id).all()
    completed_nodes = [p.node_id for p in progress_records if p.status == "Completed"]
    
    # Calculate weak and mastered topics from quiz history
    weak = [q.unit_name for q in quiz_history if q.pnl < 70]
    mastered = [q.unit_name for q in quiz_history if q.pnl >= 80]
    
    db_context = schemas.StudyContextPayload(
        mastered_topics=list(set(mastered)),
        weak_topics=list(set(weak)),
        total_quizzes_taken=len(quiz_history),
        average_quiz_score=round(sum([q.pnl for q in quiz_history]) / len(quiz_history), 1) if quiz_history else None
    )

    rec_text = ai_service.get_recommendations(user_info, quiz_history, active_units, study_context=db_context)
    return schemas.RecommendationResponse(recommendation=rec_text or "Keep going!")

@router.post("/recommendations/{user_id}", response_model=schemas.RecommendationResponse)
def get_personalized_recommendations(
    user_id: str,
    payload: Optional[schemas.StudyContextPayload] = None,
    db: Session = Depends(get_db)
):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    quiz_history = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    active_units = [u.name for u in user.units if u.is_active]

    user_info = {
        "username": user.username,
        "ai_persona": user.ai_persona,
        "semester_status": user.semester_status
    }

    rec_text = ai_service.get_recommendations(user_info, quiz_history, active_units, study_context=payload)
    return schemas.RecommendationResponse(recommendation=rec_text or "Keep pushing your boundaries!")


 