#!/bin/bash

# 学校登录接口测试脚本
# 测试新实现的双Token认证功能

echo "🏫 开始测试学校登录接口..."

API_BASE="http://localhost:48082/mock-school-api/auth"

# 测试1: 健康检查
echo ""
echo "📊 测试1: API健康检查"
curl -s "$API_BASE/health" | jq '.'

# 测试2: 学校API集成状态检查
echo ""
echo "📊 测试2: 学校API集成状态"
curl -s "$API_BASE/school-integration-status" | jq '.'

# 测试3: 学校登录接口（Mock模式）
echo ""
echo "🎭 测试3: 学校登录 - Mock模式"
curl -s -X POST "$API_BASE/school-login" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "STUDENT_001",
    "name": "Student-Test",
    "password": "admin123",
    "useRealSchoolApi": false
  }' | jq '.'

# 测试4: 学校登录接口（校长账号）
echo ""
echo "👑 测试4: 学校登录 - 校长账号"
curl -s -X POST "$API_BASE/school-login" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "PRINCIPAL_001",
    "name": "Principal-Test",
    "password": "admin123",
    "useRealSchoolApi": false
  }' | jq '.'

# 测试5: 学校登录接口（教师账号）
echo ""
echo "🎓 测试5: 学校登录 - 教师账号"
curl -s -X POST "$API_BASE/school-login" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "TEACHER_001",
    "name": "Teacher-Test",
    "password": "admin123",
    "useRealSchoolApi": false
  }' | jq '.'

# 测试6: 错误参数测试
echo ""
echo "❌ 测试6: 错误参数测试"
curl -s -X POST "$API_BASE/school-login" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "",
    "name": "",
    "password": ""
  }' | jq '.'

echo ""
echo "✅ 学校登录接口测试完成！"