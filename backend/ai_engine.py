import google.generativeai as genai
import time
import logging
import re
import json
import os
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
            logger.info(f"🚀 AI Engine initialized with {len(GEMINI_API_KEYS)} keys.")
        else:
            logger.error("❌ No Gemini API keys found in environment variables.")

    def _configure_genai(self):
        key = GEMINI_API_KEYS[self.key_index % len(GEMINI_API_KEYS)]
        genai.configure(api_key=key)

    def _rotate_key(self):
        if not GEMINI_API_KEYS: return
        self.key_index = (self.key_index + 1) % len(GEMINI_API_KEYS)
        self._configure_genai()
        logger.info(f"🔄 Rotated to Key Index: {self.key_index % len(GEMINI_API_KEYS)}")

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

        # ANSI Colors for terminal visibility
        color = "\033[92m" if status == "SUCCESS" else "\033[91m"
        reset = "\033[0m"
        logger.info(f"{color}[{status}]{reset} Task: {task} | Model: {model} | Key: #{key_idx} | Time: {duration:.2f}s")

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

        task_name = f"Quiz: {unit_name}"
        focus_clause = f" specifically focusing on '{topic}'" if topic else ""
        prompt = f"""
        Generate a 5-question multiple choice quiz for the unit: '{unit_name}'{focus_clause}.
        Level: {student_level}.
        Return ONLY valid JSON.
        Format:
        {{
          "quiz_title": "{unit_name} Assessment",
          "questions": [
            {{
              "question_text": "...",
              "options": ["A", "B", "C", "D"],
              "correct_option_index": 0,
              "explanation": "..."
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
