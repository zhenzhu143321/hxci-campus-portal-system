@echo off
REM =============================================================================
REM Vue Development Server Startup Script
REM File: start_vue_dev_server.bat
REM Purpose: Start Vue 3 Campus Portal Development Server on Port 3000
REM Author: Claude Code AI Assistant
REM Created: 2025-08-13
REM =============================================================================

title Vue Campus Portal Dev Server

echo.
echo ===============================================
echo    Vue Campus Portal Development Server
echo ===============================================
echo.
echo Starting Vue development server...
echo Project: HXCI Campus Portal System
echo Port: 3000 (auto-detected)
echo Framework: Vue 3 + Vite + TypeScript
echo.

REM Change to Vue project directory
cd /d "%~dp0..\..\hxci-campus-portal"

REM Verify we're in the correct directory
if not exist "package.json" (
    echo [ERROR] package.json not found!
    echo [ERROR] Current directory: %CD%
    echo [ERROR] Expected directory: hxci-campus-portal
    echo.
    echo Please ensure you're in the correct project directory.
    pause
    exit /b 1
)

REM Check if node_modules exists
if not exist "node_modules" (
    echo [WARNING] node_modules directory not found!
    echo [INFO] Installing dependencies first...
    echo.
    npm install
    if errorlevel 1 (
        echo [ERROR] Failed to install dependencies!
        pause
        exit /b 1
    )
    echo [SUCCESS] Dependencies installed successfully!
    echo.
)

REM Display project information
echo [INFO] Current directory: %CD%
echo [INFO] Node version: 
node --version
echo [INFO] NPM version: 
npm --version
echo.

REM Start the development server
echo [INFO] Starting Vue development server...
echo [INFO] Press Ctrl+C to stop the server
echo [INFO] Server will be available at: http://localhost:3000
echo.
echo ===============================================
echo    Server is starting... Please wait
echo ===============================================
echo.

REM Start npm dev server
npm run dev

REM If the server exits, show message
echo.
echo ===============================================
echo    Vue Development Server Stopped
echo ===============================================
echo.
echo [INFO] Development server has been stopped.
echo [INFO] You can restart by running this script again.
echo.
pause