
import os
import time
import logging
from pathlib import Path
from dotenv import load_dotenv

# Configure basic logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("trace_backend")

# Load .env BEFORE any other app imports to ensure environment variables are available
load_dotenv(Path(__file__).resolve().parents[1] / ".env")

from fastapi import FastAPI, Request, Depends, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from app.db.session import engine, Base, get_db
from app.api import auth, users, ai, teacher, parent, learning
from app.core import security
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from typing import List
import ingestion_engine

templates = Jinja2Templates(directory=str(Path(__file__).resolve().parents[1] / "templates"))

# Safe database initialization on startup
try:
    Base.metadata.create_all(bind=engine)
    logger.info("Database schemas verified successfully.")
except Exception as e:
    logger.warning(f"Initial schema migration skipped or failed: {e}. Tables will initialize on request if needed.")

app = FastAPI(title="Trace Modular API", version="3.0.0")

class HFSpacePrefixMiddleware:
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] == "http":
            path = scope.get("path", "")
            prefix = "/spaces/Agent606/Edu-AI"
            if path.startswith(prefix):
                scope["path"] = path[len(prefix):] or "/"
                if "raw_path" in scope:
                    raw_path = scope["raw_path"].decode("ascii", "ignore")
                    if raw_path.startswith(prefix):
                        scope["raw_path"] = raw_path[len(prefix):].encode("ascii")
        await self.app(scope, receive, send)

app.add_middleware(HFSpacePrefixMiddleware)

INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "64923e4d8f1a2c5b9e0f3d7a6c5b9eX0f3d7a6c5b9e0f3d7a")

# Global Security Middleware
@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    # Paths that bypass API key (root, health, docs, web signup, syllabus ingestion)
    if (
        request.url.path in ["/", "/health", "/docs", "/openapi.json", "/favicon.ico", "/signup", "/api/auth/login", "/api/auth/signup-form", "/ingest", "/delete-unit", "/update-unit"]
        or request.url.path.startswith("/delete-unit/")
        or request.url.path.startswith("/update-unit/")
    ):
        return await call_next(request)

    x_api_key = request.headers.get("X-Internal-Api-Key")
    if x_api_key != INTERNAL_API_KEY:
        return JSONResponse(status_code=403, content={"detail": "Unauthorized: Invalid API Key"})

    return await call_next(request)


# Real-Time Request and Error Logger Middleware
@app.middleware("http")
async def log_requests_middleware(request: Request, call_next):
    start_time = time.time()
    try:
        response = await call_next(request)
        process_time_ms = round((time.time() - start_time) * 1000, 2)
        status_code = response.status_code
        log_msg = f"[BACKEND API] {request.method} {request.url.path} -> {status_code} ({process_time_ms}ms)"
        if status_code >= 500:
            logger.error(log_msg)
        elif status_code >= 400:
            logger.warning(log_msg)
        else:
            logger.info(log_msg)
        return response
    except Exception as exc:
        process_time_ms = round((time.time() - start_time) * 1000, 2)
        logger.error(f"[BACKEND ERROR] {request.method} {request.url.path} -> 500 ({process_time_ms}ms): {exc}")
        raise exc

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include Routers
app.include_router(auth.router, prefix="/api/auth")
app.include_router(users.router, prefix="/api")
app.include_router(ai.router, prefix="/api")
app.include_router(teacher.router, prefix="/api")
app.include_router(parent.router, prefix="/api")
app.include_router(learning.router, prefix="/api")

@app.get("/")
def root(request: Request, db: Session = Depends(get_db)):
    # Support Hugging Face container health and logs probing
    if request.query_params.get("logs") == "container":
        return JSONResponse({
            "status": "healthy",
            "service": "Trace FastAPI Backend",
            "container": "active",
            "timestamp": time.time()
        })

    try:
        units = ingestion_engine.get_global_units(db)
        total_users = db.query(models.User).count()
        total_subtopics = db.query(models.Subtopic).count()
        total_quizzes = db.query(models.QuizHistory).count()
        quizzes = db.query(models.QuizHistory).all()
        avg_pnl = f"{round(sum([q.pnl for q in quizzes if q.pnl is not None] or [82.4]) / max(len(quizzes), 1), 1)}%"
        stats = {
            "total_users": total_users,
            "total_subtopics": total_subtopics,
            "total_quizzes": total_quizzes,
            "avg_pnl": avg_pnl
        }
        return templates.TemplateResponse("ingestion.html", {"request": request, "units": units, "stats": stats})
    except Exception as e:
        logger.warning(f"Root endpoint template fallback triggered: {e}")
        return HTMLResponse(
            f"""
            <html>
                <head><title>Trace Backend API</title></head>
                <body style="font-family:sans-serif; background:#0f172a; color:#f8fafc; padding:40px;">
                    <h1 style="color:#6366f1;">Trace Modular Learning API is Running</h1>
                    <p>Status: <strong>Online</strong></p>
                    <p><a href="/docs" style="color:#38bdf8;">Interactive Swagger API Docs &rarr;</a></p>
                </body>
            </html>
            """
        )

