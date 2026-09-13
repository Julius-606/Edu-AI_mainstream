from google import genai
from google.genai import types
import time
import logging
import re
import json
import os
import random
import threading
from datetime import datetime
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Setup Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("AI_ENGINE")

MARKDOWN_FORMAT_INSTRUCTION = (
    "Return the response as Markdown. Use headings, short paragraphs, bullet or numbered "
    "lists, bold/italic emphasis, tables when useful, fenced code blocks for code, and "
    "Markdown links where appropriate. Do not return HTML."
)

# --- 🔐 SECURE KEYCHAIN ---
GEMINI_API_KEYS = []
i = 1
while True:
    key = os.getenv(f"GEMINI_API_KEY_{i}")
    if not key:
        if i == 1:
            key = os.getenv("GEMINI_API_KEY")
            if key:
                GEMINI_API_KEYS.append(key)
        break
    GEMINI_API_KEYS.append(key)
    i += 1

# Fallback keys if none found (Safety Net)
if not GEMINI_API_KEYS:
    GEMINI_API_KEYS = [
        "AIzaSyDmvjVkFmt0RoTMNER8fYoIKfy7Pkw1sfo",
        "AIzaSyDgvt1qfR_IG-UN__WcOPj1hv5s1IVUWHY"
    ]

class AiEngine:
    def __init__(self):
        self.key_index = 0
        self.lock = threading.Lock()
        # Recommended sure-bet models
        # Production-safe model order: the active Gemini API reports that
        # gemini-3.5-flash is the only variant returning 200s in this env.
        self.model_variants = [
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-1.5-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-pro"
        ]
        self.logs = [] # Internal store for recent activities

        if GEMINI_API_KEYS:
            self._create_client()
        else:
            logger.error("❌ No Gemini API keys found.")

    def _create_client(self):
        key = GEMINI_API_KEYS[self.key_index % len(GEMINI_API_KEYS)]
        self.client = genai.Client(api_key=key)

    def _rotate_key(self):
        if not GEMINI_API_KEYS: return
        with self.lock:
            self.key_index = (self.key_index + 1) % len(GEMINI_API_KEYS)
            self._create_client()
            logger.info(f"🔄 Swapped to API Key Index: {self.key_index}")

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

    def ask(self, prompt, system_instruction=None):
        if not GEMINI_API_KEYS: return None

        task_name = "Chat/General"
        for variant in self.model_variants:
            # Try each key for each model variant
            for _ in range(len(GEMINI_API_KEYS)):
                start_time = time.time()
                current_key_idx = self.key_index % len(GEMINI_API_KEYS)
                try:
                    config = None
                    if system_instruction:
                        config = types.GenerateContentConfig(system_instruction=system_instruction)

                    response = self.client.models.generate_content(
                        model=variant,
                        contents=prompt,
                        config=config
                    )

                    if response and response.text:
                        duration = time.time() - start_time
                        self._log_performance(variant, current_key_idx, duration, "SUCCESS", task_name)
                        return response.text
                except Exception as e:
                    duration = time.time() - start_time
                    err_msg = str(e).lower()
                    self._log_performance(variant, current_key_idx, duration, "FAILED", task_name)

                    # If it's a 404, this specific model name is bad for this API, move to next variant
                    if "404" in err_msg:
                        logger.warning(f"⚠️ Model {variant} not found. Trying next variant...")
                        break

                    # For quota or auth issues, rotate key and retry SAME variant
                    self._rotate_key()
                    time.sleep(1) # Small backoff
                    continue
        return None

    def generate_quiz(self, unit_name, student_level, topic=None, subtopic=None):
        if not GEMINI_API_KEYS: return None

        num_questions = random.randint(7, 12)
        task_name = f"Quiz: {unit_name}"

        focus_context = ""
        if subtopic:
            focus_context = f" specifically focusing on the subtopic '{subtopic}' within '{topic}'"
        elif topic:
            focus_context = f" specifically focusing on the topic '{topic}'"

        prompt = f"""
        Generate a {num_questions}-question rigorous academic multiple choice quiz for the unit: '{unit_name}'{focus_context}.
        Level: {student_level}.

        CRITICAL INSTRUCTIONS:
        1. Tone: Professional, academic, and clinical. Avoid overly casual language.
        2. Content: Focus on high-yield medical concepts, pathophysiology, and diagnostic criteria relevant to the topic.
        3. Explanations: For each question, the 'explanation' field must provide a deep clinical rationale.
           It should explain the physiological basis for the correct answer and clarify why the distractors are incorrect or less appropriate.
        4. Return ONLY valid JSON; do not wrap the JSON in Markdown.

        Format:
        Return ONLY valid JSON.
        {{
          "quiz_title": "{unit_name} Advanced Assessment",
          "questions": [
            {{
              "question_text": "...",
              "options": ["A", "B", "C", "D"],
              "correct_option_index": 0,
              "explanation": "CLINICAL RATIONALE: ... DIFFERENTIAL ANALYSIS: ..."
            }}
          ]
        }}
        """

        for variant in self.model_variants:
            for _ in range(len(GEMINI_API_KEYS)):
                start_time = time.time()
                current_key_idx = self.key_index % len(GEMINI_API_KEYS)
                try:
                    config = types.GenerateContentConfig(
                        response_mime_type="application/json"
                    )

                    response = self.client.models.generate_content(
                        model=variant,
                        contents=prompt,
                        config=config
                    )

                    if response and response.text:
                        raw_text = response.text.strip()
                        # Clean markdown if present
                        if "```json" in raw_text:
                            raw_text = raw_text.split("```json")[1].split("```")[0].strip()

                        duration = time.time() - start_time
                        self._log_performance(variant, current_key_idx, duration, "SUCCESS", task_name)
                        return json.loads(raw_text)
                except Exception as e:
                    duration = time.time() - start_time
                    err_msg = str(e).lower()
                    self._log_performance(variant, current_key_idx, duration, "FAILED", task_name)

                    if "404" in err_msg:
                        break

                    self._rotate_key()
                    time.sleep(1)
                    continue
        return None

    def generate_timetable(self, user_info, quiz_history, active_units, recent_chat_titles, previous_timetable=None):
        if not GEMINI_API_KEYS: return None

        performance_summary = ""
        for q in quiz_history:
            performance_summary += f"- {q.unit_name}: {q.pnl}% score\n"

        chat_context = ", ".join(recent_chat_titles)

        timetable_continuity = ""
        if previous_timetable:
            timetable_continuity = f"Previous Timetable Context (Ensure continuity and avoid unnecessary repetition unless needed for revision):\n{json.dumps(previous_timetable)}\n"

        prompt = f"""
        Generate a dynamic weekly study timetable for {user_info['username']}.
        Current Level: {user_info['semester_status']}
        Active Units: {', '.join(active_units)}

        Performance Context:
        {performance_summary if performance_summary else "No assessments taken yet."}

        Recent Consultation Topics (What the student has been up to):
        {chat_context if chat_context else "No recent consultations."}

        {timetable_continuity}

        The timetable should prioritize units with lower quiz scores or topics discussed in recent consultations.
        It must include:
        - Study sessions (intensive focus)
        - Revision (spaced repetition)
        - Assessment (quiz prep)
        - Breaks (essential for cognitive rest)

        Return ONLY a JSON object in this format:
        {{
          "weekly_plan": [
            {{ "day": "Monday", "time": "09:00 - 10:30", "activity": "Intensive Study: [Unit]", "unit": "[Unit]", "type": "Study" }},
            ...
          ],
          "ai_brief": "A 1-2 sentence rationale for this specific layout based on their current needs and how it follows/improves upon the previous week's plan."
        }}
        """

        # Using ask as a wrapper for better rotation/variant handling
        response = self.ask(prompt)
        if response:
            try:
                raw_text = response.strip()
                if "```json" in raw_text:
                    raw_text = raw_text.split("```json")[1].split("```")[0].strip()
                return json.loads(raw_text)
            except:
                logger.error("Failed to parse timetable JSON")
        return None

    def get_recommendations(self, user_info, quiz_history, active_units, current_progress=None):
        if not GEMINI_API_KEYS: return "AI Guidance unavailable."

        history_summary = ""
        for q in quiz_history:
            history_summary += f"- {q.unit_name}: {q.pnl}% score\n"

        progress_context = ""
        if current_progress:
            progress_context = f"Current Progress Data:\n{json.dumps(current_progress)}\n"

        prompt = f"""
        Student: {user_info['username']}
        Persona: {user_info['ai_persona']}
        Level: {user_info['semester_status']}
        Active Units: {', '.join(active_units)}

        {progress_context}

        Recent Performance:
        {history_summary if history_summary else "No assessments taken yet."}

        Based on the above hierarchy and performance, provide a concise (max 3 sentences) study strategy.
        Act as the assigned AI Persona. Identify exactly which Module or Topic they should focus on next.
        {MARKDOWN_FORMAT_INSTRUCTION}
        """

        return self.ask(prompt, system_instruction=MARKDOWN_FORMAT_INSTRUCTION)

ai_engine = AiEngine()
