# Implementation Plan - Trace Comprehensive Fixes & Enhancements

## Goal Description
Address all reported issues in the Trace Android & backend application:
1. Formatting & weird characters cleanup across chats and learning tabs (`FormattedText`, `MarkdownText`).
2. 'Back' button fix in `LearnScreen`.
3. 'Ask' field in `LearnScreen` for forwarding student questions or answering AI questions.
4. Single conversation session per module / learning session (preventing new conversation/salutation resets on 'next').
5. Image and link rendering activation & verification.
6. Unit completion archiving behavior: completed units disappear from active dashboard and appear only in Archives.
7. Dark mode text contrast fix: ensure text is bright/legible in dark theme.
8. Timetable creation stability & persistence improvements.
9. Account Management user settings persistence upon saving changes.
10. 'Add Units' screen outline view: clicking a unit reveals its outline.
11. Uncovered areas learning objectives display instead of "No content saved for this subtopic yet".
12. Zenith Insights daily rate-limiting/caching.

## Proposed Changes

### Frontend (Android Kotlin)
- Fix markdown and text formatting parsing across `FormattedText.kt` and `MarkdownText.kt`.
- Fix Back button and Ask field in `LearnScreen.kt` & `LearnViewModel.kt`.
- Update dashboard unit filtering to archive completed units automatically.
- Fix dark mode text colors in `Theme.kt` and components.
- Fix settings persistence in `EduAIRepository.kt` and `StudentDashboard.kt`.
- Add unit outline view when clicking units in Library/Add units.
- Cache/rate limit Zenith Insights calls to once a day.

### Backend (Python FastAPI)
- Update learning session endpoints to maintain conversational context across objective steps.
