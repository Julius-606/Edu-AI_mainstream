FROM python:3.10-slimWORKDIR /app

# Install system dependencies for PostgreSQL driver
RUN apt-get update && apt-get install -y \
    libpq-dev \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy backend code
COPY . .

# Set working directory to where main.py is
WORKDIR /app/backend

# Hugging Face Spaces port is 7860
ENV PORT=7860
EXPOSE 7860

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "7860"]