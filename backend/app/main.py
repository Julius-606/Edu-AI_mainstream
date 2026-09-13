"""
Main application module for the Trace Modular API backend.

This module initializes the FastAPI application, configures middleware (CORS, internal API key check),
connects database models, registers API routers (auth, users, ai, learning), and serves web templates
for ingestion and account management.
"""

import os
import time
import json
import hmac
import base64
import urllib.request
import urllib.error
from datetime import datetime
from pathlib import Path
from app.core.config import load_runtime_environment

# Local development reads backend/.env; Space secrets remain the source of truth
# when Hugging Face deployment markers are present.
load_runtime_environment()

from fastapi import FastAPI, Request, Depends, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, HTMLResponse, FileResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from sqlalchemy import inspect, text
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
ARCHIVES_DIR = Path(__file__).resolve().parents[2] / "Archives"
ARCHIVE_MANIFEST = ARCHIVES_DIR / "archives.json"

def read_archive_manifest():
    if not ARCHIVE_MANIFEST.exists():
        return {"releases": [], "assets": {}}
    with ARCHIVE_MANIFEST.open("r", encoding="utf-8") as manifest_file:
        data = json.load(manifest_file)
    return {
        "releases": data.get("releases", []),
        "assets": data.get("assets", {}),
    }

def write_archive_manifest(manifest):
    ARCHIVES_DIR.mkdir(parents=True, exist_ok=True)
    with ARCHIVE_MANIFEST.open("w", encoding="utf-8") as manifest_file:
        json.dump(manifest, manifest_file, indent=2)
        manifest_file.write("\n")
    push_archive_manifest()

def push_archive_manifest():
    token = os.getenv("GITHUB_TOKEN")
    repository = os.getenv("GITHUB_REPOSITORY")
    if not token or not repository:
        logger.info("Archives manifest saved locally; GitHub push is not configured.")
        return
    branch = os.getenv("GITHUB_ARCHIVES_BRANCH", "main")
    api_url = f"https://api.github.com/repos/{repository}/contents/Archives/archives.json"
    try:
        with ARCHIVE_MANIFEST.open("rb") as manifest_file:
            content = base64.b64encode(manifest_file.read()).decode("ascii")
        request = urllib.request.Request(
            api_url,
            data=json.dumps({
                "message": "Update Archives manifest",
                "content": content,
                "branch": branch,
            }).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {token}",
                "Accept": "application/vnd.github+json",
                "Content-Type": "application/json",
                "User-Agent": "Edu-AI-admin",
            },
            method="PUT",
        )
        with urllib.request.urlopen(request, timeout=10):
            return
    except urllib.error.HTTPError as error:
        if error.code != 422:
            raise
        with urllib.request.urlopen(
            urllib.request.Request(api_url, headers={"Authorization": f"Bearer {token}", "User-Agent": "Edu-AI-admin"}),
            timeout=10,
        ) as response:
            current = json.loads(response.read().decode("utf-8"))
        request_data = json.dumps({
            "message": "Update Archives manifest",
            "content": content,
            "branch": branch,
            "sha": current["sha"],
        }).encode("utf-8")
        with urllib.request.urlopen(
            urllib.request.Request(api_url, data=request_data, headers={
                "Authorization": f"Bearer {token}",
                "Accept": "application/vnd.github+json",
                "Content-Type": "application/json",
                "User-Agent": "Edu-AI-admin",
            }, method="PUT"),
            timeout=10,
        ):
            return

# Create database tables defined in models
Base.metadata.create_all(bind=engine)

# Keep existing deployments usable when the ingestion hierarchy columns are added.
with engine.begin() as connection:
    columns = {column["name"] for column in inspect(connection).get_columns("units")}
    for column_name, definition in (("course", "VARCHAR(200)"), ("unit_group", "VARCHAR(200)")):
        if column_name not in columns:
            connection.execute(text(f"ALTER TABLE units ADD COLUMN {column_name} {definition}"))
    migrations = {
        "chat_sessions": {
            "transcript_json": "JSON NOT NULL DEFAULT '[]'",
            "client_session_id": "VARCHAR(100)",
        },
        "quiz_history": {"quiz_id": "INTEGER", "correct_answers": "INTEGER"},
    }
    for table_name, table_columns in migrations.items():
        existing = {column["name"] for column in inspect(connection).get_columns(table_name)}
        for column_name, definition in table_columns.items():
            if column_name not in existing:
                connection.execute(text(f"ALTER TABLE {table_name} ADD COLUMN {column_name} {definition}"))

