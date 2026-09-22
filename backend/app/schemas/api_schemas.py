
from pydantic import BaseModel
from typing import List, Optional, Any, Dict

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

class LearningObjectiveResponse(BaseModel):
    id: int
    description: str
    is_completed: bool
    class Config:
        from_attributes = True

class SubtopicResponse(BaseModel):
    id: int
    name: str
    is_completed: bool
    learning_objectives: List[LearningObjectiveResponse] = []
    class Config:
        from_attributes = True

class TopicResponse(BaseModel):
    id: int
    name: str
    subtopics: List[SubtopicResponse] = []
    class Config:
        from_attributes = True

class ModuleResponse(BaseModel):
    id: int
    name: str
    topics: List[TopicResponse] = []
    class Config:
        from_attributes = True

class UnitResponse(UnitBase):
    id: int
    owner_id: int
    modules: List[ModuleResponse] = []
    class Config:
        from_attributes = True

class QuizHistoryResponse(BaseModel):
    unit_name: str
    pnl: float
    timestamp: str

class ChatMessageResponse(BaseModel):
    role: str
    content: str
    timestamp: str

class DashboardResponse(BaseModel):
    username: str
    role: str
    sensory_mode: str
    semester_status: str
    difficulty: str
    ai_persona: str
    active_units: List[str]
    units: List[UnitResponse] = []
    average_pnl: float
    total_quizzes: int
    quiz_history: List[QuizHistoryResponse]
    chat_history: List[ChatMessageResponse]

class ChaosRequest(BaseModel):
    unit: str
    focus_area: Optional[str] = None
    difficulty: str = "Asian Parent Expectations (Extreme)"
    student_id: str

class ChaosResponse(BaseModel):
    case_study: str

class UserCreate(BaseModel):
    username: str
    email: str
    password: str
    role: str = "Student"
    sensory_mode: str = "Standard"
    difficulty: str = "Medium (Standard)"
    ai_persona: str = "Standard Trace"
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

# --- TEACHER PORTAL SCHEMAS ---

class StudentSummary(BaseModel):
    id: int
    username: str
    average_pnl: float
    total_quizzes: int
    semester_status: str
    active_units: List[str]
    is_at_risk: bool = False
    risk_reason: Optional[str] = None

class TeacherDashboardResponse(BaseModel):
    action_required_queue: List[StudentSummary]
    total_active_students: int
    class_health_score: float

class ClassReportResponse(BaseModel):
    report: str

# --- PARENT PORTAL SCHEMAS ---

class ParentDashboardResponse(BaseModel):
    student_name: str
    academic_status: str
    current_study_path: List[str]
    ai_progress_review: str
    teacher_remarks: Optional[str] = None
    recent_grades: List[QuizHistoryResponse]

# --- TIMETABLE SCHEMAS ---

class TimetableSlot(BaseModel):
    day: str
    time: str
    activity: str
    unit: Optional[str] = None
    type: str  # "Study", "Break", "Assessment", "Revision"

class TimetableResponse(BaseModel):
    weekly_plan: List[TimetableSlot]
    ai_brief: str

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

class LoginRequest(BaseModel):
    email: str
    password: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    username: str
    role: str

class RecommendationResponse(BaseModel):
    recommendation: str

# --- Course Objectives & Content Completion ---
class CourseObjectiveRequest(BaseModel):
    user_id: str
    objective_id: str

class AiGeneratedContentResponse(BaseModel):
    objective_id: str
    content_title: str
    generated_text: str
    related_content_ids: List[str] = []

class ContentCompletionDto(BaseModel):
    objective_id: str
    content_id: str
    is_completed: bool
    last_updated: float

class ContentCompletionSyncRequest(BaseModel):
    user_id: str
    completions: List[ContentCompletionDto]

class ContentCompletionSyncResponse(BaseModel):
    applied_completion_ids: List[str] = []
    failed_completion_ids: List[str] = []

class SyncOperationDto(BaseModel):
    operationId: str
    entityType: str
    entityId: Any = 0
    payload: Dict[str, Any] = {}

class SyncRequest(BaseModel):
    userId: str
    operations: List[SyncOperationDto] = []

class SyncResponse(BaseModel):
    appliedOperationIds: List[str] = []
    failedOperationIds: List[str] = []

class LearningContentRequest(BaseModel):
    objectiveId: int
    userId: str
    content: str

class ConnectionMessageRequest(BaseModel):
    recipient_id: int
    content: str



