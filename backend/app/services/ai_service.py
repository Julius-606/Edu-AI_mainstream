from google import genai
from google.genai import types, errors
from pydantic import BaseModel
import time
import logging
import json
import os
import asyncio
from datetime import datetime
from typing import List, Optional
from app.core.config import load_runtime_environment

load_runtime_environment()
logger = logging.getLogger("AI_SERVICE")

MARKDOWN_FORMAT_INSTRUCTION = (
    "Return the response as Markdown. Use headings, short paragraphs, bullet or numbered "
    "lists, bold/italic emphasis, tables when useful, fenced code blocks for code, and "
    "Markdown links where appropriate. Do not return HTML."
)

LEARNING_RESOURCE_INSTRUCTION = (
    "When useful for this exact objective and learner context, finish with a "
    "'Further Learning' section containing 1-3 relevant Markdown links. Prefer "
    "authoritative educational sources such as universities, government health agencies, "
    "WHO, CDC, NCBI, or OpenStax. Only include URLs you are confident are real and "
    "relevant; do not invent deep links, paper identifiers, or page paths. If you are "
    "not confident in a direct page URL, link to a trustworthy site search using a "
    "properly URL-encoded query instead. Do not add links just to fill space."
)

# --- Key Loading Logic ---
GEMINI_API_KEYS = []
i = 1
while True:
    key = os.getenv(f"GEMINI_API_KEY_{i}")
    if not key:
        if i == 1:
            key = os.getenv("GEMINI_API_KEY")
            if key: GEMINI_API_KEYS.append(key)
        break
    GEMINI_API_KEYS.append(key)
    i += 1

if not GEMINI_API_KEYS:
    GEMINI_API_KEYS = [
        "AIzaSyDmvjVkFmt0RoTMNER8fYoIKfy7Pkw1sfo",
        "AIzaSyDgvt1qfR_IG-UN__WcOPj1hv5s1IVUWHY"
    ]

# --- Pydantic Models for Response Schemas ---
class QuizQuestion(BaseModel):
    question_text: str
    options: List[str]
    correct_option_index: int
    explanation: str

class QuizSchema(BaseModel):
    quiz_title: str
    questions: List[QuizQuestion]

class TimetableSlot(BaseModel):
    day: str
    time: str
    activity: str
    unit: Optional[str] = None
    type: str

class TimetableSchema(BaseModel):
    weekly_plan: List[TimetableSlot]
    ai_brief: str

