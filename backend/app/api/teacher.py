
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.services.ai_service import ai_service

router = APIRouter(prefix="/teacher", tags=["Teacher Portal"])

@router.get("/dashboard", response_model=schemas.TeacherDashboardResponse)
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

@router.post("/class-report", response_model=schemas.ClassReportResponse)
def generate_class_report(db: Session = Depends(get_db)):
    dashboard = get_teacher_dashboard(db)
    at_risk_students = dashboard.action_required_queue[:10]
    student_lines = [
        f"{student.username}: {student.risk_reason or 'No risk reason recorded'}"
        for student in at_risk_students
    ]
    prompt = (
        "Create a concise teacher-facing class report. Include class health, "
        "top intervention priorities, and recommended next actions.\n"
        f"Total active students: {dashboard.total_active_students}\n"
        f"Class health score: {dashboard.class_health_score}%\n"
        f"Action queue:\n" + "\n".join(student_lines)
    )
    report = ai_service.ask(prompt, system_instruction="You are an academic analytics assistant.")
    return schemas.ClassReportResponse(report=report or "Class report is temporarily unavailable.")

@router.post("/send-report/{student_id}")
def send_student_report(student_id: int, db: Session = Depends(get_db)):
    student = db.query(models.User).filter(models.User.id == student_id).first()
    if not student: raise HTTPException(status_code=404, detail="Student not found")

    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == student.id).all()
    context = f"Student: {student.username}\nStatus: {student.semester_status}\nUnits: {', '.join(student.active_units_list)}\n"
    context += "Grades: " + ", ".join([f"{q.unit_name}: {q.pnl}%" for q in quizzes])

    prompt = f"Create a concise, encouraging progress report for a parent based on this data. Translate technical rubrics into accessible feedback:\n{context}"
    ai_summary = ai_service.ask(prompt, system_instruction="You are a pedagogical report assistant.")

    return {"status": "Success", "message": f"Report sent to parent of {student.username}", "ai_summary": ai_summary}


