#!/usr/bin/env bash

# Trace Learning System - Local Multi-Service Launcher
# Runs both the Python FastAPI Backend (Port 8001) and Frontend Orchestrator (Port 3000)

echo "=========================================================="
echo "    🚀 Starting Trace Learning System (Multi-Service)     "
echo "=========================================================="

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Cleanup on exit
trap 'kill $(jobs -p) 2>/dev/null' EXIT

# 1. Start Python Backend
echo "-> Starting Python FastAPI Backend on port 8001..."
cd "$ROOT_DIR/backend" || exit 1
if [ -d "venv" ]; then
    source venv/bin/activate
fi

export BACKEND_PORT=8001
python3 run_modular.py &
BACKEND_PID=$!

# 2. Start Frontend Server
echo "-> Starting Node/React Applet on port 3000..."
cd "$ROOT_DIR" || exit 1
npm run dev &
FRONTEND_PID=$!

echo ""
echo "=========================================================="
echo "  Trace Application is live!"
echo "  - Main Learning Portal:    http://localhost:3000"
echo "  - Dedicated Admin Console: http://localhost:3000/?view=admin"
echo "  - Python Backend:          http://localhost:8001"
echo "  - Backend API Docs:        http://localhost:8001/docs"
echo "=========================================================="
echo "Press Ctrl+C to stop all services."

wait
