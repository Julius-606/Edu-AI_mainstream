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

echo "[*] Launching Trace Desktop Client..."
python3 main.py "$@"
