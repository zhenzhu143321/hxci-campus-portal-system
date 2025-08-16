@echo off
echo ==========================================
echo Complete Service Lifecycle Manager
echo ==========================================
echo This script will:
echo 1. Kill all existing services
echo 2. Clean up memory and cache
echo 3. Start both notification services
echo ==========================================
echo.

REM Set optimized memory parameters
set "MAVEN_OPTS=-Xms256m -Xmx1024m -XX:MaxMetaspaceSize=512m"
set "JAVA_OPTS=-Xms256m -Xmx1024m"

echo STEP 1: Killing all existing Java services...
echo ==========================================
tasklist | findstr "java.exe"
echo.
echo Terminating all Java processes...
taskkill /f /im java.exe 2>nul
wmic process where "name='java.exe'" delete 2>nul
echo Java processes terminated.
echo.

echo STEP 2: Memory and cache cleanup...
echo ==========================================
if exist "%USERPROFILE%\.m2\repository\.cache" (
    echo Cleaning Maven cache...
    rmdir /s /q "%USERPROFILE%\.m2\repository\.cache" 2>nul
)
if exist "%TEMP%\spring-boot*" (
    echo Cleaning Spring Boot temp files...
    del /f /q "%TEMP%\spring-boot*" 2>nul
)
echo Waiting for system cleanup...
timeout /t 5 /nobreak >nul
echo.

echo STEP 3: Environment validation...
echo ==========================================
echo Memory settings: %MAVEN_OPTS%
echo.
cd /d "D:\ClaudeCode\AI_Web\yudao-boot-mini"
if not exist "pom.xml" (
    echo ERROR: Project directory not found!
    pause
    exit /b 1
)
echo Project directory OK: %CD%
echo.

echo STEP 4: Starting Main Notification Service (Port 48081)...
echo ==========================================
echo Starting in separate window - DO NOT CLOSE IT!
start "Main-Notification-Service" cmd /k "echo Starting Main Notification Service... && echo MAVEN_OPTS=%MAVEN_OPTS% && cd /d D:\ClaudeCode\AI_Web\yudao-boot-mini && mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local"
echo Main service starting...
echo.

echo Waiting 20 seconds for main service to initialize...
timeout /t 20 /nobreak

echo STEP 5: Starting Mock School API Service (Port 48082)...
echo ==========================================
echo Starting in separate window - DO NOT CLOSE IT!
start "Mock-School-API" cmd /k "echo Starting Mock School API Service... && echo MAVEN_OPTS=%MAVEN_OPTS% && cd /d D:\ClaudeCode\AI_Web\yudao-boot-mini && mvn spring-boot:run -pl yudao-mock-school-api -Dspring.profiles.active=local"
echo Mock API service starting...
echo.

echo STEP 6: Waiting for services to start...
echo ==========================================
echo Please wait 2-3 minutes for both services to fully start...
timeout /t 60 /nobreak
echo.

echo STEP 7: Testing service connectivity...
echo ==========================================
echo Testing Main Notification Service (48081)...
curl -X GET "http://localhost:48081/admin-api/infra/notification/ping" --max-time 10 --silent --show-error
if errorlevel 1 (
    echo WARNING: Main service may still be starting...
) else (
    echo SUCCESS: Main service is responding!
)
echo.

echo Testing Mock School API (48082)...
curl -X GET "http://localhost:48082/mock-school-api/auth/health" --max-time 10 --silent --show-error
if errorlevel 1 (
    echo WARNING: Mock API may still be starting...
) else (
    echo SUCCESS: Mock API service is responding!
)
echo.

echo STEP 8: Service Status Summary...
echo ==========================================
echo Checking running Java processes:
tasklist | findstr "java.exe"
echo.
echo Checking port usage:
netstat -ano | findstr ":48081"
netstat -ano | findstr ":48082"
echo.

echo ==========================================
echo SERVICE STARTUP COMPLETED!
echo ==========================================
echo.
echo Service Access URLs:
echo Main Notification Service: http://localhost:48081/admin-api/infra/notification/ping
echo Mock School API: http://localhost:48082/mock-school-api/auth/health
echo.
echo IMPORTANT NOTES:
echo 1. Two service windows are now open - DO NOT CLOSE THEM!
echo 2. Services may take 2-3 minutes to fully initialize
echo 3. If services don't respond immediately, wait a bit longer
echo 4. Use cleanup_services.bat to stop all services when done
echo.
echo Testing commands:
echo curl http://localhost:48081/admin-api/infra/notification/ping
echo curl http://localhost:48082/mock-school-api/auth/health
echo.
echo ==========================================

pause