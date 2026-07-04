# Implementation Plan - Phase 3: Structured Learning & Dashboard Redesign

This plan outlines the evolution of the Edu-AI platform to support a hierarchical syllabus (Unit > Module > Topic > Subtopic > Learning Objective), a redesigned dashboard with concentric progress rings, and a dynamic fallback for the backend URL.

## User Review Required

> [!IMPORTANT]
> The hierarchy change is a significant database evolution. Existing data in `modules` and `subtopics` will need to be migrated or reset. I recommend a database reset for development simplicity if the user doesn't have critical data.

- **Concentric Rings**: I'll implement 4 rings per Unit. 1st=Unit, 2nd=Current Module, 3rd=Current Topic, 4th=Current Subtopic.
- **Dynamic Background**: I'll use a set of local or remote academic-themed images that cycle every few minutes.

## Proposed Changes

### Backend (Python/FastAPI)

#### [NEW] [tests/README.md](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/tests/README.md)
- Create a dedicated tests folder.
- Add a README.md to guide future agents on testing the hierarchical structure and AI features.

#### [models.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/models.py)
- Add `Topic` table between `Module` and `Subtopic`.
- Add `LearningObjective` table under `Subtopic`.
- Add `UserSyllabusProgress` table to track status (Locked, Unlocked, In_Progress, Completed).
- Update relationships to reflect the 5-level hierarchy.

#### [schemas.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/schemas.py)
- Update `UnitResponse`, `ModuleResponse`, `SubtopicResponse`.
- Add `TopicResponse` and `LearningObjectiveResponse`.
- Update `DashboardResponse` to include progress percentages for each level.

#### [ingestion_engine.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/ingestion_engine.py)
- Update `parse_syllabus_markdown` to handle:
    - `# Syllabus`
    - `## Unit`
    - `### Module`
    - `#### Topic`
    - `##### Subtopic`
    - `- Learning Objective`

#### [main.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/main.py)
- Update `/api/v1/syllabuses/upload` to handle the new hierarchy.
- Update `/api/v1/syllabuses/{unit_id}/tree` to return the full 5-level tree.
- Add `PATCH /api/v1/progress/node/{node_id}` to update any node's status.
- Update `/api/user/{user_id}/dashboard` to return unit-wise hierarchical progress.

#### [ai_engine.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/ai_engine.py)
- Update prompts for `generate_quiz` and `get_recommendations` to leverage the new hierarchy for pinpoint accuracy.

---

### Frontend (Android/Compose)

#### [RetrofitClient.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/data/remote/RetrofitClient.kt)
- Implement `FallbackInterceptor` to catch `IOException` and switch `BASE_URL` to `FALLBACK_BACKEND_BASE_URL`.

#### [StudentDashboard.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/StudentDashboard.kt)
- Redesign the layout to move Zenith and Timetable content here.
- Integrate `DynamicBackground` and `ConcentricRings`.

#### [NEW] [DynamicBackground.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/components/DynamicBackground.kt)
- A composable that displays a faded background image that fades between different academic scenes.

#### [NEW] [ProgressRings.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/components/ProgressRings.kt)
- A custom Canvas-based composable that draws the four concentric rings and the scrollable learning objectives in the center.

## Verification Plan

### Automated Tests
- Create a dedicated `backend/tests/` directory.
- Move/Create `backend/tests/test_ai_engine.py` to ensure AI functionality is intact.
- Create `backend/tests/test_hierarchy.py` to test the new ingestion and API tree retrieval.
```bash
pytest backend/tests/
```

### Manual Verification
- **Backend**: Use `/docs` (Swagger) to upload a markdown syllabus and verify the tree JSON.
- **Frontend**:
    1. Start the app with the main backend.
    2. Shut down/pause the main backend and verify it switches to `127.0.0.1:8000`.
    3. Verify the Dashboard UI shows the concentric rings for uploaded units.
    4. Verify the background changes periodically.
