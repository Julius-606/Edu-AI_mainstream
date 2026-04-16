---
title: Edu-AI Backend
emoji: 🎓
colorFrom: blue
colorTo: indigo
sdk: docker
app_file: main.py
pinned: false
---

Check out the configuration reference at https://huggingface.co/docs/hub/spaces-config-reference

# 🎓 Edu-AI Backend

Welcome to the **Edu-AI Prop Firm Backend**, a robust and scalable API powered by **FastAPI** and **Google Gemini AI**. This backend serves as the core intelligence engine for the Edu-AI mobile application, handling everything from AI-driven mentorship to dynamic quiz generation.

## 🚀 Live on Hugging Face
This backend is hosted on **Hugging Face Spaces** using Docker, providing a global endpoint for the mobile app while maintaining a stateful connection to a cloud database.

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

---

## 🛠️ Environment Variables
To run this project, you must configure the following secrets/environment variables in your Space settings:

| Variable | Description |
| :--- | :--- |
| `DATABASE_URL` | Neon.tech PostgreSQL connection string. |
| `GEMINI_API_KEY_1` | Primary Google Gemini API Key. |
| `GEMINI_API_KEY_2` | Secondary Key (for rotation/failover). |
| `GEMINI_API_KEY_N` | Additional keys as needed. |

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
   uvicorn main:app --reload
   ```

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

**Built with ❤️ for the next generation of learners.**
