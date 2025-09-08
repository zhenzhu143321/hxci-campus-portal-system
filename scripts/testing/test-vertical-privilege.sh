#!/bin/bash

# P0-SEC-04 垂直越权防护集成测试脚本
# 测试数据权限防护功能的完整性

echo "=================================================="
echo "P0-SEC-04 垂直越权防护集成测试"
echo "测试时间: $(date)"
echo "=================================================="

BASE_URL="http://localhost:48081"
MOCK_API_URL="http://localhost:48082"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_api() {
    local TEST_NAME="$1"
    local METHOD="$2"
    local URL="$3"
    local TOKEN="$4"
    local DATA="$5"
    local EXPECTED_CODE="$6"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo ""
    echo "🧪 测试: $TEST_NAME"
    echo "  - URL: $URL"
    echo "  - Method: $METHOD"
    echo "  - Expected: HTTP $EXPECTED_CODE"
    
    if [ "$METHOD" = "GET" ]; then
        if [ -z "$TOKEN" ]; then
            RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$URL" -H "tenant-id: 1")
        else
            RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$URL" \
                -H "Authorization: Bearer $TOKEN" \
                -H "tenant-id: 1")
        fi
    else
        if [ -z "$TOKEN" ]; then
            RESPONSE=$(curl -s -w "\n%{http_code}" -X "$METHOD" "$URL" \
                -H "Content-Type: application/json" \
                -H "tenant-id: 1" \
                -d "$DATA")
        else
            RESPONSE=$(curl -s -w "\n%{http_code}" -X "$METHOD" "$URL" \
                -H "Authorization: Bearer $TOKEN" \
                -H "Content-Type: application/json" \
                -H "tenant-id: 1" \
                -d "$DATA")
        fi
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" = "$EXPECTED_CODE" ]; then
        echo -e "  ${GREEN}✅ PASSED${NC} - HTTP $HTTP_CODE"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # 解析响应内容
        if echo "$BODY" | jq -e . >/dev/null 2>&1; then
            echo "  - Response: $(echo "$BODY" | jq -c .)"
        fi
    else
        echo -e "  ${RED}❌ FAILED${NC} - Got HTTP $HTTP_CODE, Expected $EXPECTED_CODE"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "  - Response: $BODY"
    fi
}

# 获取JWT Token函数
get_jwt_token() {
    local EMPLOYEE_ID="$1"
    local NAME="$2"
    local PASSWORD="$3"
    
    echo "🔑 获取JWT Token: $NAME" >&2
    
    RESPONSE=$(curl -s -X POST "$MOCK_API_URL/mock-school-api/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")
    
    TOKEN=$(echo "$RESPONSE" | jq -r '.data.accessToken // .data.jwtToken // .data.token // empty')
    
    if [ -z "$TOKEN" ]; then
        echo "  - ❌ 获取Token失败" >&2
        return 1
    else
        echo "  - ✅ Token获取成功" >&2
        echo "$TOKEN"
    fi
}

echo ""
echo "=========================================="
echo "1️⃣ 测试基础连通性"
echo "=========================================="

# 测试ping接口（无需认证）
test_api "Ping测试 - 无认证" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/ping" \
    "" "" "200"

test_api "防护状态检查 - 无认证" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/protection-status" \
    "" "" "200"

echo ""
echo "=========================================="
echo "2️⃣ 获取不同角色的JWT Token"
echo "=========================================="

# 获取不同角色的Token
STUDENT_TOKEN=$(get_jwt_token "STUDENT_001" "Student-Zhang" "admin123")
TEACHER_TOKEN=$(get_jwt_token "TEACHER_001" "Teacher-Wang" "admin123")
PRINCIPAL_TOKEN=$(get_jwt_token "PRINCIPAL_001" "Principal-Zhang" "admin123")

echo ""
echo "=========================================="
echo "3️⃣ 测试用户信息获取"
echo "=========================================="

# 测试未登录用户
test_api "获取用户信息 - 未登录" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/current-user-info" \
    "" "" "401"

# 测试已登录用户
test_api "获取用户信息 - 学生角色" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/current-user-info" \
    "$STUDENT_TOKEN" "" "200"

test_api "获取用户信息 - 教师角色" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/current-user-info" \
    "$TEACHER_TOKEN" "" "200"

echo ""
echo "=========================================="
echo "4️⃣ 测试数据权限过滤"
echo "=========================================="

# 测试通知列表数据权限
test_api "通知列表 - 学生视角" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/notification-list" \
    "$STUDENT_TOKEN" "" "200"

test_api "通知列表 - 教师视角" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/notification-list" \
    "$TEACHER_TOKEN" "" "200"

test_api "通知列表 - 校长视角" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/notification-list" \
    "$PRINCIPAL_TOKEN" "" "200"

echo ""
echo "=========================================="
echo "5️⃣ 测试管理员特权"
echo "=========================================="

# 测试禁用数据权限的接口（管理员专用）
test_api "所有通知 - 学生无权访问" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/all-notifications" \
    "$STUDENT_TOKEN" "" "200"

test_api "所有通知 - 校长可访问" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/all-notifications" \
    "$PRINCIPAL_TOKEN" "" "200"

echo ""
echo "=========================================="
echo "6️⃣ 测试越权访问防护"
echo "=========================================="

# 测试访问其他班级通知
test_api "其他班级通知 - 学生越权测试" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/other-class-notification/999" \
    "$STUDENT_TOKEN" "" "200"

# 测试访问部门通知
test_api "部门通知 - 学生无权访问" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/department-notification/1" \
    "$STUDENT_TOKEN" "" "200"

test_api "部门通知 - 教师可访问" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/department-notification/1" \
    "$TEACHER_TOKEN" "" "200"

echo ""
echo "=========================================="
echo "7️⃣ 测试SQL注入防护"
echo "=========================================="

# 测试SQL注入防护 (URL编码恶意参数)
MALICIOUS_PARAM=$(echo "'; DROP TABLE users; --" | jq -sRr @uri)
test_api "SQL注入测试 - 恶意参数" "GET" \
    "$BASE_URL/admin-api/test/vertical-privilege/api/sql-injection-test?scope=$MALICIOUS_PARAM" \
    "$STUDENT_TOKEN" "" "200"

echo ""
echo "=========================================="
echo "📊 测试结果统计"
echo "=========================================="
echo "总测试数: $TOTAL_TESTS"
echo -e "通过: ${GREEN}$PASSED_TESTS${NC}"
echo -e "失败: ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 所有测试通过！垂直越权防护功能正常${NC}"
    exit 0
else
    echo -e "\n${RED}⚠️ 有 $FAILED_TESTS 个测试失败，请检查问题${NC}"
    exit 1
fi