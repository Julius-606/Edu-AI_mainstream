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

:: Select Backend Destination
echo =============================================
echo    SELECT BACKEND DESTINATION GATEWAY        
echo =============================================
echo  [1] Cloud Production (Hugging Face)
echo  [2] Remote Workstation Tunnel (Ngrok)
echo  [3] Local Container Service (Port 8001)
echo =============================================
set /p choice="Enter choice (1-3) [Default: 1]: "

set BACKEND=cloud
if "%choice%"=="2" set BACKEND=ngrok
if "%choice%"=="3" set BACKEND=container

:: Launch Desktop Client
echo [*] Starting Trace Desktop Client with backend: %BACKEND%...
python main.py --backend %BACKEND% %*
if %errorlevel% neq 0 (
    echo.
    echo [INFO] Exited.
    pause
)
