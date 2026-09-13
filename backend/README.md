
---
title: Trace Backend
emoji: 🎓
colorFrom: blue
colorTo: indigo
sdk: docker
app_file: main.py
pinned: false
---

Check out the configuration reference at https://huggingface.co/docs/hub/spaces-config-reference

# 🎓 Trace Backend

Welcome to the **Trace Learning Backend**, a robust and scalable API powered by **FastAPI** and **Google Gemini AI**. This backend serves as the core intelligence engine for the Trace mobile application, handling everything from AI-driven mentorship to dynamic quiz generation.

## 🚀 Live on Hugging Face
This backend is designed to be hosted on **Hugging Face Spaces** using Docker, providing a global endpoint for the mobile app while maintaining a stateful connection to a cloud database.

---

## ⚙️ Tech Stack
- **Framework:** FastAPI (Python 3.10+)
- **AI Engine:** Google Gemini (Generative AI) with intelligent key rotation.
- **Database:** PostgreSQL (Cloud-hosted via **Neon.tech**) with local SQLite fallback.
- **ORM:** SQLAlchemy 2.0
- **Deployment:** Docker on Hugging Face Spaces.

---

## ✨ Key Features
- **AI Socratic Mentor:** Context-aware chat system that adopts specific educational personas.
- **Dynamic Quiz Engine:** Generates personalized multiple-choice quizzes based on the student's level and active units.
- **Performance Analytics:** Tracks student "PnL" (Performance & Learning) and provides strategic recommendations.
- **Unit Management:** Allows students to organize their curriculum and focus areas.
- **Fault Tolerance:** Built-in Gemini API key rotation to handle rate limits and quotas automatically.
- **Admin Dashboard:** Browser-based admin login at `/admin` with separate ingestion and curriculum catalogue views.
- **Curriculum Editing:** Inspect a complete unit hierarchy and add, edit, or remove modules, topics, subtopics, and learning objectives.
- **User and Release Management:** Review user data, update user preferences, remove user records, and maintain a versioned archive for mobile and future artifacts.
- **Database Browser:** Inspect the backend tables from the admin sidebar in a read-only view; sensitive password hashes are masked.

---

## 🛠️ Environment Variables
To run this project, you must configure the following secrets/environment variables in your Space settings:

| Variable | Description |
| :--- | :--- |
| `DATABASE_URL` | Neon.tech PostgreSQL connection string. |
| `INTERNAL_API_KEY` | Shared backend/app API key sent as `X-Internal-Api-Key`. Must match the Android build property. |
| `JWT_SECRET_KEY` | Secret used to sign login tokens. Keep this stable across deploys or existing sessions will be logged out. |
| `GEMINI_API_KEY_1` | Primary Google Gemini API Key. |
| `GEMINI_API_KEY_2` | Secondary Key (for rotation/failover). |
| `GEMINI_API_KEY_N` | Additional keys as needed. |
| `ADMIN_EMAIL` | Email used for the browser-based backend admin login. |
| `ADMIN_PASSWORD` | Password used for the browser-based backend admin login. |

---

## 🏗️ Local Setup

1. **Clone the repository:**
   ```bash
   git clone <your-repo-url>
   cd backend
   ```

2. **Create a Virtual Environment:**
   ```bash
   python -m venv venv
   source venv/bin/activate  # Windows: venv\Scripts\activate
   ```

3. **Install Dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

4. **Run the Server:**
   ```bash
   uvicorn app.main:app --reload
   ```

The backend management dashboard is available at `/admin` (or the root URL).
It requires `ADMIN_EMAIL` and `ADMIN_PASSWORD` to be configured.

The dashboard sidebar separates ingestion from the Curriculum Catalogue. Existing units are edited from the
catalogue, and unit trees remain collapsed until explicitly opened. Destructive actions are grouped under
explicit danger-zone disclosures.

### Template organization

Browser templates are grouped by feature under `templates/admin/`, `templates/curriculum/`,
`templates/users/`, and `templates/public/`. Add future pages to the matching feature folder and reference
them with that relative path in `Jinja2Templates`.

### Protected database editing

The admin Database browser exposes editable model columns while keeping primary keys and password hashes
protected. A row update is accepted only when the submitted original snapshot still matches the database
(optimistic conflict check), and the admin must enter `ADMIN_PASSWORD` again immediately before commit.

The release archive is available to authenticated clients at `GET /api/releases/archive`, with artifact downloads at
`GET /api/releases/archive/{file_name}`. Files placed in the repository-level `Archives/` folder are discovered
automatically and can be annotated from the admin Archives page.

---

## 📊 Database Migration
To sync your local SQLite data to the cloud (Neon.tech):
```bash
python migrate_data.py
```

---

## 📡 API Endpoints (Snapshot)
- `GET /`: Health check.
- `GET /api/user/{user_id}/dashboard`: Fetch comprehensive student stats.
- `POST /api/ai/chat`: Interact with the AI Mentor.
- `POST /api/ai/quiz`: Generate a new assessment.
- `POST /api/quiz/record`: Save quiz performance to history.

---

## 🛡️ Robots & Security
- `GET /robots.txt`: Configured to prevent unauthorized crawling of API endpoints.
- **CORS:** Pre-configured for cross-origin requests from the Trace mobile app.

---

**Built with ❤️ for the next generation of learners.**
