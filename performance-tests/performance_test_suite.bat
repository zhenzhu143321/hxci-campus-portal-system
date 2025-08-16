@echo off
REM =======================================================
REM 智能通知系统性能压力测试套件
REM 版本: 1.0
REM 创建日期: 2025-08-10
REM =======================================================

echo =======================================================
echo 智能通知系统性能压力测试套件 v1.0
echo =======================================================

set BASE_URL=http://localhost:48081
set MOCK_URL=http://localhost:48082
set TEST_DATE=%date:~0,4%-%date:~5,2%-%date:~8,2%_%time:~0,2%-%time:~3,2%-%time:~6,2%
set RESULTS_DIR=D:\ClaudeCode\AI_Web\performance-tests\results\%TEST_DATE%

echo 创建测试结果目录: %RESULTS_DIR%
mkdir "%RESULTS_DIR%" 2>nul

echo.
echo 第一步: 系统健康检查
echo =======================================================

echo 检查主服务 (48081)...
curl -s -o "%RESULTS_DIR%\health_check_main.txt" -w "HTTP状态: %%{http_code}, 响应时间: %%{time_total}s\n" %BASE_URL%/admin-api/test/notification/api/ping
if %errorlevel% neq 0 (
    echo [ERROR] 主服务连接失败！
    pause
    exit /b 1
)

echo 检查Mock API (48082)...
curl -s -o "%RESULTS_DIR%\health_check_mock.txt" -w "HTTP状态: %%{http_code}, 响应时间: %%{time_total}s\n" %MOCK_URL%/mock-school-api/auth/health
if %errorlevel% neq 0 (
    echo [ERROR] Mock API连接失败！
    pause
    exit /b 1
)

echo [OK] 所有服务健康检查通过

echo.
echo 第二步: 获取测试用Token
echo =======================================================

REM 获取校长Token用于测试
echo 获取校长登录Token...
curl -s -X POST "%MOCK_URL%/mock-school-api/auth/authenticate" ^
-H "Content-Type: application/json" ^
-d "{\"employeeId\":\"PRINCIPAL_001\",\"name\":\"Principal-Zhang\",\"password\":\"admin123\"}" ^
-o "%RESULTS_DIR%\auth_response.json"

REM 从响应中提取Token (Windows CMD解析JSON比较复杂，我们用临时方案)
echo Token获取完成，保存在: %RESULTS_DIR%\auth_response.json

echo.
echo 测试环境准备完成！
echo 结果保存目录: %RESULTS_DIR%
echo.

pause