# Instantiate main FastAPI app
BACKEND_VERSION = "3.1.6"
app = FastAPI(title="Trace Modular API", version=BACKEND_VERSION)

INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "DEVELOPMENT_KEY")

# Keep the browser-facing database view explicit and read-only. Mutations continue
# to use the existing, validated admin forms below.
DATABASE_BROWSER_MODELS = (
    ("Users", models.User),
    ("Release archive", models.ReleaseArchive),
    ("Archive assets", models.ArchiveAsset),
    ("Units", models.Unit),
    ("Modules", models.Module),
    ("Topics", models.Topic),
    ("Subtopics", models.Subtopic),
    ("Learning objectives", models.LearningObjective),
    ("Learning content", models.LearningContent),
    ("User syllabus progress", models.UserSyllabusProgress),
    ("Quizzes", models.Quiz),
    ("Quiz history", models.QuizHistory),
    ("Chat sessions", models.ChatSession),
    ("Chat messages", models.ChatMessage),
    ("Performance logs", models.PerformanceLog),
    ("Timetables", models.Timetable),
)
DATABASE_MODEL_BY_TABLE = {model.__tablename__: model for _, model in DATABASE_BROWSER_MODELS}
DATABASE_PROTECTED_COLUMNS = {"id", "hashed_password"}

