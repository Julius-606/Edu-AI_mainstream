# Trace Learning System

An adaptive, clinical and academic learning platform ported from the original Android application to a modern React 18 (Vite) + Express (Node.js) full-stack TypeScript architecture.

## System Architecture

- **Frontend**: React 18, Vite, TypeScript, Tailwind CSS, Lucide icons, Motion, Canvas-Confetti, React-Markdown.
- **Backend**: Express server with Vite middleware integration, TypeScript, Node.js 22.
- **AI Engine**: Gemini 2.5 Flash (`@google/genai`) with academic and clinical domain synthesis, Socratic diagnostic breakdown, and automated report generation.
- **Local Persistence**: `TraceStore` client-side local storage engine with versioned snapshots, backup and restore, and offline-first state synchronization.

## Core Capabilities

1. **Student Dashboard & Zenith Insights**:
   - Continuous trajectory analysis calculating composite PnL across active units.
   - Concentric curriculum mastery rings (Subtopic, Module, Topic).
   - Dynamic schedule teaser and rapid access to study trails.

2. **Learn Screen & Interactive Study Trails**:
   - Structured subtopic objectives with formatted Markdown and clinical pearls.
   - Inline Socratic consultant inquiries with instant pedagogical answers.
   - High-yield note bookmarking with saved takeaways.
   - Direct launch into Knowledge Retrieval quizzes.

3. **Knowledge Retrieval Engine (Quizzes)**:
   - High-yield multiple-choice clinical questions with immediate feedback.
   - Deep pathophysiological and clinical rationales explaining correct vs distractor choices.
   - Gemini-powered on-demand quiz generation for any unit and topic.
   - PnL scoring, timer, and historical performance tracking.

4. **Socratic Consultation Vault (Chat)**:
   - Multi-persona guidance (Socratic Tutor, Clinical Attending, Exam Drillmaster, Feynman Explainer).
   - Session history management and contextual inquiry continuation.
   - Integrated Pomodoro focus study timer.

5. **Dynamic Weekly Timetable**:
   - Spaced repetition algorithm with cognitive load balancing.
   - Day-of-week and session type filtering (Study, Assessment, Revision, Break).
   - Algorithmic allocation rationale briefs.

6. **Role-Based Portals**:
   - **Student View**: Complete self-directed study and assessment suite.
   - **Faculty / Educator Console**: Cohort progression matrix, at-risk student triage queue, and automated AI pedagogical report generation.
   - **Guardian Portal**: High-level academic standing, attendance metrics, and AI-synthesized progress summaries.

7. **System Utilities**:
   - Integrated reference browser with medical resource shortcuts.
   - Snapshot export, JSON import, and state backup engine.
   - Unit enrollment and archival management.
