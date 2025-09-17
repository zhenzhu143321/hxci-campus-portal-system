#!/bin/bash

# P0级 - 服务健康检查脚本
# 验证所有端口和基础API响应时间
# 基于全面后端API测试计划 - P0.1部分

echo "🏥 P0级 - 服务健康检查"
echo "========================================="

# 检查是否为快速模式
QUICK_MODE=false
if [[ "$1" == "--quick" ]]; then
    QUICK_MODE=true
    echo "⚡ 快速模式已启用"
fi

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 记录测试结果函数
record_result() {
    local result=$1
    local test_name=$2

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$result" = "0" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "✅ $test_name - 通过"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "❌ $test_name - 失败"
    fi
}

# 端口连通性检查函数
check_port() {
    local port=$1
    local service=$2

    if command -v nc >/dev/null 2>&1; then
        if nc -z localhost $port 2>/dev/null; then
            record_result 0 "$service ($port) 端口连通性"
            return 0
        else
            record_result 1 "$service ($port) 端口连通性"
            return 1
        fi
    else
        # 使用telnet作为备选方案
        if timeout 3 bash -c "echo >/dev/tcp/localhost/$port" 2>/dev/null; then
            record_result 0 "$service ($port) 端口连通性"
            return 0
        else
            record_result 1 "$service ($port) 端口连通性"
            return 1
        fi
    fi
}

# API响应时间检查函数
check_response_time() {
    local url=$1
    local name=$2
    local max_time=$3

    echo "📊 检查 $name 响应时间..."

    # 使用curl检测响应时间
    if response_time=$(curl -o /dev/null -s -w "%{time_total}" --max-time 10 "$url" 2>/dev/null); then
        # 检查响应时间是否在可接受范围内
        if command -v bc >/dev/null 2>&1; then
            if (( $(echo "$response_time < $max_time" | bc -l) )); then
                record_result 0 "$name 响应时间 (${response_time}s < ${max_time}s)"
                return 0
            else
                record_result 1 "$name 响应时间过长 (${response_time}s > ${max_time}s)"
                return 1
            fi
        else
            # 简单的整数比较 (将秒转换为毫秒)
            response_ms=$(echo "$response_time * 1000" | awk '{print int($1)}')
            max_ms=$(echo "$max_time * 1000" | awk '{print int($1)}')
            if [ "$response_ms" -lt "$max_ms" ]; then
                record_result 0 "$name 响应时间 (${response_time}s < ${max_time}s)"
                return 0
            else
                record_result 1 "$name 响应时间过长 (${response_time}s > ${max_time}s)"
                return 1
            fi
        fi
    else
        record_result 1 "$name API无响应或超时"
        return 1
    fi
}

# 检查API返回状态码
check_api_status() {
    local url=$1
    local name=$2
    local expected_code=$3

    echo "🌐 检查 $name API状态..."

    http_code=$(curl -o /dev/null -s -w "%{http_code}" --max-time 10 "$url" 2>/dev/null)

    if [ "$http_code" = "$expected_code" ]; then
        record_result 0 "$name API状态码 ($http_code)"
        return 0
    else
        record_result 1 "$name API状态码异常 (得到: $http_code, 期望: $expected_code)"
        return 1
    fi
}

# 主要测试执行
echo ""
echo "🔍 Step 1: 端口连通性检查"
echo "----------------------------------------"

check_port 48081 "主通知服务"
check_port 48082 "Mock School API"

echo ""
echo "⏱️ Step 2: API响应时间检查"
echo "----------------------------------------"

# 基础API响应时间检查
check_response_time "http://localhost:48081/csrf-token" "CSRF Token API" 3.0
check_response_time "http://localhost:48082/mock-school-api/auth/health" "Mock School API Health" 2.0

if [ "$QUICK_MODE" = false ]; then
    # 详细API响应时间检查 (非快速模式)
    echo ""
    echo "📡 Step 3: 详细API端点检查"
    echo "----------------------------------------"

    # 检查主要API端点的可达性
    check_api_status "http://localhost:48081/csrf-token" "CSRF Token" "200"
    check_api_status "http://localhost:48082/mock-school-api/auth/health" "Mock School API Health" "200"

    # 检查认证相关API端点
    echo ""
    echo "🔐 Step 4: 认证系统基础检查"
    echo "----------------------------------------"

    # 测试认证API端点可达性
    check_response_time "http://localhost:48082/mock-school-api/auth/authenticate" "认证API" 5.0

    # 测试通知API端点可达性 (配置了@PermitAll，预期200)
    check_api_status "http://localhost:48081/admin-api/test/notification/api/ping" "通知系统Ping" "200"

    echo ""
    echo "📝 Step 5: 新功能API检查"
    echo "----------------------------------------"

    # 检查待办通知优化功能API
    check_api_status "http://localhost:48081/admin-api/test/todo-new/api/ping" "新待办API Ping" "200"

    # 检查权限缓存API
    check_response_time "http://localhost:48081/admin-api/test/permission-cache/api/ping" "权限缓存API Ping" 3.0
fi

echo ""
echo "📊 测试结果统计"
echo "========================================="
echo "总测试数: $TOTAL_TESTS"
echo "通过数: $PASSED_TESTS"
echo "失败数: $FAILED_TESTS"
echo "成功率: $(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%"

echo ""
if [ "$FAILED_TESTS" -eq 0 ]; then
    echo "🎉 P0级服务健康检查 - 全部通过!"
    echo "✅ 系统基础服务状态良好，可以继续后续测试"
    exit 0
else
    echo "⚠️ P0级服务健康检查 - 发现问题!"
    echo "❌ 有 $FAILED_TESTS 项测试失败，请先解决基础服务问题"

    # 提供故障排查建议
    echo ""
    echo "🔧 故障排查建议："
    echo "1. 检查服务是否正常启动："
    echo "   sudo netstat -tlnp | grep :48081"
    echo "   sudo netstat -tlnp | grep :48082"
    echo ""
    echo "2. 检查服务日志："
    echo "   查看Java服务启动日志中的错误信息"
    echo ""
    echo "3. 重启服务："
    echo "   sudo pkill -f java"
    echo "   然后手动重启两个服务"

    exit 1
fi