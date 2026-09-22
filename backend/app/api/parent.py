
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.services.ai_service import ai_service

router = APIRouter(prefix="/parent", tags=["Parent Portal"])

def find_user(user_id_or_name: str, db: Session):
    if str(user_id_or_name).isdigit():
        return db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    return db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()

@router.get("/dashboard/{student_id}", response_model=schemas.ParentDashboardResponse)
def get_parent_dashboard(student_id: str, db: Session = Depends(get_db)):
    student = find_user(student_id, db)
    if not student: raise HTTPException(status_code=404, detail="Student record not found")

    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == student.id).order_by(models.QuizHistory.id.desc()).limit(5).all()

    review_prompt = f"Review progress for parent: {student.username}, Units: {', '.join(student.active_units_list)}, Avg: {sum([q.pnl for q in quizzes])/len(quizzes) if quizzes else 0}%"
    ai_review = ai_service.ask(review_prompt, system_instruction="Act as a supportive AI Education Consultant.")

    return schemas.ParentDashboardResponse(
        student_name=student.username,
        academic_status=student.semester_status,
        current_study_path=student.active_units_list,
        ai_progress_review=ai_review or "Compiling progress data...",
        teacher_remarks="Student is showing consistent engagement with AI modules.",
        recent_grades=[schemas.QuizHistoryResponse(unit_name=q.unit_name, pnl=q.pnl, timestamp=q.timestamp) for q in quizzes]
    )


 