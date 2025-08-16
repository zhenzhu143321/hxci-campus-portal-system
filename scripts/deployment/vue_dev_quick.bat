@echo off
REM =============================================================================
REM Quick Vue Dev Server Launcher
REM File: vue_dev_quick.bat  
REM Purpose: Quick launch Vue dev server with minimal output
REM =============================================================================

title Vue Dev Server - Quick Start

echo Starting Vue development server...

cd /d "%~dp0..\..\hxci-campus-portal"

if not exist "package.json" (
    echo ERROR: Vue project not found!
    pause
    exit /b 1
)

echo Server starting at: http://localhost:3000
echo Press Ctrl+C to stop the server
echo.

npm run dev

echo.
echo Vue development server stopped.
pause