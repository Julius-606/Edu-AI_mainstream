# [PHASE 3] Dashboard Frontend Prioritization & Hierarchical Visualization

This plan focuses on enhancing the Student Dashboard with dynamic, hierarchical progress visualization and an immersive UI. It also includes the necessary backend changes to support the 5-level hierarchy.

## User Review Required

> [!IMPORTANT]
> The dashboard now displays **hierarchical progress** (Unit > Module > Subtopic). I've updated the frontend to use `UnitWithModules` and `ModuleWithSubtopics`.
> I am now proceeding to align the Backend (FastAPI) to support this full hierarchy during syllabus ingestion.

## Proposed Changes

### [Frontend] Dashboard & Components [COMPLETED]

#### [DynamicBackground.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/components/DynamicBackground.kt)
- Enhanced with an animated gradient and cycling icons for a premium feel.

#### [ProgressRings.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/components/ProgressRings.kt)
- Refined to handle hierarchical rings with entry animations.

#### [StudentDashboard.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/StudentDashboard.kt)
- Updated to observe hierarchical data and integrated Zenith Insights/Timetable.

---

### [Backend] Hierarchical Syllabus Support

#### [api_schemas.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/app/schemas/api_schemas.py)
- Update `ModuleResponse` to include `topics`.
- Add `TopicResponse` and `LearningObjectiveResponse` to match the DB models.

#### [learning.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/app/api/learning.py)
- Update `upload_syllabus` to correctly map the 5-level hierarchy from `ingestion_engine.py` into the database models.
- Ensure `UnitResponse` in `get_syllabus_tree` recursively includes all levels.

#### [ingestion_engine.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/ingestion_engine.py)
- Refine the parser to strictly follow the `#`, `##`, `###`, `####`, `#####` and `-` convention for Unit, Module, Topic, Subtopic, and Objectives.

---

### [Frontend] ViewModel & Data Layer [COMPLETED]

#### [StudentViewModel.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/StudentViewModel.kt)
- Updated `StudentUiState` and flow to include `unitsWithModules`.

#### [EduAIRepository.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/repository/EduAIRepository.kt)
- Updated with mock hierarchy generation for local development testing.

## Verification Plan

### Automated Tests
- N/A for UI changes.
- Will verify Backend ingestion via Swagger (`/docs`).

### Manual Verification
- **Visual Check**: Run the app and verify the `DynamicBackground` and `ProgressRings` animations.
- **Backend Ingestion**: Upload a markdown syllabus using the new 5-level format and verify the tree JSON matches.
- **End-to-End**: Verify the Dashboard reflects the uploaded hierarchy correctly.