class AiService:
    def __init__(self):
        # Create a separate client for every API key
        self.clients = [genai.Client(api_key=key) for key in GEMINI_API_KEYS]
        self.key_index = 0
        self.lock = asyncio.Lock() # Async lock for safe rotation

        # Keep the backend pinned to the model that is confirmed to work in
        # this environment before falling back to other variants.
        self.model_variants = [
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        ]
        self.logs = []
        logger.info(f"🔑 Configured {len(self.clients)} Gemini Clients for rotation.")

    async def _rotate_key(self):
        if not self.clients: return
        async with self.lock:
            self.key_index = (self.key_index + 1) % len(self.clients)
            logger.warning(f"🚨 Rotated to API Client Index: {self.key_index}")

    def _log_performance(self, model, key_idx, duration, status, task):
        log_entry = {
            "timestamp": datetime.now().strftime("%H:%M:%S"),
            "task": task,
            "model": model,
            "key_index": key_idx,
            "latency": f"{duration:.2f}s",
            "status": status
        }
        self.logs.append(log_entry)
        if len(self.logs) > 50: self.logs.pop(0)

    async def ask(self, prompt: str, system_instruction: str = None) -> str:
        if not self.clients: return None
        task_name = "Chat/General"

        config = types.GenerateContentConfig()
        if system_instruction:
            config.system_instruction = system_instruction

        for variant in self.model_variants:
            for _ in range(len(self.clients)):
                start_time = time.time()
                current_key_idx = self.key_index % len(self.clients)
                current_client = self.clients[current_key_idx]

                try:
                    response = await current_client.aio.models.generate_content(
                        model=variant,
                        contents=prompt,
                        config=config
                    )
                    duration = time.time() - start_time
                    self._log_performance(variant, current_key_idx, duration, "SUCCESS", task_name)
                    return response.text

                except Exception as e:
                    duration = time.time() - start_time
                    err_msg = str(e).lower()
                    logger.error(f"❌ AI Error with {variant} (Key {current_key_idx}): {str(e)}")
                    self._log_performance(variant, current_key_idx, duration, "FAILED", task_name)

                    if "404" in err_msg:
                        logger.warning(f"⚠️ Model {variant} not found. Skipping to next variant.")
                        break # Break inner loop, try next model variant

                    if "429" in err_msg or "quota" in err_msg:
                        logger.warning(f"🚨 Rate limit hit for {variant}. Rotating key...")
                        await self._rotate_key()
                        await asyncio.sleep(2)
                        continue # Retry SAME model with NEW key

                    # For other unknown errors, rotate and retry
                    await self._rotate_key()
                    await asyncio.sleep(1)
                    continue

        return None

    async def generate_quiz(self, unit_name: str, student_level: str, topic: str = None):
        if not self.clients: return None
        task_name = f"Quiz: {unit_name}"
        focus_clause = f" specifically focusing on '{topic}'" if topic else ""

        prompt = f"""
        Generate a rigorous academic multiple choice quiz for the unit: '{unit_name}'{focus_clause}.
        Level: {student_level}. Moderate, half of the questions to be conceptual and half to be application medical concepts and pathophysiology.
        Provide deep rationale for each question.
        Return ONLY the JSON object required by the response schema; do not wrap it in Markdown.
        """

        config = types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=QuizSchema, # Guaranteed JSON structure
        )

        for variant in self.model_variants:
            for _ in range(len(self.clients)):
                start_time = time.time()
                current_key_idx = self.key_index % len(self.clients)
                current_client = self.clients[current_key_idx]

                try:
                    response = await current_client.aio.models.generate_content(
                        model=variant,
                        contents=prompt,
                        config=config
                    )
                    duration = time.time() - start_time
                    self._log_performance(variant, current_key_idx, duration, "SUCCESS", task_name)

                    return json.loads(response.text)

                except Exception as e:
                    duration = time.time() - start_time
                    err_msg = str(e).lower()
                    logger.error(f"❌ Quiz Error with {variant} (Key {current_key_idx}): {str(e)}")
                    self._log_performance(variant, current_key_idx, duration, "FAILED", task_name)

                    if "404" in err_msg:
                        break

                    await self._rotate_key()
                    await asyncio.sleep(1)
                    continue

        return None

    async def generate_timetable(self, user_info, quiz_history, active_units, recent_chat_titles, previous_timetable=None):
        if not self.clients: return None
        task_name = "Timetable"

        performance_summary = ""
        for q in quiz_history:
            performance_summary += f"- {q.unit_name}: {q.pnl}% score\n"

        chat_context = ", ".join(recent_chat_titles)
        timetable_continuity = f"Previous Context: {json.dumps(previous_timetable)}\n" if previous_timetable else ""

        prompt = f"""
        Generate a dynamic weekly study timetable for {user_info['username']}.
        Active Units: {', '.join(active_units)}
        Performance: {performance_summary if performance_summary else "No assessments."}
        Recent Topics: {chat_context if chat_context else "No recent consultations."}
        {timetable_continuity}
        """

        config = types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=TimetableSchema,
        )

        for variant in self.model_variants:
            for _ in range(len(self.clients)):
                start_time = time.time()
                current_key_idx = self.key_index % len(self.clients)
                current_client = self.clients[current_key_idx]

                try:
                    response = await current_client.aio.models.generate_content(
                        model=variant,
                        contents=prompt,
                        config=config
                    )
                    duration = time.time() - start_time
                    self._log_performance(variant, current_key_idx, duration, "SUCCESS", task_name)
                    return json.loads(response.text)
                except Exception as e:
                    duration = time.time() - start_time
                    err_msg = str(e).lower()
                    self._log_performance(variant, current_key_idx, duration, "FAILED", task_name)

                    if "404" in err_msg:
                        break

                    await self._rotate_key()
                    await asyncio.sleep(1)
                    continue
        return None

    async def get_recommendations(self, user_info, quiz_history, active_units):
        history_summary = ""
        for q in quiz_history:
            history_summary += f"- {q.unit_name}: {q.pnl}% score\n"

        prompt = f"""
        Student: {user_info['username']}
        Persona: {user_info['ai_persona']}
        Level: {user_info['semester_status']}
        Active Units: {', '.join(active_units)}
        Recent Performance: {history_summary}

        Provide a concise study recommendation (max 3 sentences).
        {MARKDOWN_FORMAT_INSTRUCTION}
        """

        return await self.ask(prompt, system_instruction=MARKDOWN_FORMAT_INSTRUCTION)

    async def generate_learning_content(self, objective_description, username, student_message: str = None, session_context: str = None):
        clean_objective = (objective_description or '').strip()
        clean_student_message = (student_message or '').strip()
        clean_context = (session_context or '').strip()

        if clean_student_message:
            prompt = f"""
            Objective: {clean_objective}
            Learner: {username}
            Student question / follow-up: {clean_student_message}
            Prior context: {clean_context if clean_context else 'No prior context.'}

            Teach this objective in a friendly, concise way. Use Markdown and keep the session coherent with the student's question.
            If prior context exists, do not start with salutations; otherwise continue directly with the explanation.
            {LEARNING_RESOURCE_INSTRUCTION}
            End with a 'Check for Understanding' question.
            """
        else:
            prompt = f"""
            Objective: {clean_objective}
            Learner: {username}
            Prior context: {clean_context if clean_context else 'No prior context.'}

            Generate an interactive learning session for this objective.
            Explain clearly, use Markdown, and keep the explanation focused and coherent.
            {LEARNING_RESOURCE_INSTRUCTION}
            End with a 'Check for Understanding' question.
            """
        return await self.ask(
            prompt,
            system_instruction=f"{MARKDOWN_FORMAT_INSTRUCTION} {LEARNING_RESOURCE_INSTRUCTION}"
        )

ai_service = AiService()
