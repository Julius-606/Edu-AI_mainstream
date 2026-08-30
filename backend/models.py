# IDENTITY: backend/models.py
# VERSION: 1.7.0
# ⚙️ GEAR 1.2: Database Models (Entities) - PostgreSQL & CAS Optimized

from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, JSON, Text, LargeBinary
from sqlalchemy.orm import relationship
from database import Base

class CasBlob(Base):
    __tablename__ = "cas_blobs"

    hash = Column(String(64), primary_key=True, index=True)
    compressed_content = Column(LargeBinary, nullable=True)
    content_size = Column(Integer, default=0)
    created_at = Column(Float)

class CommitLog(Base):
    __tablename__ = "commit_logs"

    commit_hash = Column(String(64), primary_key=True, index=True)
    parent_hash = Column(String(64), nullable=True)
    entity_type = Column(String(50)) # "note", "chat", "quiz", "subtopic"
    entity_id = Column(String(100))
    delta_patch = Column(Text, default="")
    blob_hash = Column(String(64), default="")
    timestamp = Column(Float)

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User")

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
    notes = relationship("Note", back_populates="owner", cascade="all, delete-orphan")

    @property
    def active_units_list(self):
        return [u.name for u in self.units if u.is_active]

    @property
    def archived_units_list(self):
        return [u.name for u in self.units if not u.is_active]

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

    topics = relationship("Topic", back_populates="module", cascade="all, delete-orphan")

class Topic(Base):
    __tablename__ = "topics"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), index=True)

    module_id = Column(Integer, ForeignKey("modules.id"))
    module = relationship("Module", back_populates="topics")

    subtopics = relationship("Subtopic", back_populates="topic", cascade="all, delete-orphan")

class Subtopic(Base):
    __tablename__ = "subtopics"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(200), index=True)
    is_completed = Column(Boolean, default=False)

    topic_id = Column(Integer, ForeignKey("topics.id"))
    topic = relationship("Topic", back_populates="subtopics")

    learning_objectives = relationship("LearningObjective", back_populates="subtopic", cascade="all, delete-orphan")

class LearningObjective(Base):
    __tablename__ = "learning_objectives"

    id = Column(Integer, primary_key=True, index=True)
    description = Column(Text)
    is_completed = Column(Boolean, default=False)

    subtopic_id = Column(Integer, ForeignKey("subtopics.id"))
    subtopic = relationship("Subtopic", back_populates="learning_objectives")

class UserSyllabusProgress(Base):
    __tablename__ = "user_syllabus_progress"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    node_id = Column(Integer)
    node_type = Column(String(50))
    status = Column(String(50), default="Locked")
    last_studied_at = Column(Float, nullable=True)

    user = relationship("User")

class QuizHistory(Base):
    __tablename__ = "quiz_history"

    id = Column(Integer, primary_key=True, index=True)
    unit_name = Column(String(200))
    score = Column(Integer)
    total = Column(Integer)
    pnl = Column(Float)
    timestamp = Column(String(100))
    quiz_json_hash = Column(String(64), nullable=True)

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
    role = Column(String(20))
    content = Column(Text)
    content_hash = Column(String(64), nullable=True)
    timestamp = Column(String(100))

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="chat_messages")

    session_id = Column(Integer, ForeignKey("chat_sessions.id"), nullable=True)
    session = relationship("ChatSession", back_populates="messages")

class Note(Base):
    __tablename__ = "notes"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(200))
    content_hash = Column(String(64), nullable=True)
    head_commit_hash = Column(String(64), nullable=True)
    timestamp = Column(Float)

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="notes")

    session_id = Column(Integer, ForeignKey("chat_sessions.id"), nullable=True)

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
    timestamp = Column(Float)

    owner_id = Column(Integer, ForeignKey("users.id"))
    owner = relationship("User", back_populates="timetables")
