@echo off
echo Fixing PowerShell UTF-8 Configuration...

REM Create PowerShell profile directory if not exists
if not exist "C:\Users\Administrator\Documents\WindowsPowerShell" (
    mkdir "C:\Users\Administrator\Documents\WindowsPowerShell"
    echo Created PowerShell profile directory
)

REM Remove old profile if exists
if exist "C:\Users\Administrator\Documents\WindowsPowerShell\profile.ps1" (
    del /f /q "C:\Users\Administrator\Documents\WindowsPowerShell\profile.ps1"
    echo Removed old profile
)

REM Copy new profile
copy /y "D:\ClaudeCode\AI_Web\scripts\powershell\Campus-Portal-Profile.ps1" "C:\Users\Administrator\Documents\WindowsPowerShell\profile.ps1"

if %errorlevel% equ 0 (
    echo PowerShell profile updated successfully!
    echo Please restart PowerShell to use new configuration.
) else (
    echo Failed to update PowerShell profile.
)

echo.
echo Test the new configuration by running:
echo   powershell -ExecutionPolicy Bypass -File "D:\ClaudeCode\AI_Web\scripts\powershell\Test-API-Safe.ps1"

pause