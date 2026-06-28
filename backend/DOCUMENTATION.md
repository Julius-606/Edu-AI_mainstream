# 📘 Trace Learning System - Comprehensive Documentation

Welcome to the **Trace Learning System**, a next-generation AI-driven educational platform. This document provides an exhaustive overview of every functionality, module, and feature within the project.

---

## 🏗️ Architecture Overview
The system follows a **Modular Monolith** architecture, organized for scalability, maintainability, and clarity.

### Directory Structure
- **`app/`**: The core application package.
  - **`api/`**: API Route controllers (Endpoints).
  - **`core/`**: Security, configuration, and global settings.
  - **`db/`**: Database session management and connectivity.
  - **`models/`**: SQLAlchemy database entities.
  - **`schemas/`**: Pydantic models for data validation and serialization.
  - **`services/`**: Business logic and AI integration services.
- **`templates/`**: HTML templates for web-based views (e.g., Signup).
- **`requirements.txt`**: Project dependencies.
- **`Dockerfile`**: Containerization instructions for deployment.

---

## 🛠️ Core Functionalities

### 1. 🔐 Authentication & Security
- **JWT Authentication**: Secure stateless authentication using JSON Web Tokens.
- **Password Hashing**: Industry-standard encryption using `bcrypt`.
- **API Key Protection**: A global middleware (`X-Internal-Api-Key`) ensures that only authorized clients (like the mobile app) can access the API.
- **Modular Security**: Centralized logic in `app/core/security.py`.

### 2. 🧠 AI Intelligence Service (`AiService`)
Powered by **Google Gemini AI**, this is the "brain" of the system.
- **Multi-Model Rotation**: Automatically switches between `gemini-2.0-flash`, `gemini-2.5-flash`, and `gemini-1.5-flash` to ensure 100% uptime.
- **Key Rotary System**: Rotates through multiple API keys to bypass rate limits (429 errors).
- **Dynamic Persona Support**: The AI adapts its tone and clinical depth based on the student's chosen `ai_persona` (e.g., Socratic Tutor, Clinical Consultant).

### 3. 📝 Assessment & Quizzing
- **AI Quiz Generation**: Generates rigorous, academic multiple-choice assessments tailored to specific units and difficulty levels.
- **Clinical Rationales**: Every question includes a deep physiological explanation for the correct answer and a differential analysis for distractors.
- **Performance Tracking**: Records scores and calculates "PNL" (Percentage Next Level) to monitor student progress.

### 4. 📅 Adaptive Learning & Planning
- **AI Timetable Generation**: Analyzes quiz history and recent chat consultations to generate a dynamic weekly study plan.
- **Continuity Logic**: The AI remembers previous plans to avoid repetition and ensure spaced repetition for weak areas.
- **Active Unit Management**: Students can swap and track specific academic units.

### 5. 👨‍🏫 Teacher & Parent Portals
- **Teacher Dashboard**: Provides an "Action Required" queue identifying "At-Risk" students based on performance drops.
- **Parent Dashboard**: Offers a simplified view of academic status, teacher remarks, and AI-generated progress reviews.
- **Progress Reporting**: Teachers can trigger AI-generated progress reports that translate technical metrics into encouraging feedback for parents.

### 6. 📚 Structured Learning (Syllabus Management)
- **Syllabus Upload**: Allows teachers to upload hierarchical course structures (Units -> Modules -> Subtopics).
- **Progress Tracking**: Detailed tracking at the subtopic level (Mark as Completed).
- **Syllabus Tree View**: Fetches the entire visual hierarchy of a course.

### 7. 💬 Intelligent Consultations (Chat)
- **Contextual Memory**: The chat maintains history within a session to provide relevant follow-up answers.
- **Socratic Tutoring**: Designed to guide students toward answers rather than simply providing them, fostering deep learning.

---

## 🚀 Deployment & Local Testing

### Local Execution
To run the server locally:
```bash
python -m app.main
```

### Mock App Testing
The `mock_app_test.py` script simulates a full mobile app lifecycle, testing every major endpoint from authentication to AI generation.

### Dockerization
The project is containerized for seamless deployment on **Hugging Face Spaces** or any cloud provider. The `Dockerfile` handles:
- Environment setup (Python 3.14).
- Dependency installation.
- Database initialization.
- Uvicorn server execution.

---

## 📈 Versioning
- **Backend Version**: 3.0.0 (Modular Stability Upgrade)
- **API Version**: v1beta / v3.0.0
