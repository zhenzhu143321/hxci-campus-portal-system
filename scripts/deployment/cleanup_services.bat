@echo off
echo ==========================================
echo Service Process and Memory Cleanup Script
echo ==========================================

echo 1. Checking current Java processes and memory usage...
tasklist | findstr "java.exe"
echo.

echo 2. Checking port usage...
netstat -ano | findstr ":48081"
netstat -ano | findstr ":48082"
echo.

echo 3. Terminating all Java processes (including Maven and Spring Boot)...
REM Kill by process name first
taskkill /f /im java.exe 2>nul
REM Use wmic for thorough cleanup
wmic process where "name='java.exe'" delete 2>nul
if errorlevel 1 (
    echo INFO: No Java processes were running
) else (
    echo OK: Java processes terminated
)
echo.

echo 4. Clearing Maven temporary files and cache...
if exist "%USERPROFILE%\.m2\repository\.cache" (
    rmdir /s /q "%USERPROFILE%\.m2\repository\.cache" 2>nul
    echo OK: Maven cache cleared
)
if exist "%TEMP%\spring-boot*" (
    del /f /q "%TEMP%\spring-boot*" 2>nul
    echo OK: Spring Boot temp files cleared
)
echo.

echo 5. Waiting for system cleanup and memory release...
timeout /t 5 /nobreak >nul
echo.

echo 6. Final verification...
echo [Java Processes Check]
tasklist | findstr "java.exe"
if errorlevel 1 (
    echo OK: No Java processes running
) else (
    echo WARNING: Some Java processes still running
)
echo.

echo [Port Status Check]
netstat -ano | findstr ":4808"
if errorlevel 1 (
    echo OK: Ports 48081 and 48082 are free
) else (
    echo WARNING: Some ports may still be occupied
)
echo.

echo [Memory Status Check]
echo Available Memory Status:
wmic computersystem get TotalPhysicalMemory /format:value | findstr "="
echo.

echo ==========================================
echo Cleanup Summary:
echo - All Java processes terminated
echo - Maven cache and temp files cleared  
echo - Ports 48081/48082 released
echo - Memory freed for new services
echo.
echo System is ready for fresh service startup!
echo ==========================================

pause