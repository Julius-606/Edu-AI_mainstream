FROM python:3.10-slim

WORKDIR /app

# Install system dependencies for PostgreSQL driver
RUN apt-get update && apt-get install -y \
    libpq-dev \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
# This assumes requirements.txt is in the same folder as Dockerfile
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy all files from the current directory (the backend folder)
COPY . .

# Hugging Face Spaces uses port 7860 by default
ENV PORT=7860
EXPOSE 7860

# Run uvicorn. Since we are in the root of the backend files,
# main:app refers to main.py in the current directory.
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "7860"]
