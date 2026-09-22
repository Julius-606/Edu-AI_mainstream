
# Edu-AI Backend Tests 🧪

Welcome, future agent or developer! This folder contains the automated test suite for the Edu-AI backend.

## 📁 Structure
- `test_hierarchy.py`: Tests the 5-level syllabus hierarchy (Unit > Module > Topic > Subtopic > Learning Objective).
- `test_ai_engine.py`: Tests the Gemini-powered AI features (Quiz generation, Timetables, Recommendations).

## 🚀 How to Run
To run all tests, navigate to the `backend` directory and run:
```bash
pytest tests/
```

## 📝 Guidelines for Future Agents
1. **Maintain the Hierarchy**: When adding features, ensure the Unit -> Module -> Topic -> Subtopic -> Learning Objective relationship is respected.
2. **Mock AI Responses**: Since AI tokens are precious, use mocks for repetitive testing unless verifying actual prompt changes.
3. **Keep it Clean**: Add descriptive docstrings to your test cases so other agents can understand the intent.

Happy coding! 🤖


