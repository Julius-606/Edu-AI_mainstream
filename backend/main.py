# IDENTITY: backend/main.py
# VERSION: 1.8.0
# ⚙️ GEAR 2: The API Routes (Executing the Trades)

import os
from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List, Optional
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
# Note: On production, you might want to use Alembic migrations
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Edu_AI Prop Firm Backend", version="1.8.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {"status": "Bullish 📈", "message": "Edu_AI Backend v1.8.0 is Online."}

# --- USER MANAGEMENT ENDPOINTS ---

@app.post("/api/users", response_model=schemas.UserResponseSchema, tags=["User Management"])
def create_user(user_data: schemas.UserCreate, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.username == user_data.username).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Username already registered")

    new_user = models.User(
        username=user_data.username,
        role=user_data.role,
        sensory_mode=user_data.sensory_mode,
        difficulty=user_data.difficulty,
        ai_persona=user_data.ai_persona,
        semester_status=user_data.semester_status,
        interests=user_data.interests
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    # Handle initial units if provided
    if user_data.active_units:
        for unit_name in user_data.active_units:
            db.add(models.Unit(name=unit_name, owner_id=new_user.id, is_active=True))
        db.commit()
        db.refresh(new_user)

    # Prepare response manually to include active_units
    response = schemas.UserResponseSchema.from_orm(new_user)
    response.active_units = new_user.active_units_list
    return response

@app.get("/api/users/{user_id}", response_model=schemas.UserResponseSchema, tags=["User Management"])
def get_user(user_id: str, db: Session = Depends(get_db)):
    # Try ID first, then username
    user = None
    if user_id.isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id)).first()

    if not user:
        user = db.query(models.User).filter(models.User.username == user_id).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    response = schemas.UserResponseSchema.from_orm(user)
    response.active_units = user.active_units_list
    return response

@app.put("/api/users/{user_id}", response_model=schemas.UserResponseSchema, tags=["User Management"])
def update_user(user_id: str, user_update: schemas.UserUpdate, db: Session = Depends(get_db)):
    user = None
    if user_id.isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id)).first()

    if not user:
        user = db.query(models.User).filter(models.User.username == user_id).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    update_data = user_update.dict(exclude_unset=True)

    # Handle units separately if provided
    if "active_units" in update_data:
        new_unit_names = update_data.pop("active_units")
        # Deactivate all current units first (or delete if preferred, but deactivating is safer)
        db.query(models.Unit).filter(models.Unit.owner_id == user.id).update({"is_active": False})

        for name in new_unit_names:
            # Check if unit already exists for this user
            existing_unit = db.query(models.Unit).filter(
                models.Unit.owner_id == user.id,
                models.Unit.name == name
            ).first()
            if existing_unit:
                existing_unit.is_active = True
            else:
                db.add(models.Unit(name=name, owner_id=user.id, is_active=True))

    for key, value in update_data.items():
        setattr(user, key, value)

    db.commit()
    db.refresh(user)

    response = schemas.UserResponseSchema.from_orm(user)
    response.active_units = user.active_units_list
    return response

@app.get("/api/users", response_model=List[schemas.UserResponseSchema], tags=["User Management"])
def list_users(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    users = db.query(models.User).offset(skip).limit(limit).all()
    results = []
    for u in users:
        resp = schemas.UserResponseSchema.from_orm(u)
        resp.active_units = u.active_units_list
        results.append(resp)
    return results

# --- UNIT MANAGEMENT ENDPOINTS ---

@app.post("/api/units", response_model=schemas.UnitResponse, tags=["Unit Management"])
def create_unit(unit: schemas.UnitCreate, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.id == unit.owner_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="User not found")

    new_unit = models.Unit(
        name=unit.name,
        is_active=unit.is_active,
        category=unit.category,
        owner_id=unit.owner_id
    )
    db.add(new_unit)
    db.commit()
    db.refresh(new_unit)
    return new_unit

@app.get("/api/units/{unit_id}", response_model=schemas.UnitResponse, tags=["Unit Management"])
def get_unit(unit_id: int, db: Session = Depends(get_db)):
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if not unit:
        raise HTTPException(status_code=404, detail="Unit not found")
    return unit

@app.put("/api/units/{unit_id}", response_model=schemas.UnitResponse, tags=["Unit Management"])
def update_unit(unit_id: int, unit_update: schemas.UnitUpdate, db: Session = Depends(get_db)):
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if not unit:
        raise HTTPException(status_code=404, detail="Unit not found")

    update_data = unit_update.dict(exclude_unset=True)
    for key, value in update_data.items():
        setattr(unit, key, value)

    db.commit()
    db.refresh(unit)
    return unit

@app.delete("/api/units/{unit_id}", tags=["Unit Management"])
def delete_unit(unit_id: int, db: Session = Depends(get_db)):
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if not unit:
        raise HTTPException(status_code=404, detail="Unit not found")

    db.delete(unit)
    db.commit()
    return {"status": "Success", "message": f"Unit {unit_id} deleted."}

@app.get("/api/users/{user_id}/units", response_model=List[schemas.UnitResponse], tags=["Unit Management"])
def get_user_units(user_id: str, db: Session = Depends(get_db)):
    user = None
    if user_id.isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id)).first()

    if not user:
        user = db.query(models.User).filter(models.User.username == user_id).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return db.query(models.Unit).filter(models.Unit.owner_id == user.id).all()

# --- DASHBOARD & ACTIVITY ---

