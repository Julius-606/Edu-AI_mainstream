
from google import genai
from google.genai import types
import time
import logging
import json
import os
import random
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger("AI_SERVICE")

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

if not GEMINI_API_KEYS:
    GEMINI_API_KEYS = [
        "AIzaSyDmvjVkFmt0RoTMNER8fYoIKfy7Pkw1sfo",
        "AIzaSyDgvt1qfR_IG-UN__WcOPj1hv5s1IVUWHY"
    ]

class AiService:
    def __init__(self):
        self.key_index = 0
        self.model_variants = [
            "gemini-flash-latest",
            "gemini-2.5-flash",
            "gemini-flash-lite-latest",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash-lite",
        ]
        self.logs = []

        if GEMINI_API_KEYS:
            self._create_client()
        else:
            logger.error("❌ No Gemini API keys found.")

    def _create_client(self):
        key = GEMINI_API_KEYS[self.key_index % len(GEMINI_API_KEYS)]
        self.client = genai.Client(api_key=key)

    def _rotate_key(self):
        if not GEMINI_API_KEYS: return
        self.key_index = (self.key_index + 1) % len(GEMINI_API_KEYS)
        self._create_client()
        logger.info(f"🔄 Swapped to API Key Index: {self.key_index % len(GEMINI_API_KEYS)}")

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

                    if "404" in err_msg:
                        logger.warning(f"⚠️ Model {variant} not found. Trying next variant...")
                        break

                    self._rotate_key()
                    time.sleep(1)
                    continue
        return None

    def generate_quiz(self, unit_name, student_level, topic=None):
        if not GEMINI_API_KEYS: return None

        num_questions = random.randint(7, 12)
        task_name = f"Quiz: {unit_name}"
        focus_clause = f" specifically focusing on '{topic}'" if topic else ""
        prompt = f"""
        Generate a {num_questions}-question rigorous academic multiple choice quiz for the unit: '{unit_name}'{focus_clause}.
        Level: {student_level}.

        CRITICAL INSTRUCTIONS:
        1. Tone: Professional, academic, and clinical. Avoid overly casual language.
        2. Content: Focus on high-yield medical concepts, pathophysiology, and diagnostic criteria relevant to the topic.
        3. Explanations: For each question, the 'explanation' field must provide a deep clinical rationale.
           It should explain the physiological basis for the correct answer and clarify why the distractors are incorrect or less appropriate.

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
        performance_summary = ""
        for q in quiz_history:
            performance_summary += f"- {q.unit_name}: {q.pnl}% score\n"

        chat_context = ", ".join(recent_chat_titles)

        timetable_continuity = ""
        if previous_timetable:
            timetable_continuity = f"Previous Timetable Context:\n{json.dumps(previous_timetable)}\n"

        prompt = f"""
        Generate a dynamic weekly study timetable for {user_info['username']}.
        Current Level: {user_info['semester_status']}
        Active Units: {', '.join(active_units)}

        Performance Context:
        {performance_summary if performance_summary else "No assessments taken yet."}

        Recent Consultation Topics:
        {chat_context if chat_context else "No recent consultations."}

        {timetable_continuity}

        Format:
        {{
          "weekly_plan": [
            {{ "day": "Monday", "time": "09:00 - 10:30", "activity": "Intensive Study: [Unit]", "unit": "[Unit]", "type": "Study" }},
            ...
          ],
          "ai_brief": "Rationale..."
        }}
        """

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

    def get_recommendations(self, user_info, quiz_history, active_units):
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

        Provide a concise study recommendation (max 3 sentences).
        """

        return self.ask(prompt)

ai_service = AiService()


 