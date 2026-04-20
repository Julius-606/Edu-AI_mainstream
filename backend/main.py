# IDENTITY: backend/main.py
# VERSION: 2.6.0
# ⚙️ GEAR 2: The API Routes (Executing the Trades)

import os
import time
from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import List, Optional
import logging

try:
    from database import Base, engine, get_db
    import models as models
    import schemas as schemas
    from ai_engine import ai_engine
except ImportError:
    from database import Base, engine, get_db
    from . import models as models
    from . import schemas as schemas
    from .ai_engine import ai_engine

# Create database tables if they don't exist
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Edu_AI Learning System", version="2.6.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {"status": "Active 📚", "message": "Edu_AI Backend v2.6.0 is Online."}

# Helper to find user by ID (int) or Username (string)
def find_user(user_id_or_name: str, db: Session):
    user = None
    if str(user_id_or_name).isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    if not user:
        user = db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()
    return user

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
    user = find_user(user_id, db)
    if not user:
        role = "Student"
        if "teacher" in user_id.lower(): role = "Teacher"
        if "parent" in user_id.lower(): role = "Parent"

        user = models.User(
            username=user_id,
            role=role,
            sensory_mode="Standard",
            ai_persona="Standard Edu_AI",
            semester_status="Active"
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    response = schemas.UserResponseSchema.from_orm(user)
    response.active_units = user.active_units_list
    return response

@app.put("/api/users/{user_id}", response_model=schemas.UserResponseSchema, tags=["User Management"])
def update_user(user_id: str, user_update: schemas.UserUpdate, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    update_data = user_update.dict(exclude_unset=True)

    # Handle units separately if provided
    if "active_units" in update_data:
        new_unit_names = update_data.pop("active_units")
        # Deactivate all current units first
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

# --- TEACHER PORTAL ENDPOINTS ---

@app.get("/api/teacher/dashboard", response_model=schemas.TeacherDashboardResponse, tags=["Teacher Portal"])
def get_teacher_dashboard(db: Session = Depends(get_db)):
    all_students = db.query(models.User).filter(models.User.role == "Student").all()

    # Ensure some mock data exists if db is empty
    if not all_students:
        mock_students = [
            ("Neema Ongaga", "Year 4 - Redemption Arc"),
            ("Grace Naliaka", "Clinical Rotations"),
            ("Rayvins Otieno", "Pre-med Hustle"),
            ("Hillary Lweya", "Final Year"),
            ("Tatiana A.", "Anatomy Focus")
        ]
        for name, status in mock_students:
            s = models.User(username=name, role="Student", semester_status=status)
            db.add(s)
            db.commit()
            db.refresh(s)
            db.add(models.Unit(name="Biochemistry II", owner_id=s.id))
            db.commit()
        all_students = db.query(models.User).filter(models.User.role == "Student").all()

    action_queue = []
    total_score = 0
    count = 0

    for student in all_students:
        quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == student.id).all()
        total_quizzes = len(quizzes)
        avg_pnl = sum([q.pnl for q in quizzes]) / total_quizzes if total_quizzes > 0 else 0.0

        total_score += avg_pnl
        count += 1

        is_at_risk = False
        risk_reason = None

        # Logic for "Action Required" queue
        if avg_pnl < 65 and total_quizzes > 0:
            is_at_risk = True
            risk_reason = f"Performance drop: {round(avg_pnl, 1)}% avg score. Intervention recommended."
        elif total_quizzes == 0:
            is_at_risk = True
            risk_reason = "No assessment data recorded. Learning path stalled."

        if is_at_risk:
            action_queue.append(schemas.StudentSummary(
                id=student.id,
                username=student.username,
                average_pnl=round(avg_pnl, 2),
                total_quizzes=total_quizzes,
                semester_status=student.semester_status,
                active_units=student.active_units_list,
                is_at_risk=True,
                risk_reason=risk_reason
            ))

    class_health = total_score / count if count > 0 else 100.0

    return schemas.TeacherDashboardResponse(
        action_required_queue=action_queue,
        total_active_students=len(all_students),
        class_health_score=round(class_health, 2)
    )

@app.post("/api/teacher/send-report/{student_id}", tags=["Teacher Portal"])
def send_student_report(student_id: int, db: Session = Depends(get_db)):
    student = db.query(models.User).filter(models.User.id == student_id).first()
    if not student: raise HTTPException(status_code=404, detail="Student not found")

    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == student.id).all()
    context = f"Student: {student.username}\nStatus: {student.semester_status}\nUnits: {', '.join(student.active_units_list)}\n"
    context += "Grades: " + ", ".join([f"{q.unit_name}: {q.pnl}%" for q in quizzes])

    prompt = f"Create a concise, encouraging progress report for a parent based on this data. Translate technical rubrics into accessible feedback:\n{context}"
    ai_summary = ai_engine.ask(prompt, system_instruction="You are a pedagogical report assistant.")

    # Store this as a 'teacher remark' for the parent portal
    # For now, we'll just return it. In a full system, you'd save this to a 'Reports' table.
    return {"status": "Success", "message": f"Report sent to parent of {student.username}", "ai_summary": ai_summary}

# --- PARENT PORTAL ENDPOINTS ---

