import google.generativeai as genai
import os
import time
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# --- 🔐 SECURE KEYCHAIN ---
GEMINI_API_KEYS = [
    os.getenv("GEMINI_API_KEY_1"),
    os.getenv("GEMINI_API_KEY_2"),
    os.getenv("GEMINI_API_KEY_3"),
    os.getenv("GEMINI_API_KEY_4")
]
GEMINI_API_KEYS = [key for key in GEMINI_API_KEYS if key]

class ModelTester:
    def __init__(self):
        self.key_index = 0
        self.models = []
        if not GEMINI_API_KEYS:
            print("❌ No Gemini API keys found in environment variables.")
            exit(1)
        self._configure_genai()

    def _configure_genai(self):
        key = GEMINI_API_KEYS[self.key_index % len(GEMINI_API_KEYS)]
        genai.configure(api_key=key)

    def _rotate_key(self):
        self.key_index = (self.key_index + 1) % len(GEMINI_API_KEYS)
        self._configure_genai()
        print(f"🔄 Rotated to Key Index: {self.key_index}")

    def discover_models(self):
        print("\n📡 Discovering available models...")
        try:
            available = []
            for m in genai.list_models():
                if 'generateContent' in m.supported_generation_methods:
                    name = m.name.split('/')[-1]
                    available.append(name)
            self.models = sorted(available)
        except Exception as e:
            print(f"⚠️ Failed to list models: {e}")
            self.models = ["gemini-pro", "gemini-1.5-flash", "gemini-2.0-flash"] # Fallbacks

    def show_menu(self):
        print("\n" + "="*50)
        print("🤖 EDU-AI MODEL TESTER")
        print("="*50)
        for i, model in enumerate(self.models):
            print(f"[{i}] {model}")
        print(f"[{len(self.models)}] 🔄 REFRESH MODEL LIST")
        print(f"[{len(self.models) + 1}] ❌ EXIT")
        print("="*50)

    def test_model(self, model_name, prompt):
        print(f"\n🚀 Sending prompt to '{model_name}' using current key rotation...")

        # Try every key for the selected model
        for attempt in range(len(GEMINI_API_KEYS)):
            try:
                model = genai.GenerativeModel(model_name=model_name)
                response = model.generate_content(prompt)
                if response and response.text:
                    print(f"\n✅ RESPONSE FROM {model_name}:")
                    print("-" * 30)
                    print(response.text)
                    print("-" * 30)
                    return True
            except Exception as e:
                err_msg = str(e).lower()
                print(f"⚠️ Attempt {attempt+1} failed with key index {self.key_index}: {err_msg}")
                if any(x in err_msg for x in ["429", "quota", "limit", "401", "403", "expired", "permission", "invalid"]):
                    self._rotate_key()
                    continue
                else:
                    print("❌ Non-rotational error encountered. Skipping further attempts.")
                    break

        print(f"❌ Failed to get a response from {model_name} after trying all available keys.")
        return False

def main():
    tester = ModelTester()
    tester.discover_models()

    while True:
        tester.show_menu()
        choice = input(f"Enter option [0-{len(tester.models) + 1}]: ").strip()

        if not choice.isdigit():
            print("Invalid input. Please enter a number.")
            continue

        idx = int(choice)

        if idx == len(tester.models):
            tester.discover_models()
            continue

        if idx == len(tester.models) + 1:
            print("Goodbye! 👋")
            break

        if 0 <= idx < len(tester.models):
            selected_model = tester.models[idx]
            prompt = input(f"\nEnter prompt for {selected_model}: ").strip()
            if prompt:
                tester.test_model(selected_model, prompt)
            else:
                print("Empty prompt. Aborting test.")
        else:
            print("Choice out of range.")

if __name__ == "__main__":
    main()
