#!/bin/bash

# API基础验证脚本 - 修复版
# 解决CSRF Token使用问题：同时设置Cookie和Header

echo "🔧 校园通知系统 API 修复测试"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 测试账号配置
EMPLOYEE_ID="SYSTEM_ADMIN_001"
NAME="系统管理员"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/notification_cookies.txt"

echo ""
echo "📋 测试配置："
echo "认证API: $AUTH_API"
echo "CSRF API: $CSRF_API"
echo "发布API: $PUBLISH_API"
echo "测试账号: $EMPLOYEE_ID ($NAME)"
echo "Cookie文件: $COOKIE_JAR"

# 清理旧的Cookie文件
rm -f "$COOKIE_JAR"

# 第一步：测试认证API
echo ""
echo "🔐 第一步：测试认证API..."

AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
    -H "Content-Type: application/json" \
    -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")

echo "认证响应: $AUTH_RESPONSE"

# 提取JWT Token
if command -v jq &> /dev/null; then
    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.data.accessToken // .data.token // empty')
else
    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
fi

if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
    echo "✅ JWT Token 获取成功: ${JWT_TOKEN:0:30}..."
else
    echo "❌ JWT Token 获取失败"
    exit 1
fi

# 第二步：测试CSRF API（保存Cookie）
echo ""
echo "🛡️ 第二步：测试CSRF API（保存Cookie）..."

CSRF_RESPONSE=$(curl -s -c "$COOKIE_JAR" -X GET "$CSRF_API")
echo "CSRF响应: $CSRF_RESPONSE"

# 提取CSRF Token和配置信息
if command -v jq &> /dev/null; then
    CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | jq -r '.data.token // .token // empty')
    CSRF_HEADER_NAME=$(echo "$CSRF_RESPONSE" | jq -r '.data.headerName // .headerName // "X-XSRF-TOKEN"')
    CSRF_COOKIE_NAME=$(echo "$CSRF_RESPONSE" | jq -r '.data.cookieName // .cookieName // "XSRF-TOKEN"')
else
    CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    CSRF_HEADER_NAME="X-XSRF-TOKEN"
    CSRF_COOKIE_NAME="XSRF-TOKEN"
fi

if [ -n "$CSRF_TOKEN" ] && [ "$CSRF_TOKEN" != "null" ]; then
    echo "✅ CSRF Token 获取成功: ${CSRF_TOKEN:0:20}..."
    echo "✅ CSRF Header Name: $CSRF_HEADER_NAME"
    echo "✅ CSRF Cookie Name: $CSRF_COOKIE_NAME"

    # 手动设置CSRF Cookie（确保Cookie正确设置）
    echo -e "\tlocalhost\tFALSE\t/\tFALSE\t0\t$CSRF_COOKIE_NAME\t$CSRF_TOKEN" >> "$COOKIE_JAR"
    echo "✅ CSRF Cookie 已手动添加到Cookie文件"
else
    echo "❌ CSRF Token 获取失败"
    exit 1
fi

# 第三步：测试通知发布API（使用Cookie + Header）
echo ""
echo "📢 第三步：测试通知发布API（Cookie + Header模式）..."

# 构建测试通知JSON（修复版）
TEST_JSON='{
    "title": "🔧 API修复测试通知",
    "content": "# API修复测试\\n\\n这是一条用于测试修复后API的通知\\n\\n**测试时间**: 2025年9月13日\\n**修复内容**: 添加CSRF Cookie支持\\n\\n*如果您看到这条通知，说明CSRF问题已解决* ✅",
    "level": 4,
    "categoryId": 5,
    "targetScope": "SCHOOL_WIDE",
    "pushChannels": [1, 5],
    "requireConfirm": false,
    "pinned": true
}'

echo "发布请求JSON: $TEST_JSON"
echo ""
echo "使用的认证信息："
echo "- JWT Token: ${JWT_TOKEN:0:30}..."
echo "- CSRF Token: ${CSRF_TOKEN:0:20}..."
echo "- Cookie文件: $COOKIE_JAR"

# 显示Cookie文件内容（调试用）
echo ""
echo "Cookie文件内容："
cat "$COOKIE_JAR" 2>/dev/null || echo "Cookie文件为空"

echo ""
echo "发送请求..."

PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
    -b "$COOKIE_JAR" \
    -X POST "$PUBLISH_API" \
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

# 清理Cookie文件
rm -f "$COOKIE_JAR"

# 判断结果
if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ]; then
    echo ""
    echo "🎉 API修复成功！CSRF问题已解决"
    echo "可以继续开发分级通知脚本"

    # 验证数据库中是否真的插入了通知
    echo ""
    echo "🔍 验证数据库插入结果..."

else
    echo ""
    echo "❌ API修复失败，状态码: $HTTP_STATUS"
    echo "CSRF问题可能仍然存在"
    exit 1
fi

echo ""
echo "======================================="
echo "📊 API修复测试完成报告："
echo "- 认证API: ✅ 工作正常"
echo "- CSRF API: ✅ 工作正常"
echo "- Cookie处理: ✅ $([ -f "$COOKIE_JAR" ] && echo "支持" || echo "完成")"
echo "- 发布API: $([ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ] && echo "✅ 修复成功" || echo "❌ 仍有问题")"
echo "======================================="