@app.post("/ingest", response_class=HTMLResponse)
async def handle_ingestion(request: Request, markdown: str = Form(...), db: Session = Depends(get_db)):
    syllabus_data = ingestion_engine.parse_syllabus_markdown(markdown)
    ingestion_engine.save_syllabus_to_db(db, syllabus_data)
    units = ingestion_engine.get_global_units(db)
    return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})

@app.post("/delete-unit/{unit_id}", response_class=HTMLResponse)
async def handle_delete_unit(request: Request, unit_id: int, db: Session = Depends(get_db)):
    ingestion_engine.delete_unit(db, unit_id)
    units = ingestion_engine.get_global_units(db)
    return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})

@app.post("/update-unit/{unit_id}", response_class=HTMLResponse)
async def handle_update_unit(request: Request, unit_id: int, name: str = Form(...), db: Session = Depends(get_db)):
    ingestion_engine.update_unit(db, unit_id, name)
    units = ingestion_engine.get_global_units(db)
    return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})

@app.post("/api/units/library/add/{unit_id}")
async def add_unit_to_user(unit_id: int, user_id: int, db: Session = Depends(get_db)):
    new_unit = ingestion_engine.clone_unit_to_user(db, unit_id, user_id)
    if new_unit:
        return {"status": "success", "unit_id": new_unit.id}
    return JSONResponse(status_code=404, content={"detail": "Unit not found"})

@app.get("/api/units/library")
async def get_library_units(db: Session = Depends(get_db)):
    units = ingestion_engine.get_global_units(db)
    return [{"id": u.id, "name": u.name, "category": u.category} for u in units]

@app.get("/signup", response_class=HTMLResponse)
async def signup_page(request: Request):
    return templates.TemplateResponse("public/signup.html", {"request": request})

@app.post("/signup", response_class=HTMLResponse)
async def handle_browser_signup(
    username: str = Form(...),
    email: str = Form(...),
    password: str = Form(...),
    role: str = Form(...),
    db: Session = Depends(get_db),
):
    existing_user = db.query(models.User).filter((models.User.email == email) | (models.User.username == username)).first()
    if existing_user:
        return HTMLResponse("<h2>Error: Email or Username already exists. Please go back.</h2>", status_code=400)

    new_user = models.User(
        username=username,
        email=email,
        hashed_password=security.get_password_hash(password),
        role=role,
    )
    db.add(new_user)
    db.commit()

    return """
    <body style="font-family: sans-serif; text-align: center; padding-top: 100px;">
        <h1 style="color: #059669;">Account Created Successfully!</h1>
        <p>You can now return to the Edu-AI app and log in.</p>
    </body>
    """

# Helper to find user by ID (int) or Username (string)
def find_user(user_id_or_name: str, db: Session):
    user = None
    if str(user_id_or_name).isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    if not user:
        user = db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()
    return user

