#!/bin/bash

# 极简版通知发布测试
# 完全避免特殊字符，测试API基础功能

echo "🧪 极简版通知发布测试"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 使用PRINCIPAL校长账号
EMPLOYEE_ID="PRINCIPAL_001"
NAME="Principal-Zhang"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/minimal_test_cookies.txt"

echo ""
echo "📋 极简测试配置："
echo "发布角色: 校长 ($EMPLOYEE_ID)"
echo "内容类型: 纯文本 (无特殊字符)"

# 清理旧Cookie文件
rm -f "$COOKIE_JAR"

# 认证
echo ""
echo "🔐 认证中..."
AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
    -H "Content-Type: application/json" \
    -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")

JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ -z "$JWT_TOKEN" ]; then
    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
fi

if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
    echo "✅ 认证成功"
else
    echo "❌ 认证失败"
    exit 1
fi

# 获取CSRF Token
echo ""
echo "🛡️ 获取CSRF Token..."
CSRF_RESPONSE=$(curl -s -c "$COOKIE_JAR" -X GET "$CSRF_API")
CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -n "$CSRF_TOKEN" ] && [ "$CSRF_TOKEN" != "null" ]; then
    echo "✅ CSRF Token获取成功"
    # 手动设置Cookie
    echo -e "\tlocalhost\tFALSE\t/\tFALSE\t0\tXSRF-TOKEN\t$CSRF_TOKEN" >> "$COOKIE_JAR"
else
    echo "❌ CSRF Token获取失败"
    exit 1
fi

# 发布极简通知 - 完全避免特殊字符
echo ""
echo "📢 发布极简通知..."

# 极简JSON - 仅使用基本字符
MINIMAL_JSON='{"title":"健康提醒","content":"秋季健康指导 每日饮水2000ml 睡眠7小时 适量运动30分钟 愿大家身体健康","summary":"健康提醒","level":4,"categoryId":5,"targetScope":"SCHOOL_WIDE","pushChannels":[1,5],"requireConfirm":false,"pinned":false}'

echo "发布内容: 纯文本健康提醒 (无特殊字符)"

PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
    -b "$COOKIE_JAR" \
    -X POST "$PUBLISH_API" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "tenant-id: 1" \
    -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
    -d "$MINIMAL_JSON")

# 分析结果
HTTP_STATUS=$(echo "$PUBLISH_RESPONSE" | tail -n1 | cut -d: -f2)
RESPONSE_BODY=$(echo "$PUBLISH_RESPONSE" | sed '$d')

echo ""
echo "📊 发布结果："
echo "HTTP状态码: $HTTP_STATUS"
echo "响应内容: $RESPONSE_BODY"

# 检查JSON响应中的code字段
JSON_CODE=""
if command -v jq &> /dev/null; then
    JSON_CODE=$(echo "$RESPONSE_BODY" | jq -r '.code // empty')
elif [[ $RESPONSE_BODY == *'"code":'* ]]; then
    JSON_CODE=$(echo "$RESPONSE_BODY" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
fi

echo "JSON代码: $JSON_CODE"

# 清理Cookie文件
rm -f "$COOKIE_JAR"

# 正确的成功判断逻辑
if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ] && [ "$JSON_CODE" = "0" ]; then
    echo ""
    echo "🎉 极简通知发布成功！"
    echo "✅ API基础功能正常"
    echo "✅ 可以开始逐步添加markdown语法"
    exit 0
elif [[ $RESPONSE_BODY == *"SQL injection"* ]]; then
    echo ""
    echo "❌ 仍然触发SQL注入检测"
    echo "需要进一步简化内容或检查后端安全配置"
    exit 1
else
    echo ""
    echo "❌ 发布失败"
    echo "HTTP: $HTTP_STATUS, JSON: $JSON_CODE"
    echo "错误信息: $RESPONSE_BODY"
    exit 1
fi