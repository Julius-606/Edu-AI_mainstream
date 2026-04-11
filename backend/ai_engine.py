import google.generativeai as genai
import time
import logging
import re
import json
import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Setup Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# --- 🔐 SECURE KEYCHAIN ---
GEMINI_API_KEYS = [
    os.getenv("GEMINI_API_KEY_1"),
    os.getenv("GEMINI_API_KEY_2"),
    os.getenv("GEMINI_API_KEY_3"),
    os.getenv("GEMINI_API_KEY_4")
]
GEMINI_API_KEYS = [key for key in GEMINI_API_KEYS if key]

class AiEngine:
    def __init__(self):
        self.key_index = 0
        # 🎯 FIXATED: Start with the proven winner.
        # Fallbacks are kept at the end for extreme safety.
        self.model_variants = ["gemini-2.0-flash", "gemini-1.5-flash", "gemini-pro"]

        if GEMINI_API_KEYS:
            self._configure_genai()
            self._discover_models()
        else:
            logger.error("❌ No Gemini API keys found in environment variables.")

    def _configure_genai(self):
        key = GEMINI_API_KEYS[self.key_index % len(GEMINI_API_KEYS)]
        genai.configure(api_key=key)
        logger.info(f"🔑 AI Engine configured with key index {self.key_index % len(GEMINI_API_KEYS)}")

    def _discover_models(self):
        """🚀 Dynamically discovers available models without overwriting our fixated preference."""
        try:
            available = []
            for m in genai.list_models():
                if 'generateContent' in m.supported_generation_methods:
                    name = m.name.split('/')[-1]
                    available.append(name)

            gemini_only = [m for m in available if 'gemini' in m.lower()]

            if gemini_only:
                # Keep our preferred order, but add any new ones found as extra fallbacks
                current_set = set(self.model_variants)
                new_discoveries = [m for m in gemini_only if m not in current_set]
                self.model_variants += new_discoveries
                logger.info(f"📡 Dynamic Discovery added: {new_discoveries}")
                logger.info(f"📊 Final Model Priority: {self.model_variants}")
            else:
                logger.warning("⚠️ Discovery found no Gemini models. Using defaults.")
        except Exception as e:
            logger.warning(f"⚠️ Model discovery failed: {e}. Keeping defaults.")

    def _rotate_key(self):
        if not GEMINI_API_KEYS: return
        self.key_index = (self.key_index + 1) % len(GEMINI_API_KEYS)
        self._configure_genai()

    def ask(self, prompt, system_instruction=None):
        if not GEMINI_API_KEYS: return None

        # Logic: Try each model one by one. For each model, try ALL keys before giving up.
        for variant in self.model_variants:
            for _ in range(len(GEMINI_API_KEYS)):
                try:
                    model = genai.GenerativeModel(
                        model_name=variant,
                        system_instruction=system_instruction
                    )
                    response = model.generate_content(prompt)
                    if response and response.text:
                        return response.text
                except Exception as e:
                    err_msg = str(e).lower()
                    logger.warning(f"⚠️ {variant} failed with key index {self.key_index % len(GEMINI_API_KEYS)}: {err_msg}")

                    # If key is exhausted, blocked, or invalid -> Rotate to next key and RETRY same model
                    if any(x in err_msg for x in ["429", "quota", "limit", "401", "403", "expired", "permission", "invalid"]):
                        self._rotate_key()
                        time.sleep(0.5)
                        continue
                    else:
                        # If the error is model-specific (like 404), break to try next model variant
                        break
        return None

    def generate_quiz(self, unit_name, student_level):
        if not GEMINI_API_KEYS: return None

        prompt = f"""
        Generate a 5-question multiple choice quiz for the unit: '{unit_name}'.
        Level: {student_level}.
        Return ONLY valid JSON.
        Format:
        {{
          "quiz_title": "Title",
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
                try:
                    model = genai.GenerativeModel(model_name=variant)
                    config = None
                    if any(v in variant for v in ["1.5", "2.0", "exp"]):
                        config = genai.types.GenerationConfig(response_mime_type="application/json")

                    response = model.generate_content(prompt, generation_config=config)

                    if response and response.text:
                        raw_text = response.text.strip()
                        if raw_text.startswith("```json"):
                            raw_text = raw_text.replace("```json", "", 1).rsplit("```", 1)[0].strip()
                        elif raw_text.startswith("```"):
                            raw_text = raw_text.replace("```", "", 1).rsplit("```", 1)[0].strip()

                        return json.loads(raw_text)
                except Exception as e:
                    err_msg = str(e).lower()
                    logger.warning(f"⚠️ Quiz Gen Failed ({variant}) with key index {self.key_index % len(GEMINI_API_KEYS)}: {err_msg}")

                    # Rotate key and retry same model
                    self._rotate_key()
                    time.sleep(0.5)
                    continue
        return None

ai_engine = AiEngine()
