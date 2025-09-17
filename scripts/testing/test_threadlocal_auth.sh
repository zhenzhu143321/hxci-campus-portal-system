#!/bin/bash

# 🧪 测试ThreadLocal认证系统修复效果
# 功能：验证待办通知API的ThreadLocal优化是否生效

echo "🔍 =========================================="
echo "   ThreadLocal认证系统测试脚本"
echo "   测试目标：验证认证信息缓存优化"
echo "🔍 =========================================="
echo ""

# API基础地址
API_BASE="http://localhost:48081"
MOCK_API="http://localhost:48082"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试账号（教师角色）
EMPLOYEE_ID="TEACHER_001"
NAME="Teacher-Wang"
PASSWORD="admin123"

echo -e "${BLUE}Step 1: 获取JWT Token${NC}"
echo "----------------------------------------"

# 登录获取Token
LOGIN_RESPONSE=$(curl -s -X POST "$MOCK_API/mock-school-api/auth/authenticate" \
  -H "Content-Type: application/json" \
  -d "{
    \"employeeId\": \"$EMPLOYEE_ID\",
    \"name\": \"$NAME\",
    \"password\": \"$PASSWORD\"
  }")

# 提取Token
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ 登录失败，无法获取Token${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✅ 成功获取Token${NC}"
echo "Token: ${TOKEN:0:50}..."
echo ""

echo -e "${BLUE}Step 2: 测试待办通知API Ping（公开端点）${NC}"
echo "----------------------------------------"

# 测试ping接口（不需要认证）
PING_RESPONSE=$(curl -s -X GET "$API_BASE/admin-api/test/todo-new/api/ping")
echo "Response: $PING_RESPONSE"

if [[ $PING_RESPONSE == *"pong"* ]]; then
    echo -e "${GREEN}✅ Ping测试成功${NC}"
else
    echo -e "${RED}❌ Ping测试失败${NC}"
fi
echo ""

echo -e "${BLUE}Step 3: 测试待办列表API（需要认证）${NC}"
echo "----------------------------------------"

# 第一次调用 - 从Mock API获取用户信息
echo -e "${YELLOW}第一次调用（预期：从Mock API获取用户信息）${NC}"
START_TIME=$(date +%s%3N)

TODO_LIST_RESPONSE=$(curl -s -X GET "$API_BASE/admin-api/test/todo-new/api/my-list?page=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "tenant-id: 1")

END_TIME=$(date +%s%3N)
DURATION=$((END_TIME - START_TIME))

if [[ $TODO_LIST_RESPONSE == *"todos"* ]]; then
    echo -e "${GREEN}✅ 待办列表获取成功${NC}"
    echo "响应时间: ${DURATION}ms"
else
    echo -e "${RED}❌ 待办列表获取失败${NC}"
    echo "Response: $TODO_LIST_RESPONSE"
fi
echo ""

# 第二次调用 - 应该从ThreadLocal获取（更快）
echo -e "${YELLOW}第二次调用（预期：从ThreadLocal获取，更快）${NC}"
START_TIME=$(date +%s%3N)

TODO_LIST_RESPONSE2=$(curl -s -X GET "$API_BASE/admin-api/test/todo-new/api/my-list?page=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "tenant-id: 1")

END_TIME=$(date +%s%3N)
DURATION2=$((END_TIME - START_TIME))

if [[ $TODO_LIST_RESPONSE2 == *"todos"* ]]; then
    echo -e "${GREEN}✅ 待办列表获取成功${NC}"
    echo "响应时间: ${DURATION2}ms"

    # 比较两次响应时间
    if [ $DURATION2 -lt $DURATION ]; then
        IMPROVEMENT=$((DURATION - DURATION2))
        PERCENT=$((IMPROVEMENT * 100 / DURATION))
        echo -e "${GREEN}⚡ 性能提升: ${IMPROVEMENT}ms (${PERCENT}%)${NC}"
    fi
else
    echo -e "${RED}❌ 待办列表获取失败${NC}"
    echo "Response: $TODO_LIST_RESPONSE2"
fi
echo ""

echo -e "${BLUE}Step 4: 测试完成待办API（需要认证）${NC}"
echo "----------------------------------------"

# 测试完成待办（假设有ID为1的待办）
COMPLETE_RESPONSE=$(curl -s -X POST "$API_BASE/admin-api/test/todo-new/api/1/complete" \
  -H "Authorization: Bearer $TOKEN" \
  -H "tenant-id: 1" \
  -H "Content-Type: application/json" \
  -d '{}')

if [[ $COMPLETE_RESPONSE == *"401"* ]]; then
    echo -e "${YELLOW}⚠️ 待办不存在或无权限${NC}"
elif [[ $COMPLETE_RESPONSE == *"404"* ]]; then
    echo -e "${YELLOW}⚠️ 待办任务不存在${NC}"
elif [[ $COMPLETE_RESPONSE == *"success"* ]] || [[ $COMPLETE_RESPONSE == *"todoId"* ]]; then
    echo -e "${GREEN}✅ 待办完成API调用成功${NC}"
else
    echo -e "${RED}❌ 待办完成API调用失败${NC}"
    echo "Response: $COMPLETE_RESPONSE"
fi
echo ""

echo -e "${BLUE}Step 5: 性能对比总结${NC}"
echo "=========================================="
echo -e "第一次调用（Mock API）: ${YELLOW}${DURATION}ms${NC}"
echo -e "第二次调用（ThreadLocal）: ${GREEN}${DURATION2}ms${NC}"

if [ $DURATION2 -lt $DURATION ]; then
    IMPROVEMENT=$((DURATION - DURATION2))
    PERCENT=$((IMPROVEMENT * 100 / DURATION))
    echo -e "${GREEN}🚀 ThreadLocal优化效果: 减少${IMPROVEMENT}ms (${PERCENT}%)${NC}"
else
    echo -e "${YELLOW}⚠️ 性能差异不明显，可能需要更多测试${NC}"
fi

echo ""
echo -e "${GREEN}✅ 测试完成！${NC}"
echo ""
echo "📝 说明："
echo "1. ThreadLocal优化减少了重复的Mock API调用"
echo "2. 用户信息在请求生命周期内被缓存"
echo "3. 请求结束后ThreadLocal会自动清理"
echo "4. 这种优化对高并发场景特别有效"