
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

from fastapi import FastAPI, Request, Depends, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from app.db.session import engine, Base, get_db
from app.api import auth, users, ai, teacher, parent, learning
from app.core import security
from app.models import database_models as models
import ingestion_engine

templates = Jinja2Templates(directory=str(Path(__file__).resolve().parents[1] / "templates"))

# Safe database initialization on startup
try:
    Base.metadata.create_all(bind=engine)
    logger.info("Database schemas verified successfully.")
except Exception as e:
    logger.warning(f"Initial schema migration skipped or failed: {e}. Tables will initialize on request if needed.")

app = FastAPI(title="Trace Modular API", version="3.0.0")

INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "DEVELOPMENT_KEY")

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
    return templates.TemplateResponse("signup.html", {"request": request})

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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)


 