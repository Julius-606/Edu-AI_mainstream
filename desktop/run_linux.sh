#!/usr/bin/env bash
# Trace Learning System - Desktop Launcher (Linux)
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "========================================================"
echo "  Trace Learning System - Linux Desktop Suite"
echo "========================================================"
echo ""

if ! command -v python3 &> /dev/null; then
    echo "[ERROR] python3 not found. Please install Python 3.10+."
    exit 1
fi

if [ -f "requirements.txt" ]; then
    echo "[*] Checking desktop requirements..."
    pip3 install -q -r requirements.txt || true
fi

echo "============================================="
echo "   SELECT BACKEND DESTINATION GATEWAY        "
echo "============================================="
echo " 1) Cloud Production (Hugging Face)"
echo " 2) Remote Workstation Tunnel (Ngrok)"
echo " 3) Local Container Service (Port 8001)"
echo "============================================="
read -p "Enter choice (1-3) [Default: 1]: " choice

case "$choice" in
    2) BACKEND="ngrok" ;;
    3) BACKEND="container" ;;
    *) BACKEND="cloud" ;;
esac

echo "[*] Launching Trace Desktop Client with backend: $BACKEND..."
python3 main.py --backend "$BACKEND" "$@"
