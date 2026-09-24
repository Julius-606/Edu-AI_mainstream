# ==============================================================================
# Edu-AI Trace Modular Backend Dockerfile
#
# ENTRYPOINT / ROUTING INSTRUCTION:
# The web interface of this backend opens directly at the Superuser Admin Login:
#   -> backend/templates/admin/login.html (served at / and /admin/login)
#
# The public student / user registration portal is directly accessible at:
#   -> backend/templates/public/signup.html (served at /signup, /Edu_AI/signup.html)
# ==============================================================================

FROM python:3.10-slim

WORKDIR /app

# Install system dependencies for PostgreSQL driver
RUN apt-get update && apt-get install -y \
    libpq-dev \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy all files from backend into container
COPY . .

# Hugging Face Spaces uses port 7860 by default; local runs use 8000 / 8001
ENV PORT=7860
EXPOSE 7860

# Launch Uvicorn server hosting the modular FastAPI application
# Serves Superuser Admin Login at root '/' and Public Registration at '/signup'
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "7860"]
