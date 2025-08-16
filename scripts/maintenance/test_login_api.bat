@echo off
chcp 65001 >nul 2>&1

echo ==========================================
echo 测试智能通知系统API - 登录认证演示
echo ==========================================
echo.

echo 1. 首先进行用户登录获取访问令牌...
echo.

curl -X POST "http://localhost:48080/admin-api/system/auth/login" ^
  -H "Content-Type: application/json" ^
  -H "tenant-id: 1" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}" ^
  --silent --show-error

echo.
echo.
echo ==========================================
echo 说明：
echo 1. 登录成功后会返回访问令牌（accessToken）
echo 2. 使用返回的令牌访问其他API接口
echo 3. 在请求头中添加: Authorization: Bearer {accessToken}
echo ==========================================
echo.

pause