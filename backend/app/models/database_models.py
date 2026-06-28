from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, JSON, Text
from sqlalchemy.orm import relationship
from app.db.session import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(100), unique=True, index=True)
    email = Column(String(100), unique=True, index=True, nullable=True)
    hashed_password = Column(String(200), nullable=True)
    role = Column(String(50), default="Student")
    sensory_mode = Column(String(50), default="Standard")
    difficulty = Column(String(50), default="Medium (Standard)")
    ai_persona = Column(String(100), default="Standard Trace")
    semester_status = Column(String(100), default="Year 4 - Redemption Arc")
    interests = Column(JSON, default=list)

    # Relationships
    units = relationship("Unit", back_populates="owner", cascade="all, delete-orphan")
    quiz_history = relationship("QuizHistory", back_populates="owner", cascade="all, delete-orphan")
    chat_messages = relationship("ChatMessage", back_populates="owner", cascade="all, delete-orphan")
    chat_sessions = relationship("ChatSession", back_populates="owner", cascade="all, delete-orphan")
    performance_logs = relationship("PerformanceLog", back_populates="owner", cascade="all, delete-orphan")
    timetables = relationship("Timetable", back_populates="owner", cascade="all, delete-orphan")

    @property
    def active_units_list(self):
        return [u.name for u in self.units if u.is_active]

class Unit(Base):
    __tablename__ = "units"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), index=True)
    is_active = Column(Boolean, default=True)
    category = Column(String(100), default="General")

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="units")

    # Relationships to lower levels
    modules = relationship("Module", back_populates="unit", cascade="all, delete-orphan")

class Module(Base):
    __tablename__ = "modules"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), index=True)

    unit_id = Column(Integer, ForeignKey("units.id"))
    unit = relationship("Unit", back_populates="modules")
    subtopics = relationship("Subtopic", back_populates="module", cascade="all, delete-orphan")

class Subtopic(Base):
    __tablename__ = "subtopics"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), index=True)
    is_completed = Column(Boolean, default=False)

    module_id = Column(Integer, ForeignKey("modules.id"))
    module = relationship("Module", back_populates="subtopics")

class QuizHistory(Base):
    __tablename__ = "quiz_history"

    id = Column(Integer, primary_key=True, index=True)
    unit_name = Column(String(200))
    score = Column(Integer)
    total = Column(Integer)
    pnl = Column(Float)
    timestamp = Column(String(100))

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="quiz_history")

class ChatSession(Base):
    __tablename__ = "chat_sessions"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(200), default="New Consultation")
    description = Column(Text, nullable=True)
    timestamp = Column(Float)
    is_archived = Column(Boolean, default=False)

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="chat_sessions")
    messages = relationship("ChatMessage", back_populates="session", cascade="all, delete-orphan")

class ChatMessage(Base):
    __tablename__ = "chat_messages"

    id = Column(Integer, primary_key=True, index=True)
    role = Column(String(20)) # "user" or "model"
    content = Column(Text)
    timestamp = Column(String(100))

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="chat_messages")

    session_id = Column(Integer, ForeignKey("chat_sessions.id"), nullable=True)
    session = relationship("ChatSession", back_populates="messages")

class PerformanceLog(Base):
    __tablename__ = "performance_logs"

    id = Column(Integer, primary_key=True, index=True)
    subject = Column(String(200))
    grade = Column(Float)
    timestamp = Column(String(100))

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="performance_logs")

class Timetable(Base):
    __tablename__ = "timetables"

    id = Column(Integer, primary_key=True, index=True)
    weekly_plan_json = Column(JSON)
    ai_brief = Column(Text)
    timestamp = Column(Float) # Time of generation

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="timetables")