@app.get("/api/user/{user_id}/dashboard", response_model=schemas.DashboardResponse)
def get_dashboard(user_id: str, db: Session = Depends(get_db)):
    # Try to find user by string ID (username)
    user = db.query(models.User).filter(models.User.username == user_id).first()

    if not user:
        # Auto-create user for testing if not exists using the provided username
        user = models.User(
            username=user_id,
            role="Student",
            sensory_mode="Standard",
            ai_persona="Socratic Tutor",
            semester_status="Brace for the clinicals"
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    active_units = db.query(models.Unit).filter(
        models.Unit.owner_id == user.id,
        models.Unit.is_active == True
    ).all()

    if not active_units:
        # Add default units
        defaults = ["Biochemistry II", "General Surgery", "Internal Medicine"]
        for name in defaults:
            db.add(models.Unit(name=name, owner_id=user.id))
        db.commit()
        active_units = db.query(models.Unit).filter(models.Unit.owner_id == user.id).all()

    unit_names = [u.name for u in active_units]
    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    total_quizzes = len(quizzes)
    average_pnl = sum([q.pnl for q in quizzes]) / total_quizzes if total_quizzes > 0 else 0.0

    chat_messages = db.query(models.ChatMessage).filter(models.ChatMessage.owner_id == user.id).order_by(models.ChatMessage.id.asc()).all()

    return schemas.DashboardResponse(
        username=user.username,
        role=user.role,
        sensory_mode=user.sensory_mode,
        semester_status=user.semester_status,
        difficulty=user.difficulty,
        ai_persona=user.ai_persona,
        active_units=unit_names,
        average_pnl=round(average_pnl, 2),
        total_quizzes=total_quizzes,
        quiz_history=[schemas.QuizHistoryResponse(unit_name=q.unit_name, pnl=q.pnl, timestamp=q.timestamp) for q in quizzes],
        chat_history=[schemas.ChatMessageResponse(role=c.role, content=c.content, timestamp=c.timestamp or "") for c in chat_messages]
    )

# --- AI ENDPOINTS ---

@app.post("/api/ai/chat", response_model=schemas.ChatResponse)
def ai_chat(request: schemas.ChatRequest, db: Session = Depends(get_db)):
    # User ID coming from app is the local integer ID or username
    user = db.query(models.User).filter(models.User.id == request.user_id).first()
    if not user:
         user = db.query(models.User).filter(models.User.username == str(request.user_id)).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Record user message
    user_msg = models.ChatMessage(role="user", content=request.prompt, owner_id=user.id)
    db.add(user_msg)
    db.commit()

    system_instruction = f"You are {user.ai_persona} (an AI Study Companion). " \
                         f"The student is at level: {user.semester_status}. " \
                         f"Be encouraging, concise, and educational. Recommend a YouTube link ONLY if it directly helps explain a complex concept."
    
    history_text = ""
    for msg in request.history:
        history_text += f"{msg.role}: {msg.content}\n"

    full_prompt = f"{history_text}User: {request.prompt}"

    response_text = ai_engine.ask(prompt=full_prompt, system_instruction=system_instruction)
    if not response_text:
        raise HTTPException(status_code=500, detail="Both AI engines are currently unavailable.")

    # Record AI message
    ai_msg = models.ChatMessage(role="model", content=response_text, owner_id=user.id)
    db.add(ai_msg)
    db.commit()

    return schemas.ChatResponse(response=response_text)

@app.post("/api/ai/quiz", response_model=schemas.QuizResponse)
def generate_quiz(
    request: schemas.QuizRequest,
    topic: Optional[str] = Query(None),
    db: Session = Depends(get_db)
):
    user = db.query(models.User).filter(models.User.id == request.user_id).first()
    if not user:
        user = db.query(models.User).filter(models.User.username == str(request.user_id)).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    quiz_data = ai_engine.generate_quiz(
        unit_name=request.unit_name,
        student_level=user.semester_status,
        topic=topic
    )
    
    if not quiz_data:
        raise HTTPException(status_code=500, detail="Failed to ignite the Quiz Engine.")
    
    return quiz_data

@app.post("/api/quiz/record")
def record_quiz(history: schemas.QuizRecordRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == history.user_id).first()
    if not user:
         user = db.query(models.User).filter(models.User.username == str(history.user_id)).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    new_record = models.QuizHistory(
        unit_name=history.unit_name,
        score=history.score,
        total=history.total,
        pnl=(history.score / history.total) * 100 if history.total > 0 else 0,
        owner_id=user.id,
        timestamp=str(history.timestamp)
    )
    db.add(new_record)
    db.commit()
    return {"status": "Success", "message": "Trade recorded."}

@app.get("/api/user/{user_id}/recommendations", response_model=schemas.RecommendationResponse)
def get_recommendations(user_id: str, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.username == user_id).first()
    if not user:
        user = db.query(models.User).filter(models.User.id == (int(user_id) if user_id.isdigit() else 0)).first()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # Fetch history and active units for context
    quiz_history = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    active_units = db.query(models.Unit).filter(models.Unit.owner_id == user.id, models.Unit.is_active == True).all()
    unit_names = [u.name for u in active_units]

    user_info = {
        "username": user.username,
        "ai_persona": user.ai_persona,
        "semester_status": user.semester_status
    }

    rec_text = ai_engine.get_recommendations(user_info, quiz_history, unit_names)
    if not rec_text:
        rec_text = "Keep focusing on your current units! You're making progress."

    return schemas.RecommendationResponse(recommendation=rec_text)

if __name__ == "__main__":
    import uvicorn
    # Hugging Face Spaces uses port 7860 by default
    port = int(os.environ.get("PORT", 7860))
    uvicorn.run(app, host="0.0.0.0", port=port)
