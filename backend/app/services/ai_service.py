
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

    def generate_timetable(self, user_info, quiz_history, active_units, recent_chat_titles, previous_timetable=None, study_context=None):
        performance_summary = ""
        for q in quiz_history:
            score_val = getattr(q, 'pnl', None) or getattr(q, 'score', 0)
            unit_name = getattr(q, 'unit_name', 'General')
            performance_summary += f"- {unit_name}: {score_val}% score\n"

        chat_context = ", ".join(recent_chat_titles)

        timetable_continuity = ""
        if previous_timetable:
            timetable_continuity = f"Previous Timetable Context:\n{json.dumps(previous_timetable)}\n"

        detailed_context = ""
        if study_context:
            sc_dict = study_context if isinstance(study_context, dict) else study_context.dict()
            weak = sc_dict.get("weak_topics", [])
            mastered = sc_dict.get("mastered_topics", [])
            pending = sc_dict.get("pending_subtopic_names", [])
            completed = sc_dict.get("completed_subtopic_names", [])
            avg_score = sc_dict.get("average_quiz_score")
            overall_progress = sc_dict.get("overall_progress_percentage")

            detailed_context = f"""
            REAL-TIME STUDENT LEARNING & PERFORMANCE METRICS:
            - Overall Syllabus Completion: {overall_progress if overall_progress is not None else 'N/A'}%
            - Average Assessment Score: {avg_score if avg_score is not None else 'N/A'}%
            - Critical Weak Areas (Assessment accuracy < 70%): {', '.join(weak) if weak else 'None detected yet'}
            - Mastered High-Yield Concepts (Assessment accuracy >= 80%): {', '.join(mastered) if mastered else 'Building mastery'}
            - High-Priority Pending Subtopics: {', '.join(pending[:6]) if pending else 'Follow core syllabus'}
            - Recently Completed Subtopics: {', '.join(completed[:4]) if completed else 'None'}
            """

        prompt = f"""
        Generate a dynamic, hyper-personalized weekly study timetable for {user_info['username']}.
        Current Level: {user_info['semester_status']}
        Active Units: {', '.join(active_units)}

        {detailed_context}

        Performance History:
        {performance_summary if performance_summary else "No assessments taken yet."}

        Recent Consultation Topics:
        {chat_context if chat_context else "No recent consultations."}

        {timetable_continuity}

        CRITICAL REQUIREMENT:
        You MUST generate exactly 3 to 4 sequential study slots/activities per day for EACH day of the week (Monday through Sunday).
        Directly align the slots with the student's real-time metrics:
        1. Schedule morning 'Deep Study' sessions targeting their pending subtopics or foundational concepts.
        2. Schedule afternoon 'Diagnostic Assessment' and 'Targeted Revision' sessions specifically addressing their WEAK topics to convert weaknesses into exam-ready strengths.
        3. Include scheduled mental calibration and synthesis breaks.
        Do NOT just output one task per day. Fill the schedule of each day with 3-4 items.

        Format:
        {{
          "weekly_plan": [
            {{ "day": "Monday", "time": "08:30 - 10:30", "activity": "Deep Study: Core Pathophysiology", "unit": "Biochemistry II", "type": "Study" }},
            {{ "day": "Monday", "time": "11:00 - 12:00", "activity": "Targeted Assessment: Diagnostic Traps", "unit": "General Surgery", "type": "Assessment" }},
            {{ "day": "Monday", "time": "12:00 - 13:00", "activity": "Socratic Mental Calibration Break", "unit": null, "type": "Break" }},
            {{ "day": "Monday", "time": "14:30 - 16:30", "activity": "Differential Case Study & Revision", "unit": "Internal Medicine", "type": "Revision" }},
            ...
          ],
          "ai_brief": "A customized clinical rationale explaining how this week's plan tackles their specific weak topics and advances their pending subtopics..."
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

    def get_recommendations(self, user_info, quiz_history, active_units, study_context=None):
        history_summary = ""
        for q in quiz_history:
            score_val = getattr(q, 'pnl', None) or getattr(q, 'score', 0)
            unit_name = getattr(q, 'unit_name', 'General')
            history_summary += f"- {unit_name}: {score_val}% score\n"

        detailed_context = ""
        if study_context:
            sc_dict = study_context if isinstance(study_context, dict) else study_context.dict()
            weak = sc_dict.get("weak_topics", [])
            mastered = sc_dict.get("mastered_topics", [])
            pending = sc_dict.get("pending_subtopic_names", [])
            completed = sc_dict.get("completed_subtopic_names", [])
            avg_score = sc_dict.get("average_quiz_score")
            overall_progress = sc_dict.get("overall_progress_percentage")

            detailed_context = f"""
            REAL-TIME LEARNING & QUIZ PERFORMANCE METRICS:
            - Syllabus Progress: {overall_progress if overall_progress is not None else 'N/A'}%
            - Mean Assessment Score: {avg_score if avg_score is not None else 'N/A'}%
            - Verified Mastered Topics: {', '.join(mastered) if mastered else 'Consolidating knowledge'}
            - Critical Weak Areas (Quiz Score < 70%): {', '.join(weak) if weak else 'No acute deficiencies'}
            - Next Pending Subtopics to Unlock: {', '.join(pending[:4]) if pending else 'Syllabus on track'}
            """

        prompt = f"""
        You are Zenith AI, the elite Socratic academic mentor for {user_info['username']}.
        Persona: {user_info.get('ai_persona', 'Socratic Mentor')}
        Level: {user_info['semester_status']}
        Active Units: {', '.join(active_units)}

        {detailed_context}

        Assessment History:
        {history_summary if history_summary else "No assessments taken yet."}

        INSTRUCTIONS:
        Formulate a punchy, highly motivating, and academically precise 3-sentence Zenith Insight for the student's dashboard.
        1. Sentence 1: Acknowledge their actual learning progress or a topic they demonstrated mastery in.
        2. Sentence 2: Directly call out their weakest area or a critical diagnostic pitfall/mechanism they must review immediately based on their quiz data.
        3. Sentence 3: Prescribe the exact next actionable subtopic they should conquer today.
        Avoid vague platitudes. Speak directly to their real academic data!
        """

        return self.ask(prompt)

ai_service = AiService()


 