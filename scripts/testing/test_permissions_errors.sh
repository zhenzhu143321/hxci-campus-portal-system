#!/bin/bash

# API统计权限控制和异常处理测试脚本
echo "=== 权限控制和异常处理测试 ==="
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "======================================"

# 获取有效Token
get_token() {
    local employee_id=$1
    local name=$2
    response=$(curl -X POST "http://localhost:48082/mock-school-api/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\": \"$employee_id\", \"name\": \"$name\", \"password\": \"admin123\"}" 2>/dev/null)
    echo $(echo $response | jq -r '.data.accessToken // empty')
}

PRINCIPAL_TOKEN=$(get_token "PRINCIPAL_001" "Principal-Zhang")

echo "1. 异常处理测试 - 无效ID"
echo "--- 测试不存在的待办ID=99999 ---"
curl -X GET "http://localhost:48081/admin-api/test/todo-new/api/99999/stats" \
    -H "Authorization: Bearer $PRINCIPAL_TOKEN" \
    -H "Content-Type: application/json" \
    -H "tenant-id: 1" 2>/dev/null | jq '.'
echo ""

echo "2. 异常处理测试 - 未认证访问"
echo "--- 测试无Token访问 ---"
curl -X GET "http://localhost:48081/admin-api/test/todo-new/api/1/stats" \
    -H "Content-Type: application/json" \
    -H "tenant-id: 1" 2>/dev/null | jq '.'
echo ""

echo "3. 异常处理测试 - 无效Token"
echo "--- 测试无效Token ---"
curl -X GET "http://localhost:48081/admin-api/test/todo-new/api/1/stats" \
    -H "Authorization: Bearer INVALID_TOKEN_12345" \
    -H "Content-Type: application/json" \
    -H "tenant-id: 1" 2>/dev/null | jq '.'
echo ""

echo "4. 异常处理测试 - 缺少tenant-id"
echo "--- 测试缺少tenant-id头 ---"
curl -X GET "http://localhost:48081/admin-api/test/todo-new/api/1/stats" \
    -H "Authorization: Bearer $PRINCIPAL_TOKEN" \
    -H "Content-Type: application/json" 2>/dev/null | jq '.'
echo ""

echo "5. 权限边界测试 - 跨租户访问"
echo "--- 测试tenant-id=999访问 ---"
curl -X GET "http://localhost:48081/admin-api/test/todo-new/api/1/stats" \
    -H "Authorization: Bearer $PRINCIPAL_TOKEN" \
    -H "Content-Type: application/json" \
    -H "tenant-id: 999" 2>/dev/null | jq '.'
echo ""

echo "6. API响应时间测试"
echo "--- 性能基准测试 ---"
for i in {1..5}; do
    echo "测试轮次 $i:"
    start_time=$(date +%s%N)
    response=$(curl -X GET "http://localhost:48081/admin-api/test/todo-new/api/1/stats" \
        -H "Authorization: Bearer $PRINCIPAL_TOKEN" \
        -H "Content-Type: application/json" \
        -H "tenant-id: 1" 2>/dev/null)
    end_time=$(date +%s%N)
    
    duration=$((($end_time - $start_time) / 1000000))
    echo "  响应时间: ${duration}ms"
    echo "  状态码: $(echo $response | jq -r '.code // "N/A"')"
done

echo ""
echo "=== 权限控制和异常处理测试完成 ==="