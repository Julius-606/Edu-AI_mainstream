# Fix Login 403 Forbidden Error

The application fails to log in after signup with a 403 Forbidden error. This is caused by a middleware in `backend/main.py` that enforces an internal API key for all routes except a few whitelisted ones. The `/api/auth/login` endpoint is missing from this whitelist, causing it to be blocked if the API key is missing or mismatched. Even though the Android app is configured to send the key, it is standard practice to allow authentication endpoints to be accessible as gateways.

## Proposed Changes

### Backend

#### [main.py](file:///C:/Users/lenovo/Jay/Projects/Edu-AI_mainstream/backend/main.py)

- Add `/api/auth/login` to the `api_key_middleware` exclusion list.
- Remove duplicate definition of the `find_user` helper function.

```diff
 @app.middleware("http")
 async def api_key_middleware(request, call_next):
-    if request.url.path in ["/", "/docs", "/openapi.json", "/signup", "/favicon.ico"]:
+    if request.url.path in ["/", "/docs", "/openapi.json", "/signup", "/api/auth/login", "/favicon.ico"]:
         return await call_next(request)
```

## Verification Plan

### Automated Tests
- I will create a small verification script `backend/verify_login_access.py` that mocks the middleware logic or uses `TestClient` from FastAPI to verify that `/api/auth/login` can be accessed without a 403 error.

### Manual Verification
- Confirm that the `X-Internal-Api-Key` header is still correctly handled for other protected routes like `/api/user/{user_id}/dashboard`.
- Verify that the duplicate `find_user` function is removed and the remaining one works as expected.
