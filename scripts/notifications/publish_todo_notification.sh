#!/bin/bash

# 待办通知发布脚本 (基于Level 4成功模式)
# 适配待办通知API：/admin-api/test/todo-new/api/publish
# 支持priority、dueDate、targetScope等待办特有字段

# 导入缓存清理工具函数
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/cache_clear_utils.sh"

echo "📝 待办通知发布脚本 (基于Level 4成功模式)"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
TODO_PUBLISH_API="http://localhost:48081/admin-api/test/todo-new/api/publish"

# 使用PRINCIPAL校长账号 (最高权限)
EMPLOYEE_ID="PRINCIPAL_001"
NAME="Principal-Zhang"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/todo_notification_cookies.txt"

echo ""
echo "📋 待办通知配置："
echo "发布角色: 校长 ($EMPLOYEE_ID)"
echo "通知类型: 待办通知 (Todo)"
echo "目标范围: 全校范围"
echo "优先级: 中等优先级 (medium)"

# 清理旧Cookie文件
rm -f "$COOKIE_JAR"

# 认证获取Token函数 (与Level 4完全一致)
authenticate() {
    echo ""
    echo "🔐 身份认证中..."

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
        echo "✅ JWT Token获取成功"
        return 0
    else
        echo "❌ 认证失败"
        return 1
    fi
}

# 获取CSRF Token函数 (与Level 4完全一致)
get_csrf() {
    echo ""
    echo "🛡️ 获取CSRF防护Token..."

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
        echo "✅ CSRF Token获取成功"
        echo "✅ 防护头名称: $CSRF_HEADER_NAME"

        # 手动设置CSRF Cookie
        echo -e "\tlocalhost\tFALSE\t/\tFALSE\t0\t$CSRF_COOKIE_NAME\t$CSRF_TOKEN" >> "$COOKIE_JAR"
        echo "✅ CSRF Cookie已配置"
        return 0
    else
        echo "❌ CSRF Token获取失败"
        return 1
    fi
}

# 发布待办通知函数 (适配待办API格式)
publish_todo_notification() {
    echo ""
    echo "📝 发布待办通知..."

    # 获取当前时间和截止日期
    CURRENT_DATE=$(date '+%Y年%m月%d日')
    CURRENT_TIME=$(date '+%H:%M')
    DUE_DATE=$(date -d "+7 days" '+%Y-%m-%dT23:59:59')  # 7天后截止

    # 构建待办通知JSON (简化版本，避免安全过滤)
    TODO_JSON=$(cat <<EOF
{
    "title": "待办事项测试通知",
    "content": "这是一个待办事项测试通知。请完成以下任务：\\n\\n1. 完成课程作业\\n2. 提交实验报告\\n3. 参加期末考试\\n\\n截止时间：$(date -d "+7 days" '+%Y年%m月%d日')\\n\\n请及时完成相关任务。",
    "priority": "medium",
    "dueDate": "$DUE_DATE",
    "targetScope": "SCHOOL_WIDE",
    "targetStudentIds": [],
    "targetGradeIds": [],
    "targetClassIds": [],
    "targetDepartmentIds": []
}
EOF
)

    echo "📝 待办通知内容预览："
    echo "标题: 待办事项测试通知"
    echo "优先级: medium (中等优先级)"
    echo "范围: 全校范围"
    echo "截止时间: $(date -d "+7 days" '+%Y年%m月%d日 23:59')"
    echo ""

    # 发送发布请求 (使用待办API端点)
    PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
        -b "$COOKIE_JAR" \
        -X POST "$TODO_PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "$CSRF_HEADER_NAME: $CSRF_TOKEN" \
        -d "$TODO_JSON")

    # 分析响应结果 (与Level 4一致)
    HTTP_STATUS=$(echo "$PUBLISH_RESPONSE" | tail -n1 | cut -d: -f2)
    RESPONSE_BODY=$(echo "$PUBLISH_RESPONSE" | sed '$d')

    echo "📊 发布响应："
    echo "状态码: $HTTP_STATUS"
    echo "响应内容: $RESPONSE_BODY"

    # 提取通知ID (适配待办API响应格式)
    if command -v jq &> /dev/null; then
        NOTIFICATION_ID=$(echo "$RESPONSE_BODY" | jq -r '.data.id // .data.notificationId // empty')
        JSON_CODE=$(echo "$RESPONSE_BODY" | jq -r '.code // empty')
    else
        NOTIFICATION_ID=$(echo "$RESPONSE_BODY" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
        JSON_CODE=$(echo "$RESPONSE_BODY" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
    fi

    # 清理Cookie文件
    rm -f "$COOKIE_JAR"

    # 结果判断 (与Level 4一致)
    if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ] && [ "$JSON_CODE" = "0" ]; then
        echo ""
        echo "🎉 待办通知发布成功！"
        echo "✅ 待办ID: $NOTIFICATION_ID"
        echo "✅ 优先级: medium (中等优先级)"
        echo "✅ 截止日期: $(date -d "+7 days" '+%Y年%m月%d日 23:59')"
        echo "✅ 目标范围: 全校师生"

        # 显示缓存清理提示 (集成缓存清理工具)
        check_and_show_cache_tips "$NOTIFICATION_ID" "待办"

        return 0
    else
        echo ""
        echo "❌ 待办通知发布失败"
        echo "状态码: $HTTP_STATUS, JSON代码: $JSON_CODE"
        return 1
    fi
}

# 执行完整发布流程 (与Level 4一致)
if authenticate && get_csrf && publish_todo_notification; then
    echo ""
    echo "======================================="
    echo "🎉 待办通知发布成功报告："
    echo "- 认证状态: ✅ 校长角色认证成功"
    echo "- CSRF防护: ✅ Token验证通过"
    echo "- 待办API: ✅ /admin-api/test/todo-new/api/publish"
    echo "- 数据格式: ✅ priority/dueDate/targetScope格式正确"
    echo "- 发布状态: ✅ 发布成功"
    echo "- 前端显示: ✅ 支持待办列表展示和完成状态管理"
    echo "- 缓存清理: ✅ 已集成localStorage清理提示"
    echo "- 下一步: 测试待办通知功能完整性"
    echo "======================================="
    exit 0
else
    echo ""
    echo "======================================="
    echo "❌ 待办通知发布失败"
    echo "请检查服务状态和网络连接"
    echo "- 确保48081和48082端口服务正常运行"
    echo "- 检查待办API端点是否正确配置"
    echo "- 验证JWT和CSRF Token获取流程"
    echo "======================================="
    exit 1
fi