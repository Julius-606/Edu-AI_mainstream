# Implementation Plan - Trace Comprehensive Fixes & Enhancements

## Goal Description
Address all reported issues in the Trace Android & backend application:
1. Formatting & weird characters cleanup across chats and learning tabs (`FormattedText`, `MarkdownText`).
2. 'Back' button fix in `LearnScreen` (chevron back / navigation action).
3. 'Ask' field in `LearnScreen` for forwarding student questions or answering AI questions during learning sessions.
4. Single conversation session per module / learning session (preventing new conversation/salutation resets on 'next').
5. Image and link rendering activation & verification in formatted output and chat.
6. Unit completion archiving behavior: completed units disappear from dashboard active list and appear only in Archives.
7. Dark mode text contrast fix: ensure text is bright/legible in dark theme (`MaterialTheme.colorScheme.onSurface`, `onBackground`).
8. Timetable creation stability & persistence improvements.
9. Account Management user settings persistence upon saving changes.
10. 'Add Units' screen outline view: clicking a unit reveals its outline.
11. Uncovered areas learning objectives display instead of "No content saved for this subtopic yet".
12. Zenith Insights daily rate-limiting/caching.

## Proposed Changes

### Frontend (Android Kotlin)
#### [MODIFY] [MarkdownText.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/components/MarkdownText.kt) & [FormattedText.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/components/FormattedText.kt)
- Clean up formatting bugs and weird character encodings. Ensure full support for Markdown tables, images, links, and text formatting with proper dark mode text colors.

#### [MODIFY] [LearnScreen.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/LearnScreen.kt) & [LearnViewModel.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/LearnViewModel.kt)
- Fix 'Back' button action.
- Wire up the 'Ask AI' text field so user messages are submitted with `nextObjective` or interactive chat within the learning session.
- Maintain session state so the AI treats the module session continuously without resetting greetings on each 'Next'.

#### [MODIFY] [StudentDashboard.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/StudentDashboard.kt) & [LibraryScreen.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/student/LibraryScreen.kt)
- Filter completed units out of the active dashboard and into Archives.
- Enable unit outline click on 'Add Units' / Library screen.
- Show learning objectives for uncovered areas instead of placeholder error text.
- Ensure Zenith Insights refreshes at most once a day.
- Fix dark mode text styling across dashboard, account settings, and cards.

### Backend (Python FastAPI)
#### [MODIFY] [learning.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/app/api/learning.py) & [ai_service.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/app/services/ai_service.py)
- Support conversation history and context retention per learning module session.
- Handle student input questions during learning objective steps.
