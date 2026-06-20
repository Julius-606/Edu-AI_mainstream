import re
import json

def parse_syllabus_markdown(text):
    """
    Parses a markdown string into a structured syllabus JSON.
    Expected format:
    # Syllabus Title
    ## Unit 1: Name
    ### 1. Topic Name
    - Subtopic A
    - Subtopic B
    """
    lines = text.split('\n')
    syllabus = {
        "syllabus_title": "Untitled Syllabus",
        "units": []
    }

    current_unit = None
    current_topic = None

    for line in lines:
        line = line.strip()
        if not line:
            continue

        if line.startswith('# '):
            syllabus["syllabus_title"] = line[2:].strip()
        elif line.startswith('## '):
            current_unit = {
                "unit_title": line[3:].strip(),
                "topics": []
            }
            syllabus["units"].append(current_unit)
            current_topic = None
        elif line.startswith('### '):
            if current_unit is not None:
                current_topic = {
                    "topic_title": line[4:].strip(),
                    "subtopics": []
                }
                current_unit["topics"].append(current_topic)
        elif line.startswith('- ') or line.startswith('* '):
            if current_topic is not None:
                current_topic["subtopics"].append(line[2:].strip())
            elif current_unit is not None:
                # If no topic yet, maybe create a default one or handle as unit-level subtopic?
                # For now, let's assume hierarchy is strict.
                pass

    return syllabus

if __name__ == "__main__":
    demo_syllabus = """
# THE FULL DEMON-TIME SYLLABUS
## Unit 1: Internal Medicine
### 1. Cardiology
- A. Heart Failure (HF)
- B. Ischemic Heart Disease
### 2. Pulmonology
- A. Asthma
- B. COPD
## Unit 2: Surgery
### 1. General Surgery
- A. Appendicitis
- B. Hernia
"""
    structured_data = parse_syllabus_markdown(demo_syllabus)
    print(json.dumps(structured_data, indent=2))
