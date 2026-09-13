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

    session = None
    if request.session_id is not None:
        session = db.query(models.ChatSession).filter(
            models.ChatSession.id == request.session_id,
            models.ChatSession.owner_id == user.id,
        ).first()
        if not session:
            raise HTTPException(status_code=404, detail="Chat session not found")
    elif request.client_session_id:
        session = db.query(models.ChatSession).filter(
            models.ChatSession.owner_id == user.id,
            models.ChatSession.client_session_id == request.client_session_id,
        ).first()
    if session is None:
        session = models.ChatSession(
            owner_id=user.id,
            title=request.prompt[:80] or "New Consultation",
            timestamp=time.time(),
            client_session_id=request.client_session_id,
            transcript_json=[],
        )
        db.add(session)
        db.flush()

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

    transcript = list(session.transcript_json or [])
    transcript.extend([
        {"role": "user", "content": request.prompt, "timestamp": time.time()},
        {"role": "model", "content": response_text, "timestamp": time.time()},
    ])
    session.transcript_json = transcript
    session.timestamp = time.time()
    db.commit()

    return schemas.ChatResponse(response=response_text, session_id=session.id)

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
    quiz = models.Quiz(
        title=quiz_data["quiz_title"],
        unit_name=request.unit_name,
        topic=topic,
        questions_json=quiz_data["questions"],
        owner_id=user.id,
    )
    db.add(quiz)
    db.commit()
    db.refresh(quiz)
    return {**quiz_data, "quiz_id": quiz.id}


@router.post("/quiz/record")
async def record_quiz(request: schemas.QuizRecordRequest, db: Session = Depends(get_db)):
    user = find_user(str(request.user_id), db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    quiz = db.query(models.Quiz).filter(
        models.Quiz.id == request.quiz_id,
        models.Quiz.owner_id == user.id,
    ).first() if request.quiz_id else db.query(models.Quiz).filter(
        models.Quiz.owner_id == user.id,
        models.Quiz.unit_name == request.unit_name,
    ).order_by(models.Quiz.created_at.desc()).first()
    if quiz is None:
        quiz = models.Quiz(
            title=request.quiz_title or request.unit_name,
            unit_name=request.unit_name,
            topic=request.topic,
            questions_json=[question.model_dump() for question in request.questions],
            owner_id=user.id,
        )
        db.add(quiz)
        db.flush()

    total = max(request.total, 0)
    score = max(min(request.score, total), 0)
    history = models.QuizHistory(
        unit_name=request.unit_name,
        score=score,
        total=total,
        pnl=(score / total * 100) if total else 0.0,
        timestamp=str(request.timestamp),
        quiz_id=quiz.id,
        correct_answers=score,
        owner_id=user.id,
    )
    db.add(history)
    db.commit()
    return {"status": "recorded", "quiz_id": str(quiz.id), "history_id": str(history.id)}

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
