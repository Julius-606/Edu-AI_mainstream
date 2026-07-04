import re
import json

def parse_syllabus_markdown(text):
    """
    Parses a markdown string into a structured 5-level syllabus JSON.
    Format:
    # Syllabus Title (Optional)
    ## Unit Name
    ### Module Name
    #### Topic Name
    ##### Subtopic Name
    - Learning Objective
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

if __name__ == "__main__":
    demo_syllabus = """
# MEDICAL SCHOOL SURVIVAL KIT
## Unit 1: Internal Medicine
### Module 1.1: Cardiovascular System
#### Topic 1.1.1: Heart Failure
##### Subtopic A: Pathophysiology of CHF
- Understand the role of the RAAS system.
- Differentiate between systolic and diastolic failure.
##### Subtopic B: Clinical Presentation
- Identify JVD and peripheral edema.
### Module 1.2: Respiratory System
#### Topic 1.2.1: Obstructive Diseases
##### Subtopic A: Asthma
- Recognize wheezing and triggers.
"""
    structured_data = parse_syllabus_markdown(demo_syllabus)
    print(json.dumps(structured_data, indent=2))