@app.get("/api/parent/dashboard/{student_id}", response_model=schemas.ParentDashboardResponse, tags=["Parent Portal"])
def get_parent_dashboard(student_id: str, db: Session = Depends(get_db)):
    student = find_user(student_id, db)
    if not student: raise HTTPException(status_code=404, detail="Student record not found")

    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == student.id).order_by(models.QuizHistory.id.desc()).limit(5).all()

    review_prompt = f"Review progress for parent: {student.username}, Units: {', '.join(student.active_units_list)}, Avg: {sum([q.pnl for q in quizzes])/len(quizzes) if quizzes else 0}%"
    ai_review = ai_engine.ask(review_prompt, system_instruction="Act as a supportive AI Education Consultant.")

    return schemas.ParentDashboardResponse(
        student_name=student.username,
        academic_status=student.semester_status,
        current_study_path=student.active_units_list,
        ai_progress_review=ai_review or "Compiling progress data...",
        teacher_remarks="Student is showing consistent engagement with AI modules.",
        recent_grades=[schemas.QuizHistoryResponse(unit_name=q.unit_name, pnl=q.pnl, timestamp=q.timestamp) for q in quizzes]
    )

# --- DASHBOARD & ACTIVITY ---

@app.get("/api/user/{user_id}/dashboard", response_model=schemas.DashboardResponse)
def get_dashboard(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)

    if not user:
        user = models.User(
            username=user_id,
            role="Student",
            sensory_mode="Standard",
            ai_persona="Socratic Tutor",
            semester_status="Active"
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    active_units = db.query(models.Unit).filter(
        models.Unit.owner_id == user.id,
        models.Unit.is_active == True
    ).all()

    if not active_units:
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

@app.get("/api/user/{user_id}/timetable", response_model=schemas.TimetableResponse, tags=["Activity & Planning"])
def get_ai_timetable(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user: raise HTTPException(status_code=404, detail="User not found")

    # Check for existing timetable generated in the last 7 days
    one_week_ago = time.time() - (7 * 24 * 60 * 60)
    existing_timetable = db.query(models.Timetable).filter(
        models.Timetable.owner_id == user.id,
        models.Timetable.timestamp > one_week_ago
    ).order_by(models.Timetable.timestamp.desc()).first()

    if existing_timetable:
        return schemas.TimetableResponse(
            weekly_plan=existing_timetable.weekly_plan_json,
            ai_brief=existing_timetable.ai_brief
        )

    # If no recent timetable, generate a new one with continuity
    last_timetable = db.query(models.Timetable).filter(
        models.Timetable.owner_id == user.id
    ).order_by(models.Timetable.timestamp.desc()).first()

    previous_plan = last_timetable.weekly_plan_json if last_timetable else None

    quiz_history = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    active_units = db.query(models.Unit).filter(models.Unit.owner_id == user.id, models.Unit.is_active == True).all()

    # NEW LOGIC: Look at the past ten chat session titles instead of raw messages
    recent_sessions = db.query(models.ChatSession).filter(
        models.ChatSession.owner_id == user.id
    ).order_by(models.ChatSession.id.desc()).limit(10).all()
    chat_titles = [s.title for s in recent_sessions]

    user_info = {
        "username": user.username,
        "semester_status": user.semester_status
    }
    unit_names = [u.name for u in active_units]

    new_timetable_data = ai_engine.generate_timetable(
        user_info, quiz_history, unit_names, chat_titles, previous_plan
    )

    if not new_timetable_data:
        raise HTTPException(status_code=500, detail="The AI is still drafting your plan. Try again in a moment.")

    # Save to database
    new_db_timetable = models.Timetable(
        owner_id=user.id,
        weekly_plan_json=new_timetable_data["weekly_plan"],
        ai_brief=new_timetable_data["ai_brief"],
        timestamp=time.time()
    )
    db.add(new_db_timetable)
    db.commit()

    return schemas.TimetableResponse(
        weekly_plan=new_timetable_data["weekly_plan"],
        ai_brief=new_timetable_data["ai_brief"]
    )

# --- AI ENDPOINTS ---

@app.post("/api/ai/chat", response_model=schemas.ChatResponse)
def ai_chat(request: schemas.ChatRequest, db: Session = Depends(get_db)):
    user = find_user(str(request.user_id), db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    user_msg = models.ChatMessage(role="user", content=request.prompt, owner_id=user.id)
    db.add(user_msg)
    db.commit()

    system_instruction = f"You are {user.ai_persona} (an AI Study Companion). " \
                         f"The student is at level: {user.semester_status}. " \
                         f"Be encouraging, concise, and educational."
    
    history_text = ""
    for msg in request.history:
        history_text += f"{msg.role}: {msg.content}\n"

    full_prompt = f"{history_text}User: {request.prompt}"

    response_text = ai_engine.ask(prompt=full_prompt, system_instruction=system_instruction)
    if not response_text:
        raise HTTPException(status_code=500, detail="AI engine is currently unavailable.")

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
    user = find_user(str(request.user_id), db)
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
    user = find_user(str(history.user_id), db)
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
    return {"status": "Success", "message": "Result recorded."}

@app.get("/api/user/{user_id}/recommendations", response_model=schemas.RecommendationResponse)
def get_recommendations(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

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
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
