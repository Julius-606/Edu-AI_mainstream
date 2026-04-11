# IDENTITY: backend/main.py
# VERSION: 1.2.2
# ⚙️ GEAR 2: The API Routes (Executing the Trades)

import os
from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
import logging

try:
    from database import Base, engine, get_db
    import models as models
    import schemas as schemas
    from ai_engine import ai_engine
except ImportError:
    from .database import Base, engine, get_db
    from . import models as models
    from . import schemas as schemas
    from .ai_engine import ai_engine

# Create database tables if they don't exist
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Edu_AI Prop Firm Backend", version="1.2.2")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {"status": "Bullish 📈", "message": "Edu_AI Backend v1.2.2 is Online."}

@app.get("/api/user/{user_id}/dashboard", response_model=schemas.DashboardResponse)
def get_dashboard(user_id: int, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        # Auto-create user for testing if not exists
        user = models.User(
            id=user_id,
            username=f"Trader_{user_id}",
            role="Student",
            sensory_mode="Standard",
            ai_persona="Socratic Mentor",
            semester_status="Year 4 - Redemption Arc"
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    active_units = db.query(models.Unit).filter(
        models.Unit.owner_id == user_id, 
        models.Unit.is_active == True
    ).all()

    if not active_units:
        # Add default units
        defaults = ["Biochemistry II", "General Surgery", "Internal Medicine"]
        for name in defaults:
            db.add(models.Unit(name=name, owner_id=user_id))
        db.commit()
        active_units = db.query(models.Unit).filter(models.Unit.owner_id == user_id).all()

    unit_names = [u.name for u in active_units]
    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user_id).all()
    total_quizzes = len(quizzes)
    average_pnl = sum([q.pnl for q in quizzes]) / total_quizzes if total_quizzes > 0 else 0.0

    return schemas.DashboardResponse(
        username=user.username,
        role=user.role,
        sensory_mode=user.sensory_mode,
        semester_status=user.semester_status,
        difficulty=user.difficulty,
        ai_persona=user.ai_persona,
        active_units=unit_names,
        average_pnl=round(average_pnl, 2),
        total_quizzes=total_quizzes
    )

# --- AI ENDPOINTS ---

@app.post("/api/ai/chat", response_model=schemas.ChatResponse)
def ai_chat(request: schemas.ChatRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == request.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    system_instruction = f"You are {user.ai_persona} (an AI Study Companion). " \
                         f"The student is at level: {user.semester_status}. " \
                         f"Be encouraging, concise, and educational. Recommend a YouTube link ONLY if it directly helps explain a complex concept."
    
    # Pre-pend history if available
    history_text = ""
    for msg in request.history:
        history_text += f"{msg.role}: {msg.content}\n"

    full_prompt = f"{history_text}User: {request.prompt}"

    response_text = ai_engine.ask(prompt=full_prompt, system_instruction=system_instruction)
    if not response_text:
        raise HTTPException(status_code=500, detail="Both AI engines are currently unavailable.")
    
    return schemas.ChatResponse(response=response_text)

@app.post("/api/ai/quiz", response_model=schemas.QuizResponse)
def generate_quiz(request: schemas.QuizRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == request.user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    quiz_data = ai_engine.generate_quiz(
        unit_name=request.unit_name,
        student_level=user.semester_status
    )
    
    if not quiz_data:
        raise HTTPException(status_code=500, detail="Failed to ignite the Quiz Engine.")
    
    return quiz_data

@app.post("/api/quiz/record")
def record_quiz(history: schemas.QuizRecordRequest, db: Session = Depends(get_db)):
    new_record = models.QuizHistory(
        unit_name=history.unit_name,
        score=history.score,
        total=history.total,
        pnl=(history.score / history.total) * 100 if history.total > 0 else 0,
        owner_id=history.user_id,
        timestamp=str(history.timestamp)
    )
    db.add(new_record)
    db.commit()
    return {"status": "Success", "message": "Trade recorded."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
