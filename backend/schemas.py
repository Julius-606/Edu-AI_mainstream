# IDENTITY: backend/schemas.py
# VERSION: 1.1.0
# ⚙️ Pydantic Models for Data Validation

from pydantic import BaseModel
from typing import List, Optional, Dict

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
    student_id: int # Needed to tailor the prompt to their history

class ChaosResponse(BaseModel):
    case_study: str

class UserPreferencesUpdate(BaseModel):
    sensory_mode: Optional[str] = None
    ai_persona: Optional[str] = None