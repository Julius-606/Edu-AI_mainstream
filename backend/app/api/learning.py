
from fastapi import APIRouter, Depends, HTTPException, Body
from sqlalchemy.orm import Session
from typing import List
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from ingestion_engine import parse_syllabus_markdown

router = APIRouter(tags=["Structured Learning"])

def find_user(user_id_or_name: str, db: Session):
    if str(user_id_or_name).isdigit():
        return db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    return db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()

@router.get("/syllabuses")
def get_all_syllabuses(db: Session = Depends(get_db)):
    return db.query(models.Unit).all()

@router.get("/syllabuses/{unit_id}/tree", response_model=schemas.UnitResponse)
def get_syllabus_tree(unit_id: int, db: Session = Depends(get_db)):
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if not unit:
        raise HTTPException(status_code=404, detail="Syllabus/Unit not found")
    return unit

@router.patch("/progress/subtopic/{subtopic_id}")
def update_subtopic_progress(subtopic_id: int, is_completed: bool, db: Session = Depends(get_db)):
    subtopic = db.query(models.Subtopic).filter(models.Subtopic.id == subtopic_id).first()
    if not subtopic:
        raise HTTPException(status_code=404, detail="Subtopic not found")

    subtopic.is_completed = is_completed
    db.commit()
    db.refresh(subtopic)
    return {"status": "success", "subtopic_id": subtopic_id, "is_completed": is_completed}

@router.post("/syllabuses/ingest-markdown")
def ingest_syllabus_markdown(
    user_id: str,
    markdown_text: str = Body(..., embed=True),
    db: Session = Depends(get_db)
):
    """
    Parses Markdown syllabus and uploads it to the database for the specified user.
    """
    structured_data = parse_syllabus_markdown(markdown_text)
    return upload_syllabus(structured_data, user_id, db)

@router.post("/syllabuses/upload")
def upload_syllabus(payload: dict, user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    created_units = []
    for unit_data in payload.get("units", []):
        new_unit = models.Unit(
            name=unit_data.get("unit_title"),
            owner_id=user.id,
            category=payload.get("syllabus_title", "General")
        )
        db.add(new_unit)
        db.commit()
        db.refresh(new_unit)

        for module_data in unit_data.get("modules", []):
            new_module = models.Module(
                name=module_data.get("module_title"),
                unit_id=new_unit.id
            )
            db.add(new_module)
            db.commit()
            db.refresh(new_module)

            for topic_data in module_data.get("topics", []):
                new_topic = models.Topic(
                    name=topic_data.get("topic_title"),
                    module_id=new_module.id
                )
                db.add(new_topic)
                db.commit()
                db.refresh(new_topic)

                for subtopic_data in topic_data.get("subtopics", []):
                    new_subtopic = models.Subtopic(
                        name=subtopic_data.get("subtopic_title"),
                        topic_id=new_topic.id
                    )
                    db.add(new_subtopic)
                    db.commit()
                    db.refresh(new_subtopic)

                    for objective_text in subtopic_data.get("learning_objectives", []):
                        new_objective = models.LearningObjective(
                            description=objective_text,
                            subtopic_id=new_subtopic.id
                        )
                        db.add(new_objective)

        db.commit()
        created_units.append(new_unit.id)

    return {"status": "success", "created_unit_ids": created_units}

@router.post("/quiz/record")
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


