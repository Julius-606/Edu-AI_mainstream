
import requests
import json
import time
import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parent / ".env")

# --- CONFIGURATION ---
BASE_URL = "http://127.0.0.1:8000"
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "DEVELOPMENT_KEY")
TEST_RUN_ID = time.time_ns()
TEST_USERNAME = f"LocalTester{TEST_RUN_ID}"
TEST_EMAIL = f"test+{TEST_RUN_ID}@example.com"
TEST_PASSWORD = "password123"
HEADERS = {
    "X-Internal-Api-Key": INTERNAL_API_KEY,
    "Content-Type": "application/json"
}

def log_response(name, response):
    print(f"\n--- {name} ---")
    print(f"Status: {response.status_code}")
    try:
        data = response.json()
        # Truncate large responses for readability
        if isinstance(data, dict) and "questions" in data:
            print(f"Quiz generated with {len(data['questions'])} questions.")
        elif isinstance(data, dict) and "weekly_plan" in data:
            print(f"Timetable generated with {len(data['weekly_plan'])} slots.")
        else:
            print(json.dumps(data, indent=2)[:1000])
    except:
        print(response.text[:200])

def run_tests():
    print("Starting Modular Mock App Call Suite...")

    # 1. Signup
    print("\n1. Testing Signup...")
    signup_data = {
        "username": TEST_USERNAME,
        "email": TEST_EMAIL,
        "password": TEST_PASSWORD,
        "role": "Student"
    }
    r = requests.post(f"{BASE_URL}/api/auth/signup-form", data=signup_data)
    if r.status_code == 200:
        print("Signup Success")
    elif r.status_code == 400:
        print("Signup: User already exists (continuing...)")
    else:
        print(f"Signup Failed: {r.status_code}")

    # 2. Login
    print("\n2. Testing Login...")
    login_payload = {"email": TEST_EMAIL, "password": TEST_PASSWORD}
    r = requests.post(f"{BASE_URL}/api/auth/login", json=login_payload, headers=HEADERS)
    if r.status_code == 200:
        token_data = r.json()
        token = token_data["access_token"]
        user_id = token_data["user_id"]
        HEADERS["Authorization"] = f"Bearer {token}"
        print(f"Login Success! User ID: {user_id}")
    else:
        log_response("Login Error", r)
        return

    # 3. Get User Profile
    print("\n3. Testing User Profile...")
    r = requests.get(f"{BASE_URL}/api/users/{user_id}", headers=HEADERS)
    log_response("User Profile", r)

    # 4. Get Dashboard
    print("\n4. Testing Dashboard...")
    r = requests.get(f"{BASE_URL}/api/users/{user_id}/dashboard", headers=HEADERS)
    log_response("Dashboard", r)

    # 5. AI Chat
    print("\n5. Testing AI Chat...")
    chat_payload = {
        "user_id": str(user_id), # Pydantic expects string in new schemas
        "prompt": "How does the heart pump blood?",
        "history": []
    }
    r = requests.post(f"{BASE_URL}/api/ai/chat", json=chat_payload, headers=HEADERS)
    log_response("AI Chat", r)

    # 6. Generate Quiz
    print("\n6. Testing Quiz Generation...")
    quiz_payload = {
        "user_id": str(user_id),
        "unit_name": "Cardiology"
    }
    r = requests.post(f"{BASE_URL}/api/ai/quiz", json=quiz_payload, headers=HEADERS)
    log_response("Quiz Generation", r)

    # 7. Get Recommendations
    print("\n7. Testing Recommendations...")
    r = requests.get(f"{BASE_URL}/api/ai/recommendations/{user_id}", headers=HEADERS)
    log_response("Recommendations", r)

    # 8. Get Timetable
    print("\n8. Testing Timetable...")
    r = requests.get(f"{BASE_URL}/api/users/{user_id}/timetable", headers=HEADERS)
    log_response("Timetable", r)

    # 9. Teacher Dashboard
    print("\n9. Testing Teacher Dashboard...")
    r = requests.get(f"{BASE_URL}/api/teacher/dashboard", headers=HEADERS)
    log_response("Teacher Dashboard", r)

    # 10. Parent Dashboard
    print("\n10. Testing Parent Dashboard...")
    r = requests.get(f"{BASE_URL}/api/parent/dashboard/{user_id}", headers=HEADERS)
    log_response("Parent Dashboard", r)

    print("\nMock Testing Complete.")

if __name__ == "__main__":
    try:
        requests.get(BASE_URL, timeout=2)
        run_tests()
    except requests.exceptions.ConnectionError:
        print(f"Error: Backend server not found at {BASE_URL}")
        print("Please run 'python -m app.main' in a separate terminal first!")


