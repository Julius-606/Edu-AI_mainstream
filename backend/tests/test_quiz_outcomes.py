import pytest
import json
from unittest.mock import MagicMock
from app.services.curriculum_service import get_unit_learning_outcomes, DEFAULT_UNIT_LEARNING_OUTCOMES
from app.services.ai_service import ai_service
from app.models import database_models as models

def test_default_curriculum_learning_outcomes():
    # Mock db session
    mock_db = MagicMock()
    mock_db.query.return_value.filter.return_value.first.return_value = None

    # Test retrieving learning outcomes for Biochemistry II with a chosen subtopic
    outcomes = get_unit_learning_outcomes(
        unit_name="Biochemistry II",
        db=mock_db,
        subtopic_name="Phosphofructokinase-1"
    )
    assert len(outcomes) > 0
    # Should include PFK-1 outcome
    assert any("Phosphofructokinase-1" in o or "PFK-1" in o for o in outcomes)

def test_database_unit_hierarchy_learning_outcomes():
    # Build simulated database Unit with modules, topics, subtopics, and learning objectives
    mock_unit = models.Unit(name="Cardiology", id=1)
    mock_module = models.Module(name="Heart Failure", unit_id=1)
    mock_topic = models.Topic(name="HFrEF vs HFpEF", module_id=1)
    
    mock_subtopic1 = models.Subtopic(name="Guideline-Directed Medical Therapy", id=10, topic_id=1)
    mock_lo1 = models.LearningObjective(description="Initiate quadruple therapy with ARNI, Beta-blocker, SGLT2i, and MRA.", subtopic_id=10)
    mock_subtopic1.learning_objectives = [mock_lo1]

    mock_subtopic2 = models.Subtopic(name="Hemodynamic Profiling", id=11, topic_id=1)
    mock_lo2 = models.LearningObjective(description="Classify warm/cold and wet/dry perfusion and congestion states.", subtopic_id=11)
    mock_subtopic2.learning_objectives = [mock_lo2]

    mock_topic.subtopics = [mock_subtopic1, mock_subtopic2]
    mock_module.topics = [mock_topic]
    mock_unit.modules = [mock_module]

    mock_db = MagicMock()
    mock_db.query.return_value.filter.return_value.first.return_value = mock_unit

    # When user chooses the specific subtopic "Guideline-Directed Medical Therapy"
    outcomes = get_unit_learning_outcomes(
        unit_name="Cardiology",
        db=mock_db,
        subtopic_name="Guideline-Directed Medical Therapy"
    )
    assert len(outcomes) >= 2
    # Chosen subtopic's learning objective MUST be first
    assert outcomes[0] == "Initiate quadruple therapy with ARNI, Beta-blocker, SGLT2i, and MRA."

def test_ai_service_fallback_quiz_includes_learning_outcomes():
    test_outcomes = [
        "Master the rate-limiting step of glycolysis via PFK-1 regulation.",
        "Identify clinical uncoupling of oxidative phosphorylation."
    ]
    quiz = ai_service._fallback_quiz(
        unit_name="Biochemistry II",
        topic="PFK-1 Regulation",
        learning_outcomes=test_outcomes
    )
    assert quiz is not None
    assert "questions" in quiz
    assert len(quiz["questions"]) >= 2
    assert quiz.get("learning_outcomes") == test_outcomes
    # Each question should reference the tested outcome
    assert quiz["questions"][0].get("learning_outcome") == test_outcomes[0]
    assert quiz["questions"][1].get("learning_outcome") == test_outcomes[1]
