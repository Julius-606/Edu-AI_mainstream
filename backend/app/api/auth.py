from fastapi import APIRouter, Depends, HTTPException, Form
from sqlalchemy.orm import Session
from app.db.session import get_db
from app.models import database_models as models
from app.schemas import api_schemas as schemas
from app.core import security

router = APIRouter(tags=["Authentication"])

@router.post("/login", response_model=schemas.TokenResponse)
async def login(request: schemas.LoginRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == request.email).first()
    if not user or not security.verify_password(request.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid email or password")

    access_token = security.create_access_token(data={"sub": str(user.id)})
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user_id": str(user.id),
        "username": user.username,
        "role": user.role
    }

@router.post("/signup-form") # Renamed from /signup to avoid conflict with HTML signup if any
async def handle_signup(
    username: str = Form(...),
    email: str = Form(...),
    password: str = Form(...),
    role: str = Form(...),
    db: Session = Depends(get_db)
):
    existing_user = db.query(models.User).filter((models.User.email == email) | (models.User.username == username)).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="Email or Username already exists")

    new_user = models.User(
        username=username,
        email=email,
        hashed_password=security.get_password_hash(password),
        role=role
    )
    db.add(new_user)
    db.commit()
    return {"status": "success", "message": "Account created"}
