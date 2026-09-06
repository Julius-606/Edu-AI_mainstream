from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import Optional
import time
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.services.ai_service import MARKDOWN_FORMAT_INSTRUCTION, ai_service

router = APIRouter(prefix="/ai", tags=["AI Intelligence"])

def find_user(user_id_or_name: str, db: Session):
    if str(user_id_or_name).isdigit():
        return db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    return db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()

@router.get("/health")
async def ai_health():
    # Simplified health check focusing on internal state
    return {
        "status": "online",
        "active_models": ai_service.model_variants,
        "current_key_index": ai_service.key_index,
        "total_keys_loaded": len(ai_service.clients)
    }

@router.post("/chat", response_model=schemas.ChatResponse)
async def ai_chat(request: schemas.ChatRequest, db: Session = Depends(get_db)):
    user = find_user(str(request.user_id), db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    user_msg = models.ChatMessage(role="user", content=request.prompt, owner_id=user.id)
    db.add(user_msg)
    db.commit()

    system_instruction = (
        f"You are {user.ai_persona}. Level: {user.semester_status}. "
        f"{MARKDOWN_FORMAT_INSTRUCTION}"
    )

    history_text = ""
    for msg in request.history:
        history_text += f"{msg.role}: {msg.content}\n"

    full_prompt = f"{history_text}User: {request.prompt}"

    # AWAIT the async service call
    response_text = await ai_service.ask(prompt=full_prompt, system_instruction=system_instruction)

    if not response_text:
        raise HTTPException(status_code=500, detail="AI engine is currently unavailable.")

    ai_msg = models.ChatMessage(role="model", content=response_text, owner_id=user.id)
    db.add(ai_msg)
    db.commit()

    return schemas.ChatResponse(response=response_text)

@router.post("/quiz", response_model=schemas.QuizResponse)
async def generate_quiz(
    request: schemas.QuizRequest,
    topic: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    user = find_user(str(request.user_id), db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # AWAIT the async service call
    quiz_data = await ai_service.generate_quiz(
        unit_name=request.unit_name,
        student_level=user.semester_status,
        topic=topic
    )
    if not quiz_data:
        raise HTTPException(status_code=500, detail="Failed to ignite the Quiz Engine.")
    return quiz_data

@router.get("/recommendations/{user_id}", response_model=schemas.RecommendationResponse)
async def get_recommendations(user_id: str, db: Session = Depends(get_db)):
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

    # AWAIT the async service call
    rec_text = await ai_service.get_recommendations(user_info, quiz_history, active_units)
    return schemas.RecommendationResponse(recommendation=rec_text or "Keep going!")
