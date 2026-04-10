# IDENTITY: backend/models.py
# VERSION: 1.1.0
# ⚙️ GEAR 1.2: Database Models (Entities)

from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, JSON
from sqlalchemy.orm import relationship
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    role = Column(String, default="Student") # "Student" or "Teacher"
    sensory_mode = Column(String, default="Standard") # "Standard", "Low-Sensory", "High-Stim"
    difficulty = Column(String, default="Medium (Standard)")
    ai_persona = Column(String, default="Standard Edu_AI")
    semester_status = Column(String, default="Year 4 - Redemption Arc")
    interests = Column(JSON, default=list)

    # Relationships
    units = relationship("Unit", back_populates="owner")
    quiz_history = relationship("QuizHistory", back_populates="owner")
    performance_logs = relationship("PerformanceLog", back_populates="owner")

class Unit(Base):
    __tablename__ = "units"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    is_active = Column(Boolean, default=True)
    category = Column(String, default="General")
    
    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="units")

class QuizHistory(Base):
    __tablename__ = "quiz_history"

    id = Column(Integer, primary_key=True, index=True)
    unit_name = Column(String)
    score = Column(Integer)
    total = Column(Integer)
    pnl = Column(Float)
    timestamp = Column(String)

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="quiz_history")

class PerformanceLog(Base):
    """Tracks performance over time for the AI predictive chart."""
    __tablename__ = "performance_logs"

    id = Column(Integer, primary_key=True, index=True)
    subject = Column(String)
    grade = Column(Float)
    timestamp = Column(String)

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="performance_logs")