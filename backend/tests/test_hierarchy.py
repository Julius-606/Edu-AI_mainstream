
import pytest
from ingestion_engine import parse_syllabus_markdown

def test_parse_syllabus_hierarchy():
    markdown = """
# MED ED
## Unit 1: Anatomy
### Module 1.1: Upper Limb
#### Topic 1.1.1: Brachial Plexus
##### Subtopic A: Roots and Trunks
- Understand C5-T1 origin.
- Identify Erb's palsy symptoms.
"""
    result = parse_syllabus_markdown(markdown)

    assert result["syllabus_title"] == "MED ED"
    assert len(result["units"]) == 1
    unit = result["units"][0]
    assert unit["unit_title"] == "Unit 1: Anatomy"

    assert len(unit["modules"]) == 1
    module = unit["modules"][0]
    assert module["module_title"] == "Module 1.1: Upper Limb"

    assert len(module["topics"]) == 1
    topic = module["topics"][0]
    assert topic["topic_title"] == "Topic 1.1.1: Brachial Plexus"

    assert len(topic["subtopics"]) == 1
    subtopic = topic["subtopics"][0]
    assert subtopic["subtopic_title"] == "Subtopic A: Roots and Trunks"

    assert len(subtopic["learning_objectives"]) == 2
    assert subtopic["learning_objectives"][0] == "Understand C5-T1 origin."

def test_empty_parsing():
    result = parse_syllabus_markdown("")
    assert result["syllabus_title"] == "General Syllabus"
    assert len(result["units"]) == 0


