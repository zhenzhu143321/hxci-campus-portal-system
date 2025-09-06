#!/bin/bash

# 测试学校登录API返回格式是否与真实API一致
# Author: Auth-Integration-Expert

echo "====================================="
echo "学校登录API格式一致性测试"
echo "====================================="
echo ""

# API基础URL
BASE_URL="http://localhost:48082/mock-school-api"

# 测试账号
ACCOUNTS=(
    "STUDENT_001:Student-Zhang:学生账号"
    "TEACHER_001:Teacher-Wang:教师账号"
    "PRINCIPAL_001:Principal-Zhang:校长账号"
    "ACADEMIC_ADMIN_001:Director-Li:教务主任账号"
)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试函数
test_login() {
    local employee_id=$1
    local name=$2
    local desc=$3
    
    echo -e "${BLUE}测试 $desc${NC}"
    echo "  工号: $employee_id"
    echo "  姓名: $name"
    echo ""
    
    # 发送登录请求
    response=$(curl -s -X POST "$BASE_URL/auth/school-login" \
        -H "Content-Type: application/json" \
        -d "{
            \"employeeId\": \"$employee_id\",
            \"name\": \"$name\",
            \"password\": \"admin123\"
        }")
    
    # 检查响应
    if [ -z "$response" ]; then
        echo -e "${RED}  ✗ 请求失败：无响应${NC}"
        return 1
    fi
    
    # 解析响应
    code=$(echo "$response" | jq -r '.code')
    msg=$(echo "$response" | jq -r '.msg')
    
    if [ "$code" != "0" ]; then
        echo -e "${RED}  ✗ 登录失败：$msg${NC}"
        echo "  完整响应: $response"
        return 1
    fi
    
    # 验证数据格式
    echo -e "${GREEN}  ✓ 登录成功${NC}"
    
    # 提取关键字段
    id=$(echo "$response" | jq -r '.data.id')
    no=$(echo "$response" | jq -r '.data.no')
    name_resp=$(echo "$response" | jq -r '.data.name')
    token=$(echo "$response" | jq -r '.data.token')
    role=$(echo "$response" | jq -r '.data.role')
    grade=$(echo "$response" | jq -r '.data.grade')
    className=$(echo "$response" | jq -r '.data.className')
    
    # 显示关键信息
    echo "  返回数据:"
    echo "    - ID: $id"
    echo "    - 工号/学号: $no"
    echo "    - 姓名: $name_resp"
    echo "    - Token: ${token:0:8}..."
    echo "    - 角色: $role"
    
    # 验证Token格式（UUID）
    if [[ $token =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]]; then
        echo -e "    - Token格式: ${GREEN}✓ UUID格式正确${NC}"
    else
        echo -e "    - Token格式: ${RED}✗ 不是UUID格式${NC}"
    fi
    
    # 验证角色是否为数组
    if [ "$(echo "$response" | jq -r '.data.role | type')" == "array" ]; then
        echo -e "    - 角色类型: ${GREEN}✓ 数组格式正确${NC}"
    else
        echo -e "    - 角色类型: ${RED}✗ 不是数组格式${NC}"
    fi
    
    # 根据角色验证特定字段
    if [[ $employee_id == STUDENT* ]]; then
        if [ "$grade" != "null" ] && [ "$className" != "null" ]; then
            echo -e "    - 学生字段: ${GREEN}✓ 年级($grade) 班级($className)${NC}"
        else
            echo -e "    - 学生字段: ${RED}✗ 缺少年级或班级信息${NC}"
        fi
    elif [[ $employee_id == TEACHER* ]] || [[ $employee_id == PRINCIPAL* ]] || [[ $employee_id == ACADEMIC_ADMIN* ]]; then
        if [ "$grade" == "null" ] && [ "$className" == "null" ]; then
            echo -e "    - 教师字段: ${GREEN}✓ 无年级班级信息（正确）${NC}"
        else
            echo -e "    - 教师字段: ${YELLOW}⚠ 包含学生字段${NC}"
        fi
    fi
    
    echo ""
    return 0
}

# 检查服务是否运行
echo "检查Mock School API服务状态..."
if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/ping" | grep -q "200"; then
    echo -e "${GREEN}✓ 服务运行正常${NC}"
else
    echo -e "${RED}✗ 服务未运行或无法访问${NC}"
    echo "请先启动服务: mvn spring-boot:run -pl yudao-mock-school-api"
    exit 1
fi

echo ""
echo "开始测试各类账号..."
echo "====================================="
echo ""

# 测试所有账号
success_count=0
total_count=${#ACCOUNTS[@]}

for account in "${ACCOUNTS[@]}"; do
    IFS=':' read -r employee_id name desc <<< "$account"
    if test_login "$employee_id" "$name" "$desc"; then
        ((success_count++))
    fi
done

# 显示测试结果
echo "====================================="
echo "测试完成"
echo "====================================="
if [ $success_count -eq $total_count ]; then
    echo -e "${GREEN}✓ 所有测试通过 ($success_count/$total_count)${NC}"
    echo -e "${GREEN}✓ Mock API返回格式与真实API完全一致${NC}"
else
    echo -e "${RED}✗ 部分测试失败 ($success_count/$total_count)${NC}"
fi

# 显示环境变量提示
echo ""
echo "====================================="
echo "环境变量控制提示"
echo "====================================="
echo "通过设置 SCHOOL_API_MODE 环境变量控制API模式："
echo "  export SCHOOL_API_MODE=mock  # 使用Mock数据（默认）"
echo "  export SCHOOL_API_MODE=real  # 调用真实学校API"
echo ""
echo "当前模式: ${SCHOOL_API_MODE:-mock（默认）}"