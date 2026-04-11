# IDENTITY: backend/schemas.py
# VERSION: 1.1.1
# ⚙️ Pydantic Models for Data Validation

from pydantic import BaseModel
from typing import List, Optional, Any

class UnitBase(BaseModel):
    name: str
    is_active: bool = True
    category: str = "General"

class UnitResponse(UnitBase):
    id: int
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
    student_id: int

class ChaosResponse(BaseModel):
    case_study: str

class UserPreferencesUpdate(BaseModel):
    sensory_mode: Optional[str] = None
    ai_persona: Optional[str] = None

# --- AI Models ---
class ChatMessage(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    prompt: str
    user_id: int
    history: List[ChatMessage] = []

class ChatResponse(BaseModel):
    response: str

class QuizRequest(BaseModel):
    unit_name: str
    user_id: int

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
    user_id: int
    timestamp: Any
