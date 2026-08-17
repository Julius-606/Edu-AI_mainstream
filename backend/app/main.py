import os
from pathlib import Path
from dotenv import load_dotenv

# Load .env BEFORE any other app imports to ensure environment variables are available
load_dotenv(Path(__file__).resolve().parents[1] / ".env")

from fastapi import FastAPI, Request, Depends, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from app.db.session import engine, Base, get_db
from app.api import auth, users, ai, learning
from app.core import security
from app.models import database_models as models
import ingestion_engine
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

templates = Jinja2Templates(directory=str(Path(__file__).resolve().parents[1] / "templates"))

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Trace Modular API", version="3.0.0")

INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "DEVELOPMENT_KEY")

# Global Security Middleware
@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    # Paths that bypass API key
    bypass_paths = [
        "/", "/docs", "/openapi.json", "/favicon.ico", "/signup",
        "/api/auth/login", "/api/auth/signup-form",
        "/ingest", "/delete-unit", "/update-unit",
        "/api/units/library"
    ]

    path = request.url.path
    if path in bypass_paths or path.startswith("/delete-unit/") or path.startswith("/update-unit/") or path.startswith("/api/units/library/add/"):
        return await call_next(request)

    x_api_key = request.headers.get("X-Internal-Api-Key")
    if x_api_key != INTERNAL_API_KEY:
        logger.warning(f"Unauthorized access attempt to {path} with key {x_api_key}")
        return JSONResponse(status_code=403, content={"detail": "Unauthorized: Invalid API Key"})

    return await call_next(request)

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
app.include_router(learning.router, prefix="/api")

# Helper to find user (same as in users.py)
def find_user(user_id_or_name: str, db: Session):
    user = None
    if str(user_id_or_name).isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    if not user:
        user = db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()
    return user

@app.get("/", response_class=HTMLResponse)
def root(request: Request, db: Session = Depends(get_db)):
    units = ingestion_engine.get_global_units(db)
    return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})

@app.post("/ingest", response_class=HTMLResponse)
async def handle_ingestion(request: Request, markdown: str = Form(...), db: Session = Depends(get_db)):
    try:
        syllabus_data = ingestion_engine.parse_syllabus_markdown(markdown)
        ingestion_engine.save_syllabus_to_db(db, syllabus_data)
        units = ingestion_engine.get_global_units(db)
        return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})
    except Exception as e:
        logger.error(f"Ingestion error: {e}")
        return HTMLResponse(content=f"<h1>Internal Server Error</h1><p>{str(e)}</p>", status_code=500)

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
async def add_unit_to_user(unit_id: int, user_id: str, db: Session = Depends(get_db)):
    user = find_user(user_id, db)
    if not user:
        return JSONResponse(status_code=404, content={"detail": f"User {user_id} not found"})

    try:
        new_unit = ingestion_engine.clone_unit_to_user(db, unit_id, user.id)
        if new_unit:
            return {"status": "success", "unit_id": new_unit.id}
        return JSONResponse(status_code=404, content={"detail": "Unit not found"})
    except Exception as e:
        logger.error(f"Error adding unit to user: {e}")
        return JSONResponse(status_code=500, content={"detail": str(e)})

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
