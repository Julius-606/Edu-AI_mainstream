@echo off
title Trace Learning System - Desktop Launcher (Windows)
echo ========================================================
echo   Trace Learning System - Windows Desktop Suite
echo ========================================================
echo.

:: Check Python installation
where python >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Python is not installed or not in PATH.
    echo Please install Python 3.10+ from https://python.org
    pause
    exit /b 1
)

:: Install desktop requirements if needed
if exist requirements.txt (
    echo [*] Checking desktop dependencies...
    pip install -q -r requirements.txt
)

:: Launch Desktop Client
echo [*] Starting Trace Desktop Client...
python main.py %*
if %errorlevel% neq 0 (
    echo.
    echo [INFO] Exited.
    pause
)
