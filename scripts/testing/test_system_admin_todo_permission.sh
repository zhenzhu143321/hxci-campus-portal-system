#!/bin/bash

# 测试SYSTEM_ADMIN待办通知发布权限修复
# 验证修复ACL权限冲突问题

echo "🔍 测试SYSTEM_ADMIN待办通知发布权限..."
echo "================================================="

# 服务配置
AUTH_SERVER="http://localhost:48082"
API_SERVER="http://localhost:48081"

# 测试账号
EMPLOYEE_ID="SYSTEM_ADMIN_001"
NAME="系统管理员"
PASSWORD="admin123"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 步骤1: 登录获取JWT Token
echo -e "\n${YELLOW}Step 1: 系统管理员登录...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${AUTH_SERVER}/mock-school-api/auth/authenticate" \
    -H "Content-Type: application/json" \
    -d "{
        \"employeeId\": \"${EMPLOYEE_ID}\",
        \"name\": \"${NAME}\",
        \"password\": \"${PASSWORD}\"
    }")

# 提取JWT Token (修复: 使用accessToken字段)
JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
    echo -e "${RED}❌ 登录失败，无法获取JWT Token${NC}"
    echo "登录响应: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✅ 登录成功，获取到JWT Token${NC}"
echo "Token: ${JWT_TOKEN:0:50}..."

# 步骤2: 获取CSRF Token
echo -e "\n${YELLOW}Step 2: 获取CSRF Token...${NC}"
CSRF_TOKEN=$(curl -s "${API_SERVER}/csrf-token" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$CSRF_TOKEN" ]; then
    echo -e "${RED}❌ 无法获取CSRF Token${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 获取CSRF Token成功${NC}"
echo "CSRF Token: ${CSRF_TOKEN:0:30}..."

# 步骤3: 测试待办通知发布（SCHOOL_WIDE范围）
echo -e "\n${YELLOW}Step 3: 测试发布SCHOOL_WIDE范围待办通知...${NC}"

# 生成唯一的待办标题
TIMESTAMP=$(date +%s)
TODO_TITLE="系统管理员测试待办_${TIMESTAMP}"

PUBLISH_RESPONSE=$(curl -s -X POST "${API_SERVER}/admin-api/test/todo-new/api/publish" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "tenant-id: 1" \
    -H "Cookie: XSRF-TOKEN=${CSRF_TOKEN}" \
    -H "X-XSRF-TOKEN: ${CSRF_TOKEN}" \
    -d "{
        \"title\": \"${TODO_TITLE}\",
        \"content\": \"这是系统管理员发布的全校范围待办通知测试\",
        \"priority\": \"high\",
        \"dueDate\": \"2025-12-31T23:59:59\",
        \"targetScope\": \"SCHOOL_WIDE\",
        \"targetStudentIds\": [],
        \"targetGradeIds\": [],
        \"targetClassIds\": [],
        \"targetDepartmentIds\": []
    }")

# 检查响应
if echo "$PUBLISH_RESPONSE" | grep -q "\"code\":0"; then
    echo -e "${GREEN}✅ 待办通知发布成功！${NC}"
    TODO_ID=$(echo "$PUBLISH_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
    echo "待办ID: $TODO_ID"
    echo "待办标题: $TODO_TITLE"

    # 显示完整响应
    echo -e "\n${GREEN}完整响应:${NC}"
    echo "$PUBLISH_RESPONSE" | python3 -m json.tool

    echo -e "\n${GREEN}🎉 测试通过！SYSTEM_ADMIN权限修复成功！${NC}"
    echo "================================================="
    echo "✅ SYSTEM_ADMIN可以成功发布SCHOOL_WIDE范围的待办通知"
    echo "✅ ACL权限冲突问题已解决"
    echo "✅ @PreAuthorize和业务层权限验证一致"

elif echo "$PUBLISH_RESPONSE" | grep -q "ACL_DENIED"; then
    echo -e "${RED}❌ 待办通知发布失败 - ACL权限被拒绝${NC}"
    echo "错误响应: $PUBLISH_RESPONSE"
    echo -e "\n${RED}⚠️ 问题仍然存在：ACL检查仍在阻止SYSTEM_ADMIN${NC}"
    exit 1
elif echo "$PUBLISH_RESPONSE" | grep -q "TODO_CREATE_SCHOOL"; then
    echo -e "${RED}❌ 待办通知发布失败 - TODO_CREATE_SCHOOL权限缺失${NC}"
    echo "错误响应: $PUBLISH_RESPONSE"
    echo -e "\n${RED}⚠️ 问题仍然存在：TODO_CREATE_SCHOOL权限检查失败${NC}"
    exit 1
else
    echo -e "${RED}❌ 待办通知发布失败${NC}"
    echo "错误响应: $PUBLISH_RESPONSE"
    exit 1
fi

# 步骤4: 测试其他范围
echo -e "\n${YELLOW}Step 4: 测试其他范围权限...${NC}"

# 测试DEPARTMENT范围
echo -e "\n测试DEPARTMENT范围..."
DEPT_RESPONSE=$(curl -s -X POST "${API_SERVER}/admin-api/test/todo-new/api/publish" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "tenant-id: 1" \
    -H "Cookie: XSRF-TOKEN=${CSRF_TOKEN}" \
    -H "X-XSRF-TOKEN: ${CSRF_TOKEN}" \
    -d "{
        \"title\": \"部门待办测试_${TIMESTAMP}\",
        \"content\": \"部门范围待办测试\",
        \"priority\": \"medium\",
        \"dueDate\": \"2025-12-31\",
        \"targetScope\": \"DEPARTMENT\"
    }")

if echo "$DEPT_RESPONSE" | grep -q "\"code\":0"; then
    echo -e "${GREEN}✅ DEPARTMENT范围发布成功${NC}"
else
    echo -e "${RED}❌ DEPARTMENT范围发布失败${NC}"
fi

# 测试CLASS范围
echo -e "\n测试CLASS范围..."
CLASS_RESPONSE=$(curl -s -X POST "${API_SERVER}/admin-api/test/todo-new/api/publish" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "tenant-id: 1" \
    -H "Cookie: XSRF-TOKEN=${CSRF_TOKEN}" \
    -H "X-XSRF-TOKEN: ${CSRF_TOKEN}" \
    -d "{
        \"title\": \"班级待办测试_${TIMESTAMP}\",
        \"content\": \"班级范围待办测试\",
        \"priority\": \"low\",
        \"dueDate\": \"2025-12-31\",
        \"targetScope\": \"CLASS\"
    }")

if echo "$CLASS_RESPONSE" | grep -q "\"code\":0"; then
    echo -e "${GREEN}✅ CLASS范围发布成功${NC}"
else
    echo -e "${RED}❌ CLASS范围发布失败${NC}"
fi

echo -e "\n================================================="
echo -e "${GREEN}测试完成！${NC}"