# 📖 Trace Learning System - Comprehensive System Guide

## Quick Start Guide

### 1. Running the Web Preview Locally
```bash
npm run dev
```
Listens on `http://localhost:3000`. The Vite development server and Express API gateway run together.

### 2. Running the Python Modular Backend
```bash
cd backend
python -m pip install -r requirements.txt
python run_modular.py
```
Starts the FastAPI application on `http://localhost:7860` with interactive Swagger docs at `http://localhost:7860/docs`.

### 3. Deploying to Hugging Face Spaces
Hugging Face Spaces runs as a Docker space or Python FastAPI space:
1. Ensure your Hugging Face Space is created (e.g., `Agent606/Edu-AI`).
2. Add your repository secret `HF_TOKEN` in GitHub Secrets.
3. Every commit to `main` touching `backend/` automatically triggers `.github/workflows/hf-sync.yml`, deploying the updated backend directly to Hugging Face Spaces!
4. Alternatively, push manually:
```bash
cd backend
git init
git remote add space https://huggingface.co/spaces/Agent606/Edu-AI
git add .
git commit -m "Deploy modular backend v3.1"
git push -f space main
```

### 4. Running the Desktop Client
#### On Windows:
Double-click `desktop/run_windows.bat` or run:
```cmd
cd desktop
run_windows.bat
```

#### On Linux:
```bash
cd desktop
chmod +x run_linux.sh
./run_linux.sh
```

---

## 🗄️ Database Architecture (Neon + Local SQLite + Android Room)

| Layer | Technology | Primary Location | Use Case |
|---|---|---|---|
| **Cloud Central** | Neon Serverless PostgreSQL | `NEON_DATABASE_URL` | User credentials, cross-device sync, analytics |
| **Backend Local** | SQLite | `backend/edu_ai_vault.db` | Local offline backend testing |
| **Android Mobile** | Android Room (SQLite) | `app/.../EduAIDatabase.kt` | Offline-first mobile device storage |
| **Web Client** | IndexedDB / LocalStorage | `src/lib/store.ts` | Immediate UI response, trail caching |

---

## 🛡️ Synchronization Checklist

When adding a new feature (e.g., a new assessment metric or note field):
1. **Python Model**: Add column to `backend/app/models/database_models.py` and `backend/models.py`.
2. **Pydantic Schema**: Add field to `backend/app/schemas/api_schemas.py` and `backend/schemas.py`.
3. **Android Room Entity**: Add `@ColumnInfo` to entity in `app/src/main/java/com/example/edu_ai/data/local/`.
4. **Android Retrofit Model**: Update DTO in `app/src/main/java/com/example/edu_ai/data/remote/ApiModels.kt`.
5. **Web TypeScript Type**: Update interface in `src/types.ts`.
6. **Web Store**: Persist in `src/lib/store.ts`.
