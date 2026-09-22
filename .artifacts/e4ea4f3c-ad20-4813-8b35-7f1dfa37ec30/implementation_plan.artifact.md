# Enhancing Content Persistence and AI Integration

This plan aims to resolve the "forgetting" learned data issue by revamping how learning progress is stored and synchronized. It also incorporates the new AI interaction model, where the backend dictates the content generation based on learning objectives, and the AI provides consistent, non-personalized responses.

## User Review Required

*   **Significant Data Model Changes**: This plan involves modifying existing database entities and potentially introducing new ones. This will require a database migration if users have existing data.
*   **Backend Changes**: The AI content generation and objective-driven prompting will primarily reside on the backend. This plan will focus on the client-side changes required to support this new backend functionality.
*   **Refactoring**: The current `SyncOperationEntity` will be replaced with a more robust system that tracks detailed content completion rather than just subtopic status.

## Open Questions

*   What are the specific "course objectives" that the AI will be prompted with? (e.g., "Learn about Kotlin Coroutines", "Understand Jetpack Compose Basics")
*   How should the "learnt content" for a given objective be structured? (e.g., a list of topic IDs, a single content blob, etc.)
*   What is the desired behavior when the app is offline regarding content generation and progress tracking? Should it cache objectives/content, or strictly rely on online backend interaction for new content?

## Proposed Changes

### `app` module

#### [MODIFY] [EduAIDao.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/data/local/EduAIDao.kt)

*   Remove `updateSubtopicStatus`.
*   Add methods to track `ContentCompletionEntity` (new entity) and retrieve completion status for given objectives/content.
*   Update queries related to subtopic progress to use the new content completion tracking.

#### [NEW] [ContentCompletionEntity.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/data/local/ContentCompletionEntity.kt)

*   A new Room entity to store detailed progress for "learnt content" based on course objectives. This will include fields for `objectiveId`, `userId`, `contentId`, `isCompleted`, and `lastUpdated`.

#### [MODIFY] [EduAIRepository.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/repository/EduAIRepository.kt)

*   Update `updateSubtopicProgress` to utilize `ContentCompletionEntity` and the new DAO methods.
*   Modify `syncPendingChanges` to handle the synchronization of `ContentCompletionEntity` with the backend.
*   Introduce new methods for interacting with the backend for AI-generated content based on course objectives.

#### [MODIFY] [ApiModels.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/data/remote/ApiModels.kt)

*   Add new data classes for `CourseObjectiveRequest`, `CourseObjectiveResponse`, and potentially `ContentCompletionSyncRequest`/`Response` to support the new backend AI interaction and content completion synchronization.

#### [MODIFY] [EduAIApi.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/data/remote/EduAIApi.kt)

*   Add new API endpoints for submitting course objectives to the backend AI and receiving generated content.
*   Add API endpoints for syncing content completion status.

#### [MODIFY] [StudentDashboard.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/StudentDashboard.kt)

*   Update the UI logic to fetch and display progress based on the new `ContentCompletionEntity` model.
*   Integrate with the new repository methods to trigger AI content generation based on user interaction with learning objectives.

## Verification Plan

### Automated Tests

*   **Unit Tests for `EduAIDao`**: Create or modify unit tests to ensure `insert`, `update`, and `query` operations for `ContentCompletionEntity` work as expected.
*   **Unit Tests for `EduAIRepository`**:
    *   Test the `updateSubtopicProgress` method to ensure it correctly updates `ContentCompletionEntity` and triggers synchronization.
    *   Test the new AI interaction methods, mocking the `EduAIApi` to verify correct request/response handling.
*   **Integration Tests**: If possible, write integration tests that simulate the entire flow of completing a subtopic, updating progress, and verifying persistence across app restarts.

### Manual Verification

*   **End-to-end user flow**:
    1.  Launch the app.
    2.  Select a course objective.
    3.  Complete a subtopic within that objective.
    4.  Navigate back to the dashboard and verify that the progress is correctly displayed and persists across app restarts.
    5.  Go offline, complete a subtopic, then go online again to verify proper synchronization.
*   **Network Monitoring**: Use Android Studio's Network Profiler to monitor API calls for content completion synchronization and AI content generation to ensure they are being made correctly.
*   **Database Inspection**: Continuously use the Database Inspector to observe the `content_completion` table (the new entity) and `sync_operations` table to confirm data is being stored and processed as expected.