# 🛠️ Local Backend Testing Environment

This guide helps you run and test the Trace Learning Backend on your local machine.

## 1. Prerequisites
- **Python 3.14+** (Already installed in your project folder)
- **.env file**: Ensure your `backend/.env` has the `GEMINI_API_KEYS` (I have already updated this for you).

## 2. Starting the Server
Open a terminal in the `backend` folder and run:
```bash
# Activate virtual environment
.venv\Scripts\activate

# Run the server
python main.py
```
The server will start at `http://127.0.0.1:8000`.

## 3. Running Mock Tests
While the server is running, open **another terminal** and run the mock test script. This script mimics all the calls the mobile app makes (Login, Dashboard, AI Chat, Quiz, etc.).

```bash
# In a second terminal
cd backend
.venv\Scripts\activate
python mock_app_test.py
```

## 4. Troubleshooting
- **Unauthorized (403)**: Ensure `INTERNAL_API_KEY` in `main.py` matches the one in `mock_app_test.py` (default is `DEVELOPMENT_KEY`).
- **AI Errors**: Check the server terminal for logs. I've enabled automatic key rotation, so it should handle rate limits (429) automatically.

---
**Note:** You can now modify `ai_engine.py` or `main.py` and immediately test the changes by rerunning `mock_app_test.py` without pushing to production!