# ---------------------------------------------------------------------------
# Application middleware and API registration
# ---------------------------------------------------------------------------
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
        "/api/units/library", "/api/sync", "/admin/login"
    ]

    path = request.url.path
    if (
        path in bypass_paths
        or path.startswith("/delete-unit/")
        or path.startswith("/update-unit/")
        or path.startswith("/api/units/library/add/")
        or path.startswith("/admin")
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


# ---------------------------------------------------------------------------
# Shared request helpers and admin view context
# ---------------------------------------------------------------------------

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


def admin_token(request: Request):
    token = request.cookies.get("trace_admin_token")
    if not token:
        return None
    payload = security.decode_access_token(token)
    return payload if payload and payload.get("role") == "Admin" else None


def require_admin(request: Request):
    payload = admin_token(request)
    if not payload:
        raise HTTPException(status_code=303, headers={"Location": "/admin/login"})
    return payload


def build_database_browser(db: Session):
    """Return table-shaped snapshots for the guarded admin database editor."""
    tables = []
    for label, model in DATABASE_BROWSER_MODELS:
        columns = [column.name for column in model.__table__.columns]
        editable_columns = [
            column for column in columns
            if column not in DATABASE_PROTECTED_COLUMNS
            and not getattr(model.__table__.columns[column], "primary_key", False)
        ]
        rows = []
        for record in db.query(model).all():
            values = {}
            for column in columns:
                value = getattr(record, column)
                if column == "hashed_password":
                    values[column] = "[hidden]"
                elif isinstance(value, (dict, list)):
                    values[column] = json.dumps(value)
                else:
                    values[column] = "" if value is None else str(value)
            rows.append({
                "id": record.id,
                "values": values,
                "original": json.dumps(
                    {column: values[column] for column in editable_columns},
                    sort_keys=True,
                ),
            })
        tables.append({
            "label": label,
            "table_name": model.__tablename__,
            "columns": columns,
            "editable_columns": editable_columns,
            "rows": rows,
        })
    return tables


def database_column_value(model, column_name, value):
    """Convert an editor value using the SQLAlchemy column's declared type."""
    column = model.__table__.columns[column_name]
    if value == "" and column.nullable:
        return None
    try:
        python_type = column.type.python_type
    except (AttributeError, NotImplementedError):
        python_type = str
    if python_type is bool:
        return value.lower() in {"1", "true", "yes", "on"}
    if python_type in (dict, list):
        return json.loads(value)
    return python_type(value)


def render_admin(request: Request, db: Session, section="overview", **context):
    users = db.query(models.User).order_by(models.User.id.desc()).all()
    units = ingestion_engine.get_global_units(db)
    manifest = read_archive_manifest()
    releases = manifest["releases"]
    archive_files = []
    if ARCHIVES_DIR.is_dir():
        for path in sorted(ARCHIVES_DIR.iterdir(), key=lambda item: item.stat().st_mtime, reverse=True):
            if path.is_file() and path.name != ARCHIVE_MANIFEST.name:
                metadata = manifest["assets"].get(path.name, {})
                stat = path.stat()
                archive_files.append({
                    "file_name": path.name,
                    "display_name": metadata.get("display_name") or path.stem,
                    "artifact_type": metadata.get("artifact_type") or "Trace Mobile App",
                    "release_notes": metadata.get("release_notes"),
                    "size": stat.st_size,
                    "modified_at": datetime.fromtimestamp(stat.st_mtime),
                })
    database_tables = build_database_browser(db) if section == "database" else []
    return templates.TemplateResponse(
        "admin/dashboard.html",
        {
            "request": request,
            "version": BACKEND_VERSION,
            "section": section,
            "users": users,
            "units": units,
            "releases": releases,
            "archive_files": archive_files,
            "database_tables": database_tables,
            **context,
        },
    )


# ---------------------------------------------------------------------------
# Admin authentication and dashboard navigation
# ---------------------------------------------------------------------------

@app.get("/", response_class=HTMLResponse)
def root(request: Request, db: Session = Depends(get_db)):
    """
    Renders the root ingestion dashboard interface.

    :param request: The incoming HTTP request.
    :param db: SQLAlchemy Session dependency.
    :return: HTMLResponse rendering the admin login or dashboard.
    """
    if not admin_token(request):
        return templates.TemplateResponse(
            "admin/login.html",
            {"request": request, "version": BACKEND_VERSION, "error": None},
        )
    return render_admin(request, db)


@app.get("/admin/login", response_class=HTMLResponse)
def admin_login_page(request: Request):
    return templates.TemplateResponse(
        "admin/login.html",
        {"request": request, "version": BACKEND_VERSION, "error": None},
    )


@app.post("/admin/login", response_class=HTMLResponse)
def admin_login(
    request: Request,
    email: str = Form(...),
    password: str = Form(...),
):
    configured_email = os.getenv("ADMIN_EMAIL")
    configured_password = os.getenv("ADMIN_PASSWORD")
    if not configured_email or not configured_password:
        return templates.TemplateResponse(
            "admin/login.html",
            {
                "request": request,
                "version": BACKEND_VERSION,
                "error": "Admin login is not configured. Set ADMIN_EMAIL and ADMIN_PASSWORD.",
            },
            status_code=503,
        )
    if email.strip().lower() != configured_email.strip().lower() or password != configured_password:
        return templates.TemplateResponse(
            "admin/login.html",
            {"request": request, "version": BACKEND_VERSION, "error": "Invalid admin credentials."},
            status_code=401,
        )
    response = HTMLResponse(status_code=303, headers={"Location": "/admin"})
    response.set_cookie(
        "trace_admin_token",
        security.create_access_token({"sub": "admin", "role": "Admin"}),
        httponly=True,
        secure=False,
        samesite="lax",
    )
    return response


@app.post("/admin/logout")
def admin_logout():
    response = HTMLResponse(status_code=303, headers={"Location": "/admin/login"})
    response.delete_cookie("trace_admin_token")
    return response


@app.get("/admin", response_class=HTMLResponse)
def admin_dashboard(request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    return render_admin(request, db)


@app.get("/admin/ingestion", response_class=HTMLResponse)
def admin_ingestion(request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    catalog = ingestion_engine.get_global_unit_catalog(db)
    return templates.TemplateResponse(
        "curriculum/ingestion.html",
        {"request": request, "catalog": catalog, "version": BACKEND_VERSION, "admin": True},
    )


@app.get("/admin/catalogue", response_class=HTMLResponse)
def admin_catalogue(request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    return render_admin(request, db, section="catalogue", catalog=ingestion_engine.get_global_unit_catalog(db))


@app.get("/admin/database", response_class=HTMLResponse)
def admin_database(request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    return render_admin(request, db, section="database")


@app.post("/admin/database/{table_name}/{row_id}", response_class=HTMLResponse)
async def update_database_row(
    table_name: str,
    row_id: int,
    request: Request,
    db: Session = Depends(get_db),
):
    """Commit one guarded row update after optimistic conflict and password checks."""
    require_admin(request)
    model = DATABASE_MODEL_BY_TABLE.get(table_name)
    if not model:
        raise HTTPException(status_code=404, detail="Database table is not available")
    form = await request.form()
    admin_password = str(form.get("admin_password", ""))
    configured_password = os.getenv("ADMIN_PASSWORD", "")
    if not configured_password or not hmac.compare_digest(admin_password, configured_password):
        raise HTTPException(status_code=401, detail="Admin password confirmation failed")

    record = db.query(model).filter(model.id == row_id).first()
    if not record:
        raise HTTPException(status_code=404, detail="Database row not found")
    editable_columns = [
        column.name for column in model.__table__.columns
        if column.name not in DATABASE_PROTECTED_COLUMNS
        and not column.primary_key
    ]
    original = json.loads(str(form.get("original", "{}")))
    current = {}
    for column_name in editable_columns:
        value = getattr(record, column_name)
        current[column_name] = json.dumps(value) if isinstance(value, (dict, list)) else (
            "" if value is None else str(value)
        )
    if current != original:
        raise HTTPException(
            status_code=409,
            detail="Conflict detected: reload the database browser before committing.",
        )
    try:
        for column_name in editable_columns:
            if column_name in form:
                setattr(record, column_name, database_column_value(
                    model,
                    column_name,
                    str(form.get(column_name)),
                ))
        db.commit()
    except (TypeError, ValueError, json.JSONDecodeError) as error:
        db.rollback()
        raise HTTPException(status_code=422, detail=f"Invalid value: {error}") from error
    return HTMLResponse(status_code=303, headers={"Location": "/admin/database"})


@app.get("/admin/users", response_class=HTMLResponse)
def admin_users(request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    return render_admin(request, db, section="users")


@app.get("/admin/users/{user_id}", response_class=HTMLResponse)
def admin_user_detail(user_id: int, request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return templates.TemplateResponse(
        "users/detail.html",
        {
            "request": request,
            "version": BACKEND_VERSION,
            "user": user,
            "progress": db.query(models.UserSyllabusProgress)
            .filter(models.UserSyllabusProgress.user_id == user.id)
            .order_by(models.UserSyllabusProgress.id.desc())
            .all(),
            "quiz_history": db.query(models.QuizHistory)
            .filter(models.QuizHistory.owner_id == user.id)
            .order_by(models.QuizHistory.id.desc())
            .all(),
            "chat_sessions": db.query(models.ChatSession)
            .filter(models.ChatSession.owner_id == user.id)
            .order_by(models.ChatSession.id.desc())
            .all(),
            "performance_logs": db.query(models.PerformanceLog)
            .filter(models.PerformanceLog.owner_id == user.id)
            .order_by(models.PerformanceLog.id.desc())
            .all(),
        },
    )


@app.post("/admin/users/{user_id}", response_class=HTMLResponse)
def update_admin_user(
    user_id: int,
    request: Request,
    username: str = Form(...),
    email: str = Form(""),
    role: str = Form(...),
    sensory_mode: str = Form(...),
    difficulty: str = Form(...),
    ai_persona: str = Form(...),
    semester_status: str = Form(...),
    db: Session = Depends(get_db),
):
    require_admin(request)
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    duplicate = db.query(models.User).filter(
        models.User.id != user.id,
        (models.User.username == username.strip()) | (models.User.email == email.strip()),
    ).first()
    if duplicate:
        raise HTTPException(status_code=409, detail="Username or email is already in use")
    user.username = username.strip()
    user.email = email.strip() or None
    user.role = role.strip()
    user.sensory_mode = sensory_mode.strip()
    user.difficulty = difficulty.strip()
    user.ai_persona = ai_persona.strip()
    user.semester_status = semester_status.strip()
    db.commit()
    return HTMLResponse(status_code=303, headers={"Location": f"/admin/users/{user_id}"})


@app.post("/admin/users/{user_id}/delete")
def delete_admin_user(user_id: int, request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    db.query(models.UserSyllabusProgress).filter(
        models.UserSyllabusProgress.user_id == user.id
    ).delete(synchronize_session=False)
    db.delete(user)
    db.commit()
    return HTMLResponse(status_code=303, headers={"Location": "/admin/users"})


@app.get("/admin/releases", response_class=HTMLResponse)
def admin_releases(request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    return render_admin(request, db, section="archives")


@app.post("/admin/archives/{file_name}", response_class=HTMLResponse)
def update_archive_asset(
    file_name: str,
    request: Request,
    display_name: str = Form(...),
    artifact_type: str = Form(...),
    release_notes: str = Form(""),
    db: Session = Depends(get_db),
):
    require_admin(request)
    path = (ARCHIVES_DIR / file_name).resolve()
    if path.parent != ARCHIVES_DIR.resolve() or not path.is_file():
        raise HTTPException(status_code=404, detail="Archive file not found")
    asset = db.query(models.ArchiveAsset).filter(models.ArchiveAsset.file_name == file_name).first()
    if not asset:
        asset = models.ArchiveAsset(file_name=file_name)
        db.add(asset)
    asset.display_name = display_name.strip()
    asset.artifact_type = artifact_type.strip()
    asset.release_notes = release_notes.strip() or None
    db.commit()
    manifest = read_archive_manifest()
    manifest["assets"][file_name] = {
        "display_name": display_name.strip(),
        "artifact_type": artifact_type.strip(),
        "release_notes": release_notes.strip() or None,
    }
    write_archive_manifest(manifest)
    return HTMLResponse(status_code=303, headers={"Location": "/admin/releases"})


@app.get("/admin/archives/{file_name}")
def download_archive_asset(file_name: str, request: Request):
    require_admin(request)
    path = (ARCHIVES_DIR / file_name).resolve()
    if path.parent != ARCHIVES_DIR.resolve() or not path.is_file():
        raise HTTPException(status_code=404, detail="Archive file not found")
    return FileResponse(path)


@app.get("/api/releases/archive/{file_name}")
def download_release_archive_asset(file_name: str):
    path = (ARCHIVES_DIR / file_name).resolve()
    if path.parent != ARCHIVES_DIR.resolve() or not path.is_file():
        raise HTTPException(status_code=404, detail="Archive file not found")
    return FileResponse(path)


@app.post("/admin/releases", response_class=HTMLResponse)
def create_release(
    request: Request,
    version: str = Form(...),
    artifact_type: str = Form("Trace Mobile App"),
    download_url: str = Form(""),
    release_notes: str = Form(""),
    is_current: bool = Form(False),
    db: Session = Depends(get_db),
):
    require_admin(request)
    if is_current:
        db.query(models.ReleaseArchive).update(
            {models.ReleaseArchive.is_current: False},
            synchronize_session=False,
        )
    release = models.ReleaseArchive(
        version=version.strip(),
        artifact_type=artifact_type.strip(),
        download_url=download_url.strip() or None,
        release_notes=release_notes.strip() or None,
        released_at=time.time(),
        is_current=is_current,
    )
    db.add(release)
    db.commit()
    manifest = read_archive_manifest()
    manifest["releases"].append({
        "version": version.strip(),
        "artifact_type": artifact_type.strip(),
        "download_url": download_url.strip() or None,
        "release_notes": release_notes.strip() or None,
        "released_at": time.time(),
        "is_current": is_current,
    })
    write_archive_manifest(manifest)
    return HTMLResponse(status_code=303, headers={"Location": "/admin/releases"})


@app.post("/admin/releases/{release_id}/delete")
def delete_release(release_id: int, request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    release = db.query(models.ReleaseArchive).filter(models.ReleaseArchive.id == release_id).first()
    if not release:
        raise HTTPException(status_code=404, detail="Release not found")
    db.delete(release)
    db.commit()
    manifest = read_archive_manifest()
    manifest["releases"] = [
        item for item in manifest["releases"]
        if item.get("version") != release.version
    ]
    write_archive_manifest(manifest)
    return HTMLResponse(status_code=303, headers={"Location": "/admin/releases"})


@app.get("/api/releases/archive")
def release_archive(db: Session = Depends(get_db)):
    releases = read_archive_manifest()["releases"]
    if ARCHIVES_DIR.is_dir():
        for path in sorted(ARCHIVES_DIR.iterdir(), key=lambda item: item.stat().st_mtime, reverse=True):
            if path.is_file() and path.name != ARCHIVE_MANIFEST.name:
                metadata = read_archive_manifest()["assets"].get(path.name, {})
                releases.append({
                    "version": metadata.get("display_name") or path.stem,
                    "artifact_type": metadata.get("artifact_type") or "Trace Mobile App",
                    "download_url": f"/api/releases/archive/{path.name}",
                    "release_notes": metadata.get("release_notes"),
                    "released_at": path.stat().st_mtime,
                    "is_current": False,
                    "file_name": path.name,
                })
    return releases


# ---------------------------------------------------------------------------
# Curriculum catalogue editing
# ---------------------------------------------------------------------------

@app.get("/admin/units/{unit_id}", response_class=HTMLResponse)
def admin_unit_detail(unit_id: int, request: Request, db: Session = Depends(get_db)):
    require_admin(request)
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id, models.Unit.owner_id.is_(None)).first()
    if not unit:
        raise HTTPException(status_code=404, detail="Global unit not found")
    return templates.TemplateResponse(
        "curriculum/unit_detail.html",
        {"request": request, "unit": unit, "version": BACKEND_VERSION},
    )


@app.post("/admin/units/{unit_id}", response_class=HTMLResponse)
def update_admin_unit(
    unit_id: int,
    request: Request,
    name: str = Form(...),
    field: str = Form(...),
    course: str = Form(...),
    unit_group: str = Form(...),
    db: Session = Depends(get_db),
):
    require_admin(request)
    unit = db.query(models.Unit).filter(models.Unit.id == unit_id, models.Unit.owner_id.is_(None)).first()
    if not unit:
        raise HTTPException(status_code=404, detail="Global unit not found")
    unit.name = name.strip()
    unit.category = field.strip()
    unit.course = course.strip()
    unit.unit_group = unit_group.strip()
    db.commit()
    return HTMLResponse(status_code=303, headers={"Location": f"/admin/units/{unit_id}"})


@app.post("/admin/nodes/{node_type}/{node_id}", response_class=HTMLResponse)
def update_admin_node(
    node_type: str,
    node_id: int,
    request: Request,
    name: str = Form(...),
    db: Session = Depends(get_db),
):
    require_admin(request)
    node_models = {
        "module": models.Module,
        "topic": models.Topic,
        "subtopic": models.Subtopic,
        "objective": models.LearningObjective,
    }
    node_model = node_models.get(node_type)
    if not node_model:
        raise HTTPException(status_code=400, detail="Unsupported curriculum node")
    node = db.query(node_model).filter(node_model.id == node_id).first()
    if not node:
        raise HTTPException(status_code=404, detail="Curriculum node not found")
    if node_type == "objective":
        node.description = name.strip()
        unit_id = node.subtopic.topic.module.unit_id
    elif node_type == "module":
        node.name = name.strip()
        unit_id = node.unit_id
    elif node_type == "topic":
        node.name = name.strip()
        unit_id = node.module.unit_id
    else:
        node.name = name.strip()
        unit_id = node.topic.module.unit_id
    db.commit()
    return HTMLResponse(status_code=303, headers={"Location": f"/admin/units/{unit_id}"})


@app.post("/admin/nodes", response_class=HTMLResponse)
def create_admin_node(
    request: Request,
    node_type: str = Form(...),
    parent_id: int = Form(...),
    name: str = Form(...),
    db: Session = Depends(get_db),
):
    require_admin(request)
    if not name.strip():
        raise HTTPException(status_code=400, detail="Curriculum node name cannot be empty")
    parent_models = {
        "module": (models.Unit, models.Module, "unit_id"),
        "topic": (models.Module, models.Topic, "module_id"),
        "subtopic": (models.Topic, models.Subtopic, "topic_id"),
        "objective": (models.Subtopic, models.LearningObjective, "subtopic_id"),
    }
    relation = parent_models.get(node_type)
    if not relation:
        raise HTTPException(status_code=400, detail="Unsupported curriculum node")
    parent_model, node_model, foreign_key = relation
    parent = db.query(parent_model).filter(parent_model.id == parent_id).first()
    if not parent:
        raise HTTPException(status_code=404, detail="Curriculum parent not found")
    node = node_model(
        **{foreign_key: parent_id},
        **({"description": name.strip()} if node_type == "objective" else {"name": name.strip()}),
    )
    db.add(node)
    db.commit()
    if node_type == "module":
        unit_id = parent.id
    elif node_type == "topic":
        unit_id = parent.unit_id
    elif node_type == "subtopic":
        unit_id = parent.module.unit_id
    else:
        unit_id = parent.topic.module.unit_id
    return HTMLResponse(status_code=303, headers={"Location": f"/admin/units/{unit_id}"})


@app.post("/admin/nodes/{node_type}/{node_id}/delete", response_class=HTMLResponse)
def delete_admin_node(
    node_type: str,
    node_id: int,
    request: Request,
    db: Session = Depends(get_db),
):
    require_admin(request)
    node_models = {
        "module": models.Module,
        "topic": models.Topic,
        "subtopic": models.Subtopic,
        "objective": models.LearningObjective,
    }
    node_model = node_models.get(node_type)
    if not node_model:
        raise HTTPException(status_code=400, detail="Unsupported curriculum node")
    node = db.query(node_model).filter(node_model.id == node_id).first()
    if not node:
        raise HTTPException(status_code=404, detail="Curriculum node not found")
    if node_type == "module":
        unit_id = node.unit_id
    elif node_type == "topic":
        unit_id = node.module.unit_id
    elif node_type == "subtopic":
        unit_id = node.topic.module.unit_id
    else:
        unit_id = node.subtopic.topic.module.unit_id
    db.delete(node)
    db.commit()
    return HTMLResponse(status_code=303, headers={"Location": f"/admin/units/{unit_id}"})


# ---------------------------------------------------------------------------
# Ingestion and application-facing unit library endpoints
# ---------------------------------------------------------------------------

@app.post("/ingest", response_class=HTMLResponse)
async def handle_ingestion(
    request: Request,
    markdown: str = Form(...),
    field: str = Form(...),
    course: str = Form(...),
    unit: str = Form(""),
    db: Session = Depends(get_db),
):
    """
    Processes markdown syllabus content for content ingestion and unit creation.

    :param request: The incoming HTTP request.
    :param markdown: Markdown formatted syllabus string submitted via form payload.
    :param db: SQLAlchemy Session dependency.
    :return: Updated ingestion HTML page or HTTP 500 HTML error page on failure.
    """
    require_admin(request)
    try:
        syllabus_data = ingestion_engine.parse_syllabus_markdown(markdown, field, course, unit or None)
        ingestion_engine.save_syllabus_to_db(db, syllabus_data)
        catalog = ingestion_engine.get_global_unit_catalog(db)
        return templates.TemplateResponse(
            "curriculum/ingestion.html",
            {"request": request, "catalog": catalog, "version": BACKEND_VERSION, "admin": True},
        )
    except Exception as e:
        logger.error(f"Ingestion error: {e}")
        return HTMLResponse(content=f"<h1>Internal Server Error</h1><p>{str(e)}</p>", status_code=500)


@app.post("/delete-unit/{unit_id}", response_class=HTMLResponse)
async def handle_delete_unit(
    request: Request,
    unit_id: int,
    delete_scope: str = Form("catalogue"),
    db: Session = Depends(get_db),
):
    """
    Deletes a specified global learning unit and re-renders the ingestion management UI.

    :param request: The incoming HTTP request.
    :param unit_id: Unique identifier of the unit to delete.
    :param db: SQLAlchemy Session dependency.
    :return: HTMLResponse rendering 'ingestion.html'.
    """
    require_admin(request)
    unit = db.query(models.Unit).filter(
        models.Unit.id == unit_id,
        models.Unit.owner_id.is_(None),
    ).first()
    if not unit:
        raise HTTPException(status_code=404, detail="Catalogue unit not found")
    if delete_scope == "users":
        db.query(models.Unit).filter(
            models.Unit.owner_id.is_not(None),
            models.Unit.name == unit.name,
            models.Unit.unit_group == unit.unit_group,
        ).delete(synchronize_session=False)
    db.delete(unit)
    db.commit()
    catalog = ingestion_engine.get_global_unit_catalog(db)
    return templates.TemplateResponse(
        "curriculum/ingestion.html",
        {"request": request, "catalog": catalog, "version": BACKEND_VERSION, "admin": True},
    )


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
    require_admin(request)
    ingestion_engine.update_unit(db, unit_id, name)
    catalog = ingestion_engine.get_global_unit_catalog(db)
    return templates.TemplateResponse(
        "curriculum/ingestion.html",
        {"request": request, "catalog": catalog, "version": BACKEND_VERSION, "admin": True},
    )


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
    return templates.TemplateResponse("public/signup.html", {"request": request})


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
