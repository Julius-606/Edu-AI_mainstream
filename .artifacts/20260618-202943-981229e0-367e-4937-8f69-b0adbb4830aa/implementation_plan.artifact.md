# Implementation Plan - Custom Authentication Migration

This plan replaces Firebase Authentication with a custom, backend-driven solution. It includes a web-based signup flow and a JWT-based login system.

## Proposed Changes

### Backend (FastAPI)

#### [models.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/models.py)
- Add `email` (unique) and `hashed_password` to the `User` model.

#### [NEW] [auth.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/auth.py)
- Implement password hashing using `passlib`.
- Implement JWT token generation and verification using `python-jose`.

#### [main.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/main.py)
- Add `/api/auth/login` endpoint.
- Add `/signup` endpoint that serves a basic HTML form.
- Update middleware to optionally verify the `Authorization: Bearer <token>` header.

#### [NEW] [templates/signup.html](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/templates/signup.html)
- A simple HTML page for user registration.

---

### Android App (Frontend)

#### [build.gradle.kts](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/build.gradle.kts)
- Remove Firebase plugins and dependencies.
- Add `androidx.security:security-crypto` for secure token storage.

#### [RetrofitClient.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/data/remote/RetrofitClient.kt)
- Update interceptor to include `Authorization: Bearer <token>` if a token is stored.

#### [LoginScreen.kt](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/app/src/main/java/com/example/edu_ai/ui/screens/LoginScreen.kt)
- Remove Firebase logic.
- Implement login call to the custom backend.
- Use `Intent(Intent.ACTION_VIEW)` to launch the browser for the signup URL.

---

## Verification Plan

### Automated Tests
- Syntax check using `analyze_file`.
- Verify backend endpoints (login, signup page) using `curl` or internal tests if possible.

### Manual Verification
1. Launch app -> Click Sign Up -> Browser opens to signup page.
2. Complete signup in browser.
3. Return to app -> Login with credentials.
4. Verify data loads correctly (proving JWT is working).
