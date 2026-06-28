import os
from fastapi import FastAPI, Request, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, HTMLResponse
from app.db.session import engine, Base
from app.api import auth, users, ai, teacher, parent, learning

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Trace Modular API", version="3.0.0")

INTERNAL_API_KEY = os.environ.get("INTERNAL_API_KEY", "DEVELOPMENT_KEY")

# Global Security Middleware
@app.middleware("http")
async def api_key_middleware(request: Request, call_next):
    # Paths that bypass API key
    if request.url.path in ["/", "/docs", "/openapi.json", "/favicon.ico", "/api/auth/login", "/api/auth/signup-form"]:
        return await call_next(request)

    x_api_key = request.headers.get("X-Internal-Api-Key")
    if x_api_key != INTERNAL_API_KEY:
        return JSONResponse(status_code=403, content={"detail": "Unauthorized: Invalid API Key"})

    return await call_next(request)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include Routers
app.include_router(auth.router, prefix="/api/auth")
app.include_router(users.router, prefix="/api")
app.include_router(ai.router, prefix="/api")
app.include_router(teacher.router, prefix="/api")
app.include_router(parent.router, prefix="/api")
app.include_router(learning.router, prefix="/api")

@app.get("/", response_class=HTMLResponse)
def root():
    return "<h1>Trace Modular API v3.0.0 Online</h1>"

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
