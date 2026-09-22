# 🌐 Trace Learning System - Synchronized Multi-System Architecture

> **Version:** 3.1.0 (Modular Multi-System Architecture)  
> **Status:** Active & Synchronized  
> **Target Platforms:** Android (Kotlin/Compose), Cloud Backend (Python/FastAPI/Hugging Face Spaces), Neon DB (PostgreSQL Serverless), Desktop (Windows & Linux), Web Client (React/Vite).

---

## 🏛️ The 7 Core Pillars of Trace

```
                                  [ NEON DB ]
                       (Serverless PostgreSQL + SSL)
                                     ▲
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           ▼                         ▼                         ▼
   [ PYTHON BACKEND ]       [ NODE/EXPRESS GW ]       [ ANDROID LOCAL DB ]
  (FastAPI on Hugging Face) (Port 3000 Dev Preview)      (Room KSP SQLite)
           ▲                         ▲                         ▲
           │                         │                         │
     ┌─────┴───────┐           ┌─────┴───────┐                 │
     ▼             ▼           ▼             ▼                 │
[ DESKTOP WIN ] [ DESKTOP LINUX ] [ WEB APP ] [ ANDROID COMPOSE APP ]
(Bat/Exe Run)   (Bash/AppImg)   (React 18)   (Android Studio Ready)
```

### 1. The Interactive Preview & Gateway (`server.ts` & `src/`)
- **Port:** 3000 (`0.0.0.0`)
- **Stack:** Express gateway + Vite SPA middleware + React 18 + Tailwind CSS.
- **Function:** Serves the active web app in real-time within the container, proxies AI queries with fallback resiliency, and mirrors student, teacher, and parent consoles.

### 2. The Modular Python Backend (`backend/`)
- **Stack:** Python 3.10+, FastAPI, SQLAlchemy 2.0+, Pydantic v2.
- **Structure:**
  - `backend/app/main.py`: Modular FastAPI application
  - `backend/app/api/`: Domain routers (`ai.py`, `auth.py`, `learning.py`, `parent.py`, `teacher.py`, `users.py`)
  - `backend/app/services/`: AI inference & clinical logic (`ai_service.py`)
  - `backend/app/db/`: Session management & Neon DB pooling (`session.py`)
  - `backend/app/models/`: Declarative relational models (`database_models.py`)
  - `backend/app/schemas/`: Typed request/response contracts (`api_schemas.py`)
  - `backend/app/core/`: Security and JWT authentication (`security.py`)
- **Deployment:** Deployable directly to **Hugging Face Spaces** (`Agent606/Edu-AI`) via Dockerfile or `git push`.

### 3. Neon DB (PostgreSQL Serverless)
- **Engine:** PostgreSQL 16+ on Neon.tech with serverless scale-to-zero capabilities.
- **Resilience:** Auto-injection of `sslmode=require`, `pool_pre_ping=True`, and `pool_recycle=300` to prevent stale connection drops on serverless restarts.
- **Config:** Accepts `NEON_DATABASE_URL` or `DATABASE_URL` with automatic URI normalization.

### 4. The Android Gradle App (`app/` & root Gradle)
- **IDE:** Android Studio (Hedgehog, Iguana, Jellyfish, Koala+).
- **Compile SDK:** 35 | **Min SDK:** 24.
- **Stack:** Jetpack Compose (BOM 2024.12.01), Room DB (KSP), Retrofit 2.11, Kotlin Coroutines, Material3.
- **Version Catalog:** `gradle/libs.versions.toml` with Gradle 8.9 wrapper.

### 5. Computer Desktop Suite (`desktop/`)
- **Platforms:** Windows 10/11 (`run_windows.bat`, PyInstaller `.exe`), Linux Ubuntu/Debian/Fedora (`run_linux.sh`, AppImage).
- **Modes:** Native desktop window via `pywebview` or cross-platform Electron runner (`desktop/electron_main.cjs`).

### 6. Web-Based Interface (`src/`)
- **Features:**
  - Student Dashboard (Zenith AI trajectory, unit trail, timetable, consultation vault)
  - Teacher Dashboard (student assessment rubrics, clinical progress report generation)
  - Parent Dashboard (weekly engagement index, milestone reports)
  - Offline-first cache (CAS snapshot restoration, JSON trail exports)

### 7. Documentation & Hugging Face CI/CD (`.github/workflows/` & `docs/`)
- Automated sync pipeline (`.github/workflows/hf-sync.yml`) pushing changes from `backend/` to Hugging Face Spaces.
- Comprehensive guides in `docs/` and `ARCHITECTURE.md`.

---

## 🔄 Synchronization Protocol

Whenever code or schemas are updated:
1. **Model Changes:** Update `backend/app/models/database_models.py`, `backend/models.py`, Android Room entities in `app/src/main/java/com/example/edu_ai/data/local/`, and TypeScript types in `src/types.ts`.
2. **API Changes:** Maintain contract parity between FastAPI endpoints in `backend/app/api/`, Android Retrofit interfaces in `EduAIApi.kt`, and the React client services in `src/lib/store.ts`.
3. **Repository Integrity:** Never prune or exclude sub-systems (`app/`, `backend/`, `desktop/`, `docs/`) during Git pushes or branch merges.
