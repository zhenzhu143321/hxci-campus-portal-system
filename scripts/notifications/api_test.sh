#!/bin/bash

# API基础验证脚本
# 用于测试通知发布系统的各个API端点

echo "🔍 校园通知系统 API 基础验证"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 测试账号配置
EMPLOYEE_ID="SYSTEM_ADMIN_001"
NAME="系统管理员"
PASSWORD="admin123"

echo ""
echo "📋 测试配置："
echo "认证API: $AUTH_API"
echo "CSRF API: $CSRF_API"
echo "发布API: $PUBLISH_API"
echo "测试账号: $EMPLOYEE_ID ($NAME)"

# 第一步：测试认证API
echo ""
echo "🔐 第一步：测试认证API..."
echo "请求数据: {\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}"

AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
    -H "Content-Type: application/json" \
    -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")

echo "认证响应: $AUTH_RESPONSE"

# 提取JWT Token
if command -v jq &> /dev/null; then
    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.data.token // .data.accessToken // empty')
else
    # 使用sed作为备选方案
    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    if [ -z "$JWT_TOKEN" ]; then
        JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
    fi
fi

if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
    echo "✅ JWT Token 获取成功: ${JWT_TOKEN:0:20}..."
else
    echo "❌ JWT Token 获取失败"
    echo "完整响应: $AUTH_RESPONSE"
    exit 1
fi

# 第二步：测试CSRF API
echo ""
echo "🛡️ 第二步：测试CSRF API..."

CSRF_RESPONSE=$(curl -s -X GET "$CSRF_API")
echo "CSRF响应: $CSRF_RESPONSE"

# 提取CSRF Token
if command -v jq &> /dev/null; then
    CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | jq -r '.data.token // .token // empty')
    CSRF_HEADER_NAME=$(echo "$CSRF_RESPONSE" | jq -r '.data.headerName // .headerName // "X-CSRF-TOKEN"')
else
    CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    CSRF_HEADER_NAME="X-CSRF-TOKEN"
fi

if [ -n "$CSRF_TOKEN" ] && [ "$CSRF_TOKEN" != "null" ]; then
    echo "✅ CSRF Token 获取成功: ${CSRF_TOKEN:0:20}..."
    echo "✅ CSRF Header Name: $CSRF_HEADER_NAME"
else
    echo "❌ CSRF Token 获取失败"
    exit 1
fi

# 第三步：测试通知发布API
echo ""
echo "📢 第三步：测试通知发布API..."

# 构建测试通知JSON
TEST_JSON='{
    "title": "API测试通知",
    "content": "这是一条用于测试API的通知\\n\\n**测试时间**: 2025年9月13日\\n\\n*如果您看到这条通知，说明API工作正常*",
    "level": 4,
    "categoryId": 5,
    "targetScope": "SCHOOL_WIDE",
    "pushChannels": [1, 5],
    "requireConfirm": false,
    "pinned": true
}'

echo "发布请求JSON: $TEST_JSON"
echo "请求头: Authorization: Bearer ${JWT_TOKEN:0:20}..."
echo "请求头: tenant-id: 1"
echo "请求头: $CSRF_HEADER_NAME: ${CSRF_TOKEN:0:20}..."

PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$PUBLISH_API" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "tenant-id: 1" \
    -H "$CSRF_HEADER_NAME: $CSRF_TOKEN" \
    -d "$TEST_JSON")

# 分离响应体和状态码
HTTP_STATUS=$(echo "$PUBLISH_RESPONSE" | tail -n1 | cut -d: -f2)
RESPONSE_BODY=$(echo "$PUBLISH_RESPONSE" | sed '$d')

echo ""
echo "发布响应状态: $HTTP_STATUS"
echo "发布响应内容: $RESPONSE_BODY"

# 判断结果
if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ]; then
    echo ""
    echo "🎉 API测试成功！所有接口工作正常"
    echo "可以继续开发分级通知脚本"
else
    echo ""
    echo "❌ API测试失败，状态码: $HTTP_STATUS"
    echo "需要检查API配置或服务状态"
    exit 1
fi

echo ""
echo "======================================="
echo "📊 API验证完成报告："
echo "- 认证API: ✅ 工作正常"
echo "- CSRF API: ✅ 工作正常"
echo "- 发布API: $([ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ] && echo "✅ 工作正常" || echo "❌ 存在问题")"
echo "======================================="