@app.get("/api/user/{user_id}/bookmarks", response_model=List[schemas.BookmarkResponse])
def get_bookmarks(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return db.query(models.Bookmark).filter(models.Bookmark.owner_id == user.id).all()

@app.post("/api/user/{user_id}/bookmarks", response_model=schemas.BookmarkResponse)
def create_bookmark(user_id: str, bookmark: schemas.BookmarkCreate, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    new_bookmark = models.Bookmark(
        type=bookmark.type,
        title=bookmark.title,
        target=bookmark.target,
        context=bookmark.context,
        timestamp=bookmark.timestamp,
        owner_id=user.id
    )
    db.add(new_bookmark)
    db.commit()
    db.refresh(new_bookmark)
    return new_bookmark

@app.delete("/api/user/{user_id}/bookmarks/{bookmark_id}")
def delete_bookmark(user_id: str, bookmark_id: int, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    bk = db.query(models.Bookmark).filter(models.Bookmark.id == bookmark_id, models.Bookmark.owner_id == user.id).first()
    if not bk:
        raise HTTPException(status_code=404, detail="Bookmark not found")
    
    db.delete(bk)
    db.commit()
    return {"status": "success", "message": "Bookmark deleted"}

@app.get("/api/user/{user_id}/sync")
def get_user_sync_data(user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # 1. Fetch Syllabus Progress
    progress = db.query(models.UserSyllabusProgress).filter(models.UserSyllabusProgress.user_id == user.id).all()
    progress_list = []
    for p in progress:
        progress_list.append({
            "node_id": p.node_id,
            "node_type": p.node_type,
            "status": p.status,
            "last_studied_at": p.last_studied_at
        })

    # 2. Fetch Bookmarks
    bookmarks = db.query(models.Bookmark).filter(models.Bookmark.owner_id == user.id).all()
    bookmarks_list = []
    for b in bookmarks:
        bookmarks_list.append({
            "id": b.id,
            "type": b.type,
            "title": b.title,
            "target": b.target,
            "context": b.context,
            "timestamp": b.timestamp
        })

    # 3. Fetch Quiz History
    quizzes = db.query(models.QuizHistory).filter(models.QuizHistory.owner_id == user.id).all()
    quizzes_list = []
    for q in quizzes:
        quizzes_list.append({
            "id": q.id,
            "unit_name": q.unit_name,
            "score": q.score,
            "total": q.total,
            "pnl": q.pnl,
            "timestamp": q.timestamp
        })

    # 4. Fetch Chat sessions & messages
    sessions = db.query(models.ChatSession).filter(models.ChatSession.owner_id == user.id).all()
    sessions_list = []
    for s in sessions:
        msgs = db.query(models.ChatMessage).filter(models.ChatMessage.session_id == s.id).order_by(models.ChatMessage.id.asc()).all()
        msgs_list = []
        for m in msgs:
            msgs_list.append({
                "id": m.id,
                "role": m.role,
                "content": m.content,
                "timestamp": m.timestamp
            })
        sessions_list.append({
            "id": s.id,
            "title": s.title,
            "description": s.description,
            "timestamp": s.timestamp,
            "is_archived": s.is_archived,
            "messages": msgs_list
        })

    return {
        "progress": progress_list,
        "bookmarks": bookmarks_list,
        "quizzes": quizzes_list,
        "chats": sessions_list,
        "user": {
            "id": str(user.id),
            "username": user.username,
            "email": user.email,
            "role": user.role,
            "difficulty": user.difficulty,
            "semesterStatus": user.semester_status,
            "aiPersona": user.ai_persona,
            "sensoryMode": user.sensory_mode,
            "activeUnits": [u.name for u in user.units if u.is_active]
        }
    }

@app.post("/api/user/{user_id}/sync", response_model=schemas.SyncResponse)
def sync_user_data(user_id: str, payload: schemas.SyncRequest, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # 1. Sync Syllabus Progress
    for p_item in payload.progress:
        existing = db.query(models.UserSyllabusProgress).filter(
            models.UserSyllabusProgress.user_id == user.id,
            models.UserSyllabusProgress.node_id == p_item.node_id,
            models.UserSyllabusProgress.node_type == p_item.node_type
        ).first()
        if existing:
            existing.status = p_item.status
            existing.last_studied_at = p_item.last_studied_at
        else:
            db.add(models.UserSyllabusProgress(
                user_id=user.id,
                node_id=p_item.node_id,
                node_type=p_item.node_type,
                status=p_item.status,
                last_studied_at=p_item.last_studied_at
            ))
    
    # 2. Sync Bookmarks
    for b_item in payload.bookmarks:
        existing_b = db.query(models.Bookmark).filter(
            models.Bookmark.owner_id == user.id,
            models.Bookmark.target == b_item.target,
            models.Bookmark.type == b_item.type
        ).first()
        if not existing_b:
            db.add(models.Bookmark(
                type=b_item.type,
                title=b_item.title,
                target=b_item.target,
                context=b_item.context,
                timestamp=b_item.timestamp,
                owner_id=user.id
            ))
            
    db.commit()
    return schemas.SyncResponse(success=True, message="Data synced successfully")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)


 