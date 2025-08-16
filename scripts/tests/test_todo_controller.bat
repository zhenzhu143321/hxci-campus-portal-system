@echo off
chcp 65001 >nul
echo 📋 T13待办通知系统 - API测试脚本
echo ========================================
echo.

REM 设置测试变量
set "BASE_URL=http://localhost:48081/admin-api/test/todo"
set "AUTH_TOKEN=Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbXBsb3llZUlkIjoiU1lTVEVNX0FETUlOXzAwMSIsInVzZXJuYW1lIjoi57O757uf566h55CG5ZGYIiwicm9sZUNvZGUiOiJTWVNURU1fQURNSU4iLCJyb2xlTmFtZSI6Iue7n-e7n-euoeeQhuWRmCIsImV4cCI6MTczNDI2MTExMH0.kw9jb6z8Y7z9X5XQE6sW8Y3DnAJzLbC4Z6b4cZqQkwY"

echo 🧪 测试1: Ping测试
curl -s -X GET "%BASE_URL%/api/ping" ^
  -H "Authorization: %AUTH_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -H "tenant-id: 1" | jq .
echo.

echo 📝 测试2: 发布待办通知
curl -s -X POST "%BASE_URL%/api/publish" ^
  -H "Authorization: %AUTH_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -H "tenant-id: 1" ^
  -d "{\"title\":\"📋 测试待办任务\",\"content\":\"这是一个测试待办任务，请在截止日期前完成。\\n\\n具体要求：\\n1. 完成课程作业\\n2. 提交实验报告\\n3. 参加小组讨论\",\"priority\":\"high\",\"dueDate\":\"2025-08-20\",\"targetScope\":\"CLASS\"}" | jq .
echo.

echo 📋 测试3: 获取我的待办列表
curl -s -X GET "%BASE_URL%/api/my-list?page=1&pageSize=10" ^
  -H "Authorization: %AUTH_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -H "tenant-id: 1" | jq .
echo.

echo ✅ 测试4: 标记待办完成 (需要先获取待办ID)
echo 请从上面的列表中获取待办ID，然后手动测试：
echo curl -X POST "%BASE_URL%/api/{id}/complete" -H "Authorization: %AUTH_TOKEN%" -H "Content-Type: application/json" -H "tenant-id: 1" -d "{}"
echo.

echo 📊 测试5: 获取待办统计 (需要先获取待办ID)
echo 请从上面的列表中获取待办ID，然后手动测试：
echo curl -X GET "%BASE_URL%/api/{id}/stats" -H "Authorization: %AUTH_TOKEN%" -H "Content-Type: application/json" -H "tenant-id: 1"
echo.

echo 📋 T13待办通知系统测试完成
echo ========================================
pause