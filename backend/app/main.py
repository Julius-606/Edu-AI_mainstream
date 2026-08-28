"""
Main application module for the Trace Modular API backend.

This module initializes the FastAPI application, configures middleware (CORS, internal API key check),
connects database models, registers API routers (auth, users, ai, learning), and serves web templates
for ingestion and account management.
"""

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
from app.api import auth, users, ai, learning, sync
from app.core import security
from app.models import database_models as models
import ingestion_engine
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Template engine configuration for server-rendered HTML pages
templates = Jinja2Templates(directory=str(Path(__file__).resolve().parents[1] / "templates"))

# Create database tables defined in models
Base.metadata.create_all(bind=engine)

# Instantiate main FastAPI app
app = FastAPI(title="Trace Modular API", version="3.0.0")

INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "DEVELOPMENT_KEY")

# Global Security Middleware
@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    """
    Middleware to validate incoming HTTP requests against an internal API key.

    Requests to specific public endpoints (docs, login, signup, ingestion forms, etc.)
    bypass this key check. For all other endpoints, the header 'X-Internal-Api-Key'
    must match the configured INTERNAL_API_KEY environment variable.
    """
    # Paths that bypass API key verification
    bypass_paths = [
        "/", "/docs", "/openapi.json", "/favicon.ico", "/signup",
        "/api/auth/login", "/api/auth/signup-form",
        "/ingest", "/delete-unit", "/update-unit",
        "/api/units/library", "/api/sync"
    ]

    path = request.url.path
    if (
        path in bypass_paths
        or path.startswith("/delete-unit/")
        or path.startswith("/update-unit/")
        or path.startswith("/api/units/library/add/")
    ):
        return await call_next(request)

    x_api_key = request.headers.get("X-Internal-Api-Key")
    if x_api_key != INTERNAL_API_KEY:
        logger.warning(f"Unauthorized access attempt to {path} with key {x_api_key}")
        return JSONResponse(status_code=403, content={"detail": "Unauthorized: Invalid API Key"})

    return await call_next(request)

# Configure Cross-Origin Resource Sharing (CORS)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API Routers
app.include_router(auth.router, prefix="/api/auth")
app.include_router(users.router, prefix="/api")
app.include_router(ai.router, prefix="/api")
app.include_router(learning.router, prefix="/api")
app.include_router(sync.router, prefix="/api")


def find_user(user_id_or_name: str, db: Session):
    """
    Helper utility to look up a User database record by ID or username.

    :param user_id_or_name: String representing either the user ID (integer string) or username.
    :param db: SQLAlchemy Session object.
    :return: User model instance if found, else None.
    """
    user = None
    if str(user_id_or_name).isdigit():
        user = db.query(models.User).filter(models.User.id == int(user_id_or_name)).first()
    if not user:
        user = db.query(models.User).filter(models.User.username == str(user_id_or_name)).first()
    return user


@app.get("/", response_class=HTMLResponse)
def root(request: Request, db: Session = Depends(get_db)):
    """
    Renders the root ingestion dashboard interface.

    :param request: The incoming HTTP request.
    :param db: SQLAlchemy Session dependency.
    :return: HTMLResponse rendering 'ingestion.html' with current global units.
    """
    units = ingestion_engine.get_global_units(db)
    return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})


@app.post("/ingest", response_class=HTMLResponse)
async def handle_ingestion(request: Request, markdown: str = Form(...), db: Session = Depends(get_db)):
    """
    Processes markdown syllabus content for content ingestion and unit creation.

    :param request: The incoming HTTP request.
    :param markdown: Markdown formatted syllabus string submitted via form payload.
    :param db: SQLAlchemy Session dependency.
    :return: Updated ingestion HTML page or HTTP 500 HTML error page on failure.
    """
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
    """
    Deletes a specified global learning unit and re-renders the ingestion management UI.

    :param request: The incoming HTTP request.
    :param unit_id: Unique identifier of the unit to delete.
    :param db: SQLAlchemy Session dependency.
    :return: HTMLResponse rendering 'ingestion.html'.
    """
    ingestion_engine.delete_unit(db, unit_id)
    units = ingestion_engine.get_global_units(db)
    return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})


@app.post("/update-unit/{unit_id}", response_class=HTMLResponse)
async def handle_update_unit(request: Request, unit_id: int, name: str = Form(...), db: Session = Depends(get_db)):
    """
    Updates the title of a specified global learning unit.

    :param request: The incoming HTTP request.
    :param unit_id: Unique identifier of the unit to update.
    :param name: New name/title for the unit submitted via form.
    :param db: SQLAlchemy Session dependency.
    :return: HTMLResponse rendering 'ingestion.html'.
    """
    ingestion_engine.update_unit(db, unit_id, name)
    units = ingestion_engine.get_global_units(db)
    return templates.TemplateResponse("ingestion.html", {"request": request, "units": units})


@app.post("/api/units/library/add/{unit_id}")
async def add_unit_to_user(unit_id: int, user_id: str, db: Session = Depends(get_db)):
    """
    Clones a global template unit into a specific user's personalized unit library.

    :param unit_id: ID of the global unit to clone.
    :param user_id: ID or username of the user receiving the unit.
    :param db: SQLAlchemy Session dependency.
    :return: JSON object containing operation status and new unit ID, or JSON error message.
    """
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
    """
    Retrieves all available global template units for the client application unit library.

    :param db: SQLAlchemy Session dependency.
    :return: List of dictionaries containing unit metadata (id, name, category).
    """
    units = ingestion_engine.get_global_units(db)
    return [{"id": u.id, "name": u.name, "category": u.category} for u in units]


@app.get("/signup", response_class=HTMLResponse)
async def signup_page(request: Request):
    """
    Renders the web browser signup form page.

    :param request: The incoming HTTP request.
    :return: HTMLResponse rendering 'signup.html'.
    """
    return templates.TemplateResponse("signup.html", {"request": request})


@app.post("/signup", response_class=HTMLResponse)
async def handle_browser_signup(
    username: str = Form(...),
    email: str = Form(...),
    password: str = Form(...),
    role: str = Form(...),
    db: Session = Depends(get_db),
):
    """
    Handles browser form submissions to register a new user account.

    :param username: Requested username from form data.
    :param email: Registered user email address from form data.
    :param password: Raw user password to be hashed prior to DB persistence.
    :param role: User role designation (e.g., student, instructor).
    :param db: SQLAlchemy Session dependency.
    :return: HTMLResponse with success message or 400 error message if credentials already exist.
    """
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
