# Task Management

- [/] Migrate from Firebase to Custom Authentication
    - [/] Step 1: Cleanup and Backend Foundation
        - [ ] Remove Firebase dependencies from `app/build.gradle.kts` and `libs.versions.toml`
        - [ ] Add `passlib` and `python-jose` to backend requirements
        - [ ] Update `backend/models.py` with `email` and `hashed_password`
        - [ ] Create `backend/auth.py` for JWT and Hashing logic
    - [ ] Step 2: Backend Implementation
        - [ ] Implement `POST /api/auth/login` in `backend/main.py`
        - [ ] Create a simple `signup.html` and a `GET /signup` route
        - [ ] Update backend middleware to verify JWT
    - [ ] Step 3: Android App Implementation
        - [ ] Update `RetrofitClient.kt` to handle JWT
        - [ ] Update `LoginScreen.kt` to use custom login and launch browser for signup
        - [ ] Implement local token storage (EncryptedSharedPreferences)
        - [ ] Update `EduAIRepository.kt` and `AppNavigation.kt` for the new flow
- [ ] Provide final setup instructions (JWT Secret Key, etc.)
