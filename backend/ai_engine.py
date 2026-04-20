import google.generativeai as genai
import time
import logging
import re
import json
import os
import random
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

class AiEngine:
    def __init__(self):
        self.key_index = 0
        # Recommended sure-bet models
        self.model_variants = ["gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-flash-latest"]
        self.logs = [] # Internal store for recent activities

        if GEMINI_API_KEYS:
            self._configure_genai()
        else:
            logger.error("❌ No Gemini API keys found in environment variables.")

    def _configure_genai(self):
        key = GEMINI_API_KEYS[self.key_index % len(GEMINI_API_KEYS)]
        genai.configure(api_key=key)

    def _rotate_key(self):
        if not GEMINI_API_KEYS: return
        self.key_index = (self.key_index + 1) % len(GEMINI_API_KEYS)
        self._configure_genai()

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
            for _ in range(len(GEMINI_API_KEYS)):
                start_time = time.time()
                current_key_idx = self.key_index % len(GEMINI_API_KEYS)
                try:
                    model = genai.GenerativeModel(
                        model_name=variant,
                        system_instruction=system_instruction
                    )
                    response = model.generate_content(prompt)

                    if response and response.text:
                        duration = time.time() - start_time
                        self._log_performance(variant, current_key_idx, duration, "SUCCESS", task_name)
                        return response.text
                except Exception as e:
                    duration = time.time() - start_time
                    err_msg = str(e).lower()
                    self._log_performance(variant, current_key_idx, duration, "FAILED", task_name)

                    if any(x in err_msg for x in ["429", "quota", "limit", "401", "403", "expired", "permission", "invalid"]):
                        self._rotate_key()
                        time.sleep(0.5)
                        continue
                    else:
                        break
        return None

    def generate_quiz(self, unit_name, student_level, topic=None):
        if not GEMINI_API_KEYS: return None

        num_questions = random.randint(7, 12)
        task_name = f"Quiz: {unit_name}"
        focus_clause = f" specifically focusing on '{topic}'" if topic else ""
        prompt = f"""
        Generate a {num_questions}-question multiple choice quiz for the unit: '{unit_name}'{focus_clause}.
        Level: {student_level}.

        CRITICAL INSTRUCTION: For each question, the 'explanation' field must be comprehensive.
        It should not only explain why the correct answer is right but also specifically address common misconceptions
        related to the wrong options (why they are incorrect in this context).

        Make the questions fun, engaging, and a little bit creative while remaining educational.
        Return ONLY valid JSON.
        Format:
        {{
          "quiz_title": "{unit_name} Fun Assessment",
          "questions": [
            {{
              "question_text": "...",
              "options": ["A", "B", "C", "D"],
              "correct_option_index": 0,
              "explanation": "CORRECT RATIONALE: ... WRONG OPTION ANALYSIS: ..."
            }}
          ]
        }}
        """

        for variant in self.model_variants:
            for _ in range(len(GEMINI_API_KEYS)):
                start_time = time.time()
                current_key_idx = self.key_index % len(GEMINI_API_KEYS)
                try:
                    model = genai.GenerativeModel(model_name=variant)
                    generation_config = None
                    if "1.5" in variant:
                        generation_config = {"response_mime_type": "application/json"}

                    response = model.generate_content(prompt, generation_config=generation_config)

                    if response and response.text:
                        raw_text = response.text.strip()
                        if raw_text.startswith("```json"):
                            raw_text = raw_text.replace("```json", "", 1).rsplit("```", 1)[0].strip()
                        elif raw_text.startswith("```"):
                            raw_text = raw_text.replace("```", "", 1).rsplit("```", 1)[0].strip()

                        duration = time.time() - start_time
                        self._log_performance(variant, current_key_idx, duration, "SUCCESS", task_name)
                        return json.loads(raw_text)
                except Exception as e:
                    duration = time.time() - start_time
                    self._log_performance(variant, current_key_idx, duration, "FAILED", task_name)
                    self._rotate_key()
                    time.sleep(0.5)
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

        for variant in self.model_variants:
            try:
                model = genai.GenerativeModel(model_name=variant)
                response = model.generate_content(prompt)
                if response and response.text:
                    raw_text = response.text.strip()
                    if "```json" in raw_text:
                        raw_text = raw_text.split("```json")[1].split("```")[0].strip()
                    return json.loads(raw_text)
            except:
                continue
        return None

    def get_recommendations(self, user_info, quiz_history, active_units):
        if not GEMINI_API_KEYS: return "AI Guidance unavailable."

        history_summary = ""
        for q in quiz_history:
            history_summary += f"- {q.unit_name}: {q.pnl}% score\n"

        prompt = f"""
        Student: {user_info['username']}
        Persona: {user_info['ai_persona']}
        Level: {user_info['semester_status']}
        Active Units: {', '.join(active_units)}
        Recent Performance:
        {history_summary if history_summary else "No assessments taken yet."}

        Based on the above, provide a concise (max 3 sentences) study strategy or recommendation.
        Act as the assigned AI Persona. Focus on specific units or areas of improvement.
        """

        return self.ask(prompt)

ai_engine = AiEngine()
