# IDENTITY: backend/main.py
# VERSION: 1.1.0
# ⚙️ GEAR 2: The API Routes (Executing the Trades)

import os

import google.generativeai as genai
from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session

try:
    from .database import Base, engine, get_db
    from . import models as models
    from . import schemas as schemas
except ImportError:
    from database import Base, engine, get_db
    import models as models
    import schemas as schemas

# Create database tables if they don't exist
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Edu_AI Prop Firm Backend", version="1.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

GEMINI_KEY = os.environ.get("GEMINI_API_KEY", "YOUR_FALLBACK_KEY_HERE")
genai.configure(api_key=GEMINI_KEY)
ai_model = genai.GenerativeModel("gemini-1.5-flash")

@app.get("/")
def read_root():
    return {"status": "Bullish 📈", "message": "Edu_AI Backend v1.1.0 is Online."}

@app.get("/api/user/{user_id}/dashboard", response_model=schemas.DashboardResponse)
def get_dashboard(user_id: int, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")

    active_units = db.query(models.Unit).filter(
        models.Unit.owner_id == user_id, 
        models.Unit.is_active == True
    ).all()
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

@app.put("/api/user/{user_id}/preferences")
def update_preferences(user_id: int, prefs: schemas.UserPreferencesUpdate, db: Session = Depends(get_db)):
    """Updates user UI/UX preferences."""
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")
    
    if prefs.sensory_mode:
        user.sensory_mode = prefs.sensory_mode
    if prefs.ai_persona:
        user.ai_persona = prefs.ai_persona
        
    db.commit()
    return {"status": "Success", "message": "Preferences updated."}

@app.post("/api/chaos/generate_case", response_model=schemas.ChaosResponse)
def generate_medical_case(request: schemas.ChaosRequest, db: Session = Depends(get_db)):
    """Generates case, tailoring to user history if necessary."""
    
    # 1. Fetch user memory to personalize the prompt
    user = db.query(models.User).filter(models.User.id == request.student_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Student not found.")
        
    # Check history for weak spots in this unit
    weak_spots = ""
    history = db.query(models.QuizHistory).filter(
        models.QuizHistory.owner_id == request.student_id,
        models.QuizHistory.unit_name == request.unit,
        models.QuizHistory.pnl < 50.0 # If they scored less than 50%
    ).all()
    
    if history:
         weak_spots = "NOTE: Student previously struggled with this unit. Ensure the case reinforces core pathophysiological concepts."

    focus = f" specifically focusing on {request.focus_area}" if request.focus_area else ""
    
    prompt = f"""
    ACT AS: {user.ai_persona}.
    TASK: Present a "Medical Mystery" case study for a student.
    TOPIC: {request.unit}{focus}.
    DIFFICULTY: {request.difficulty}.
    {weak_spots}
    
    Format the output beautifully using Markdown. Include Patient Demographics, Vitals, Labs, 
    and a clinical presentation. End by asking the student for their differential diagnosis.
    Adapt the language to be accessible but rigorous.
    """
    
    try:
        response = ai_model.generate_content(prompt)
        return schemas.ChaosResponse(case_study=response.text)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI connection failed: {str(e)}")


if __name__ == "__main__":
    import uvicorn

    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))

    uvicorn.run(app, host=host, port=port)