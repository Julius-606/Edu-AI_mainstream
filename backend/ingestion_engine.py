
import re
import json
from sqlalchemy.orm import Session
from app.models import database_models as models

def parse_syllabus_markdown(text):
    """
    Parses a markdown string into a structured 5-level syllabus JSON.
    """
    lines = text.split('\n')
    syllabus = {
        "syllabus_title": "General Syllabus",
        "units": []
    }

    current_unit = None
    current_module = None
    current_topic = None
    current_subtopic = None

    for line in lines:
        line = line.strip()
        if not line:
            continue

        if line.startswith('# '):
            syllabus["syllabus_title"] = line[2:].strip()
        elif line.startswith('## '):
            current_unit = {
                "unit_title": line[3:].strip(),
                "modules": []
            }
            syllabus["units"].append(current_unit)
            current_module = current_topic = current_subtopic = None
        elif line.startswith('### '):
            if current_unit is not None:
                current_module = {
                    "module_title": line[4:].strip(),
                    "topics": []
                }
                current_unit["modules"].append(current_module)
                current_topic = current_subtopic = None
        elif line.startswith('#### '):
            if current_module is not None:
                current_topic = {
                    "topic_title": line[5:].strip(),
                    "subtopics": []
                }
                current_module["topics"].append(current_topic)
                current_subtopic = None
        elif line.startswith('##### '):
            if current_topic is not None:
                current_subtopic = {
                    "subtopic_title": line[6:].strip(),
                    "learning_objectives": []
                }
                current_topic["subtopics"].append(current_subtopic)
        elif line.startswith('- ') or line.startswith('* '):
            if current_subtopic is not None:
                current_subtopic["learning_objectives"].append(line[2:].strip())

    return syllabus

def save_syllabus_to_db(db: Session, syllabus_data: dict, owner_id=None):
    """
    Saves parsed syllabus data into the database.
    If owner_id is None, it's a global/available unit.
    """
    for unit_data in syllabus_data.get("units", []):
        db_unit = models.Unit(
            name=unit_data["unit_title"],
            owner_id=owner_id,
            category="Global" if owner_id is None else "Personal"
        )
        db.add(db_unit)
        db.flush()

        for module_data in unit_data.get("modules", []):
            db_module = models.Module(name=module_data["module_title"], unit_id=db_unit.id)
            db.add(db_module)
            db.flush()

            for topic_data in module_data.get("topics", []):
                db_topic = models.Topic(name=topic_data["topic_title"], module_id=db_module.id)
                db.add(db_topic)
                db.flush()

                for subtopic_data in topic_data.get("subtopics", []):
                    db_subtopic = models.Subtopic(name=subtopic_data["subtopic_title"], topic_id=db_topic.id)
                    db.add(db_subtopic)
                    db.flush()

                    for obj in subtopic_data.get("learning_objectives", []):
                        db_obj = models.LearningObjective(description=obj, subtopic_id=db_subtopic.id)
                        db.add(db_obj)

    db.commit()

def get_global_units(db: Session):
    return db.query(models.Unit).filter(models.Unit.owner_id.is_(None)).all()

def delete_unit(db: Session, unit_id: int):
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if unit:
        db.delete(unit)
        db.commit()
        return True
    return False

def update_unit(db: Session, unit_id: int, name: str):
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if unit:
        unit.name = name
        db.commit()
        return True
    return False

def clone_unit_to_user(db: Session, unit_id: int, user_id: int):
    """
    Clones a global unit (and its hierarchy) to a specific user.
    """
    global_unit = db.query(models.Unit).filter(models.Unit.id == unit_id).first()
    if not global_unit:
        return None

    # Create new Unit
    new_unit = models.Unit(
        name=global_unit.name,
        owner_id=user_id,
        category="Ongoing"
    )
    db.add(new_unit)
    db.flush()

    for module in global_unit.modules:
        new_module = models.Module(name=module.name, unit_id=new_unit.id)
        db.add(new_module)
        db.flush()

        for topic in module.topics:
            new_topic = models.Topic(name=topic.name, module_id=new_module.id)
            db.add(new_topic)
            db.flush()

            for subtopic in topic.subtopics:
                new_subtopic = models.Subtopic(name=subtopic.name, topic_id=new_topic.id)
                db.add(new_subtopic)
                db.flush()

                for obj in subtopic.learning_objectives:
                    new_obj = models.LearningObjective(description=obj.description, subtopic_id=new_subtopic.id)
                    db.add(new_obj)

    db.commit()
    return new_unit


 