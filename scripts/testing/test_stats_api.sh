#!/bin/bash

# API统计功能测试脚本
# 测试目标：验证 /admin-api/test/todo-new/api/{id}/stats 端点的完整功能

echo "=== 哈尔滨信息工程学院校园门户系统 - 统计API测试报告 ==="
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "======================================================"

# 认证函数
get_auth_token() {
    local role=$1
    local employee_id=$2
    local name=$3
    local password="admin123"
    
    response=$(curl -X POST "http://localhost:48082/mock-school-api/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d "{
            \"employeeId\": \"$employee_id\",
            \"name\": \"$name\", 
            \"password\": \"$password\"
        }" 2>/dev/null)
    
    token=$(echo $response | jq -r '.data.accessToken // empty')
    echo $token
}

# 获取统计数据函数
get_stats() {
    local todo_id=$1
    local token=$2
    local role_name=$3
    
    echo "--- 测试 ${role_name} 角色访问待办 ID=${todo_id} 的统计数据 ---"
    
    response=$(curl -X GET "http://localhost:48081/admin-api/test/todo-new/api/${todo_id}/stats" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -H "tenant-id: 1" 2>/dev/null)
    
    echo "响应结果:"
    echo $response | jq '.'
    echo ""
}

# 获取三个角色的Token
echo "1. 获取认证Token..."
PRINCIPAL_TOKEN=$(get_auth_token "PRINCIPAL" "PRINCIPAL_001" "Principal-Zhang")
TEACHER_TOKEN=$(get_auth_token "TEACHER" "TEACHER_001" "Teacher-Wang")
STUDENT_TOKEN=$(get_auth_token "STUDENT" "STUDENT_001" "Student-Zhang")

echo "校长Token: ${PRINCIPAL_TOKEN:0:50}..."
echo "教师Token: ${TEACHER_TOKEN:0:50}..."
echo "学生Token: ${STUDENT_TOKEN:0:50}..."
echo ""

# 测试不同待办的统计数据
echo "2. 开始统计API功能测试..."
echo ""

# 测试ID=1的全校通知统计
get_stats 1 "$PRINCIPAL_TOKEN" "校长"
get_stats 1 "$TEACHER_TOKEN" "教师"  
get_stats 1 "$STUDENT_TOKEN" "学生"

# 测试ID=6的个人通知统计
get_stats 6 "$PRINCIPAL_TOKEN" "校长"
get_stats 6 "$TEACHER_TOKEN" "教师"
get_stats 6 "$STUDENT_TOKEN" "学生"

echo "3. 测试数据准确性验证..."
echo "--- 数据库实际完成记录对比 ---"
mysql -u root ruoyi-vue-pro -e "
SELECT 
    tn.id,
    tn.title,
    COUNT(tc.id) as total_completions,
    SUM(CASE WHEN tc.user_role = 'STUDENT' THEN 1 ELSE 0 END) as student_completions,
    SUM(CASE WHEN tc.user_role = 'TEACHER' THEN 1 ELSE 0 END) as teacher_completions,
    SUM(CASE WHEN tc.user_role = 'PRINCIPAL' THEN 1 ELSE 0 END) as principal_completions
FROM todo_notifications tn 
LEFT JOIN todo_completions tc ON tn.id = tc.todo_id 
WHERE tn.id IN (1, 6)
GROUP BY tn.id, tn.title
ORDER BY tn.id;
"

echo ""
echo "=== 测试完成 ==="