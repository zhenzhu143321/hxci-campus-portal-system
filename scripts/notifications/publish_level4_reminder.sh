#!/bin/bash

# Level 4 提醒通知发布脚本
# 使用PRINCIPAL校长角色发布温馨提醒类通知
# 解决SQL注入检测问题 - 使用简化markdown语法

echo "🌟 Level 4 提醒通知发布脚本"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 使用PRINCIPAL校长账号 (最高权限)
EMPLOYEE_ID="PRINCIPAL_001"
NAME="Principal-Zhang"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/level4_notification_cookies.txt"

echo ""
echo "📋 Level 4 提醒通知配置："
echo "发布角色: 校长 ($EMPLOYEE_ID)"
echo "通知级别: Level 4 (温馨提醒)"
echo "目标范围: 全校范围"
echo "消息类型: 健康生活指导"

# 清理旧Cookie文件
rm -f "$COOKIE_JAR"

# 第一步：认证获取JWT Token
echo ""
echo "🔐 第一步：校长身份认证..."

AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
    -H "Content-Type: application/json" \
    -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")

# 提取JWT Token
if command -v jq &> /dev/null; then
    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.data.accessToken // .data.token // empty')
    ROLE_CODE=$(echo "$AUTH_RESPONSE" | jq -r '.data.roleCode // empty')
else
    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
    ROLE_CODE=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"roleCode":"\([^"]*\)".*/\1/p')
fi

if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
    echo "✅ 校长认证成功，角色代码: $ROLE_CODE"
    echo "✅ JWT Token: ${JWT_TOKEN:0:30}..."
else
    echo "❌ 认证失败，退出脚本"
    exit 1
fi

# 第二步：获取CSRF Token
echo ""
echo "🛡️ 第二步：获取CSRF防护Token..."

CSRF_RESPONSE=$(curl -s -c "$COOKIE_JAR" -X GET "$CSRF_API")

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
    echo "✅ CSRF Token获取成功: ${CSRF_TOKEN:0:20}..."
    echo "✅ 防护头名称: $CSRF_HEADER_NAME"

    # 手动设置CSRF Cookie
    echo -e "\tlocalhost\tFALSE\t/\tFALSE\t0\t$CSRF_COOKIE_NAME\t$CSRF_TOKEN" >> "$COOKIE_JAR"
    echo "✅ CSRF Cookie已配置"
else
    echo "❌ CSRF Token获取失败，退出脚本"
    exit 1
fi

# 第三步：发布Level 4提醒通知 (避免SQL注入检测的简化版本)
echo ""
echo "🌟 第三步：发布Level 4温馨提醒通知..."

# 构建通知内容 - 使用简化markdown避免SQL注入检测
CURRENT_DATE=$(date '+%Y年%m月%d日')
CURRENT_TIME=$(date '+%H:%M')

LEVEL4_JSON=$(cat <<EOF
{
    "title": "🌟【温馨提醒】健康生活指导",
    "content": "# 健康生活温馨提醒\\n\\n秋季健康指导：\\n\\n## 日常保健\\n- 每日饮水量 2000ml以上\\n- 睡眠时间 7-8小时\\n- 适量运动 30分钟每天\\n\\n## 饮食建议\\n1. 多吃应季蔬果\\n2. 减少辛辣刺激食物\\n3. 规律三餐时间\\n\\n## 心理健康\\n如有困扰可联系心理咨询中心\\n电话: 010-8888-9999\\n\\n愿每位师生都拥有健康快乐的校园生活！",
    "summary": "秋季健康生活指导，包含日常保健、饮食建议和心理健康建议",
    "level": 4,
    "categoryId": 5,
    "targetScope": "SCHOOL_WIDE",
    "pushChannels": [1, 5],
    "requireConfirm": false,
    "pinned": false
}
EOF
)

echo "发布请求预览："
echo "标题: 🌟【温馨提醒】健康生活指导"
echo "级别: Level 4 (提醒)"
echo "范围: 全校范围"
echo "内容: 健康生活指导 (markdown格式)"
echo ""

# 发送发布请求
PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
    -b "$COOKIE_JAR" \
    -X POST "$PUBLISH_API" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "tenant-id: 1" \
    -H "$CSRF_HEADER_NAME: $CSRF_TOKEN" \
    -d "$LEVEL4_JSON")

# 分析响应结果
HTTP_STATUS=$(echo "$PUBLISH_RESPONSE" | tail -n1 | cut -d: -f2)
RESPONSE_BODY=$(echo "$PUBLISH_RESPONSE" | sed '$d')

echo "📊 发布响应："
echo "状态码: $HTTP_STATUS"
echo "响应内容: $RESPONSE_BODY"

# 清理Cookie文件
rm -f "$COOKIE_JAR"

# 结果判断
if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ]; then
    echo ""
    echo "🎉 Level 4 提醒通知发布成功！"
    echo "✅ 校长角色权限验证通过"
    echo "✅ markdown格式内容发布完成"
    echo "✅ 可以继续开发Level 3常规通知脚本"

    # 提取通知ID（如果有）
    if command -v jq &> /dev/null; then
        NOTIFICATION_ID=$(echo "$RESPONSE_BODY" | jq -r '.data.notificationId // .data.id // empty')
        if [ -n "$NOTIFICATION_ID" ] && [ "$NOTIFICATION_ID" != "null" ]; then
            echo "📝 通知ID: $NOTIFICATION_ID"
        fi
    fi
else
    echo ""
    echo "❌ Level 4 提醒通知发布失败"
    echo "状态码: $HTTP_STATUS"
    echo "可能原因:"
    if [[ $RESPONSE_BODY == *"SQL injection"* ]]; then
        echo "- 内容包含特殊字符，触发SQL注入检测"
        echo "- 建议：进一步简化markdown语法"
    elif [[ $RESPONSE_BODY == *"权限不足"* ]]; then
        echo "- 权限配置问题"
        echo "- 建议：检查PRINCIPAL角色权限配置"
    else
        echo "- 其他技术问题，需要查看服务端日志"
    fi
    exit 1
fi

echo ""
echo "======================================="
echo "📋 Level 4 提醒通知发布完成报告："
echo "- 认证状态: ✅ 校长角色认证成功"
echo "- CSRF防护: ✅ Token验证通过"
echo "- 内容格式: ✅ Markdown格式"
echo "- 发布状态: $([ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ] && echo "✅ 发布成功" || echo "❌ 发布失败")"
echo "- 下一步: 开发Level 3常规通知脚本"
echo "======================================="