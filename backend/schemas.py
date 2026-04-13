# IDENTITY: backend/schemas.py
# VERSION: 1.5.0
# ⚙️ Pydantic Models for Data Validation

from pydantic import BaseModel
from typing import List, Optional, Any

class UnitBase(BaseModel):
    name: str
    is_active: bool = True
    category: str = "General"

class UnitCreate(UnitBase):
    owner_id: int

class UnitUpdate(BaseModel):
    name: Optional[str] = None
    is_active: Optional[bool] = None
    category: Optional[str] = None

class UnitResponse(UnitBase):
    id: int
    owner_id: int
    class Config:
        from_attributes = True

class DashboardResponse(BaseModel):
    username: str
    role: str
    sensory_mode: str
    semester_status: str
    difficulty: str
    ai_persona: str
    active_units: List[str]
    average_pnl: float
    total_quizzes: int

class ChaosRequest(BaseModel):
    unit: str
    focus_area: Optional[str] = None
    difficulty: str = "Asian Parent Expectations (Extreme)"
    student_id: str

class ChaosResponse(BaseModel):
    case_study: str

class UserCreate(BaseModel):
    username: str
    role: str = "Student"
    sensory_mode: str = "Standard"
    difficulty: str = "Medium (Standard)"
    ai_persona: str = "Standard Edu_AI"
    semester_status: str = "Year 4 - Redemption Arc"
    interests: List[str] = []
    active_units: List[str] = []

class UserUpdate(BaseModel):
    role: Optional[str] = None
    sensory_mode: Optional[str] = None
    difficulty: Optional[str] = None
    ai_persona: Optional[str] = None
    semester_status: Optional[str] = None
    interests: Optional[List[str]] = None
    active_units: Optional[List[str]] = None

class UserResponseSchema(BaseModel):
    id: int
    username: str
    role: str
    sensory_mode: str
    difficulty: str
    ai_persona: str
    semester_status: str
    interests: List[str]
    active_units: List[str] = []

    class Config:
        from_attributes = True

class UserPreferencesUpdate(BaseModel):
    sensory_mode: Optional[str] = None
    ai_persona: Optional[str] = None

# --- AI Models ---
class ChatMessage(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    prompt: str
    user_id: str
    history: List[ChatMessage] = []

class ChatResponse(BaseModel):
    response: str

class QuizRequest(BaseModel):
    unit_name: str
    user_id: str

class QuizQuestion(BaseModel):
    question_text: str
    options: List[str]
    correct_option_index: int
    explanation: str

class QuizResponse(BaseModel):
    quiz_title: str
    questions: List[QuizQuestion]

class QuizRecordRequest(BaseModel):
    unit_name: str
    score: int
    total: int
    user_id: str
    timestamp: Any

class RecommendationResponse(BaseModel):
    recommendation: str
