#!/bin/bash

# 角色权限测试脚本
# 测试不同角色的通知发布权限

echo "🔍 角色权限映射测试"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# Cookie文件
COOKIE_JAR="/tmp/role_test_cookies.txt"

# 测试角色列表
declare -a ROLES=(
    "PRINCIPAL_001:Principal-Zhang:admin123:校长"
    "ACADEMIC_ADMIN_001:Director-Li:admin123:教务主任"
    "TEACHER_001:Teacher-Wang:admin123:教师"
    "STUDENT_001:Student-Zhang:admin123:学生"
)

# 公共函数：测试单个角色
test_role() {
    local employee_id=$1
    local name=$2
    local password=$3
    local role_desc=$4

    echo ""
    echo "🧪 测试角色：$role_desc ($employee_id)"
    echo "----------------------------------------"

    # 清理Cookie文件
    rm -f "$COOKIE_JAR"

    # 认证
    echo "  🔐 认证中..."
    AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\": \"$employee_id\", \"name\": \"$name\", \"password\": \"$password\"}")

    # 提取JWT Token
    if command -v jq &> /dev/null; then
        JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.data.accessToken // .data.token // empty')
        ROLE_CODE=$(echo "$AUTH_RESPONSE" | jq -r '.data.roleCode // empty')
    else
        JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
        ROLE_CODE=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"roleCode":"\([^"]*\)".*/\1/p')
    fi

    if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
        echo "  ✅ 认证成功，角色代码: $ROLE_CODE"
    else
        echo "  ❌ 认证失败"
        return 1
    fi

    # 获取CSRF Token
    echo "  🛡️ 获取CSRF Token..."
    CSRF_RESPONSE=$(curl -s -c "$COOKIE_JAR" -X GET "$CSRF_API")

    if command -v jq &> /dev/null; then
        CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | jq -r '.data.token // .token // empty')
        CSRF_COOKIE_NAME=$(echo "$CSRF_RESPONSE" | jq -r '.data.cookieName // .cookieName // "XSRF-TOKEN"')
    else
        CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
        CSRF_COOKIE_NAME="XSRF-TOKEN"
    fi

    if [ -n "$CSRF_TOKEN" ] && [ "$CSRF_TOKEN" != "null" ]; then
        echo "  ✅ CSRF Token获取成功"
        # 手动设置Cookie
        echo -e "\\tlocalhost\\tFALSE\\t/\\tFALSE\\t0\\t$CSRF_COOKIE_NAME\\t$CSRF_TOKEN" >> "$COOKIE_JAR"
    else
        echo "  ❌ CSRF Token获取失败"
        return 1
    fi

    # 测试发布通知
    echo "  📢 测试通知发布..."
    TEST_JSON=$(cat <<EOF
{
    "title": "角色测试: $role_desc",
    "content": "# 角色权限测试\\n\\n**测试角色**: $role_desc\\n**角色代码**: $ROLE_CODE\\n**测试时间**: $(date '+%Y年%m月%d日 %H:%M:%S')\\n\\n这是一条角色权限测试通知。",
    "level": 4,
    "categoryId": 5,
    "targetScope": "SCHOOL_WIDE",
    "pushChannels": [1, 5],
    "requireConfirm": false,
    "pinned": false
}
EOF
)

    PUBLISH_RESPONSE=$(curl -s -w "\\nHTTP_STATUS:%{http_code}" \
        -b "$COOKIE_JAR" \
        -X POST "$PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
        -d "$TEST_JSON")

    # 分析结果
    HTTP_STATUS=$(echo "$PUBLISH_RESPONSE" | tail -n1 | cut -d: -f2)
    RESPONSE_BODY=$(echo "$PUBLISH_RESPONSE" | sed '$d')

    echo "  📊 发布结果："
    echo "     状态码: $HTTP_STATUS"
    echo "     响应: $RESPONSE_BODY"

    if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ]; then
        echo "  🎉 $role_desc 角色权限验证成功！"
        return 0
    else
        echo "  ❌ $role_desc 角色权限验证失败"
        return 1
    fi
}

# 主测试循环
successful_roles=0
total_roles=${#ROLES[@]}

for role_info in "${ROLES[@]}"; do
    IFS=':' read -r employee_id name password role_desc <<< "$role_info"

    if test_role "$employee_id" "$name" "$password" "$role_desc"; then
        ((successful_roles++))
    fi

    # 清理Cookie文件
    rm -f "$COOKIE_JAR"
done

echo ""
echo "======================================="
echo "📊 角色权限测试总结："
echo "成功: $successful_roles/$total_roles 个角色"

if [ $successful_roles -gt 0 ]; then
    echo "✅ 找到可用角色，可以继续开发分级通知脚本"
else
    echo "❌ 所有角色都失败，需要检查权限配置"
fi
echo "======================================="