
import requests
import json
import time
import subprocess
import os

# Configuration
BASE_URL = "http://127.0.0.1:8001"
HEADERS = {
    "X-Internal-Api-Key": "DEVELOPMENT_KEY",
    "Content-Type": "application/json"
}

def wait_for_server():
    print("Waiting for modular server to start...")
    for _ in range(10):
        try:
            requests.get(BASE_URL)
            print("✅ Server is up!")
            return True
        except:
            time.sleep(1)
    return False

def test_modular_api():
    print("\n--- Testing Modular API ---")

    # 0. Sign up / Prepare User
    print("0. Preparing Test User...")
    signup_data = {"username": "testuser", "email": "test@example.com", "password": "password123", "role": "Student"}
    # The /api/auth/signup-form endpoint expects Form data
    r = requests.post(f"{BASE_URL}/api/auth/signup-form", data=signup_data, headers={"X-Internal-Api-Key": "DEVELOPMENT_KEY"})
    if r.status_code == 200 or (r.status_code == 400 and "already exists" in r.text):
        print("✅ Test User is ready")
    else:
        print(f"❌ Signup Failed: {r.status_code}")
        print(r.text)
        return

    # 1. Login
    print("1. Testing Login...")
    login_data = {"email": "test@example.com", "password": "password123"}
    r = requests.post(f"{BASE_URL}/api/auth/login", json=login_data, headers=HEADERS)
    if r.status_code == 200:
        print("✅ Login Success")
        token = r.json()["access_token"]
        HEADERS["Authorization"] = f"Bearer {token}"
        user_id = r.json()["user_id"]
    else:
        print(f"❌ Login Failed: {r.status_code}")
        print(r.text)
        return

    # 2. Get User
    print("\n2. Testing Get User...")
    r = requests.get(f"{BASE_URL}/api/users/{user_id}", headers=HEADERS)
    if r.status_code == 200:
        print(f"✅ Get User Success: {r.json()['username']}")
    else:
        print(f"❌ Get User Failed: {r.status_code}")

    # 3. AI Chat
    print("\n3. Testing AI Chat...")
    chat_payload = {"user_id": user_id, "prompt": "Ping?", "history": []}
    r = requests.post(f"{BASE_URL}/api/ai/chat", json=chat_payload, headers=HEADERS)
    if r.status_code == 200:
        print(f"✅ Chat Success: {r.json()['response'][:30]}...")
    else:
        print(f"❌ Chat Failed: {r.status_code}")

if __name__ == "__main__":
    # Note: This assumes the user will start the server manually as instructed
    # or we can try to launch it in background for a quick check.
    if wait_for_server():
        test_modular_api()
    else:
        print("❌ Could not connect to server. Please run 'python -m app.main' from the backend directory.")


 