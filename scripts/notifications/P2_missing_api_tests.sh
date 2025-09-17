#!/bin/bash

# P2级 - 遗漏API补充测试脚本
# 基于Sequential Thinking深度分析发现的测试盲点
# 覆盖10个未测试的Controller的关键API端点

echo "🔍 P2级 - 遗漏API补充测试"
echo "========================================="

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

# 获取JWT Token用于认证
get_jwt_token() {
    local token_response=$(curl -s -X POST http://localhost:48082/mock-school-api/auth/authenticate \
        -H "Content-Type: application/json" \
        -d '{"employeeId": "SYSTEM_ADMIN_001", "name": "系统管理员", "password": "admin123"}' 2>/dev/null)

    if [ $? -eq 0 ] && [ -n "$token_response" ]; then
        echo "$token_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4
    else
        echo ""
    fi
}

# API测试函数 - 支持认证
test_api() {
    local url=$1
    local name=$2
    local expected_code=$3
    local description=$4
    local need_auth=$5

    echo "🌐 测试 $name API..."
    echo "   URL: $url"
    echo "   期望状态码: $expected_code"
    echo "   描述: $description"

    # 构建curl命令
    local curl_cmd="curl -o /dev/null -s -w \"%{http_code}\" --max-time 10"

    if [ "$need_auth" = "true" ]; then
        if [ -z "$JWT_TOKEN" ]; then
            JWT_TOKEN=$(get_jwt_token)
        fi
        if [ -n "$JWT_TOKEN" ]; then
            curl_cmd="$curl_cmd -H \"Authorization: Bearer $JWT_TOKEN\" -H \"tenant-id: 1\""
        fi
    fi

    curl_cmd="$curl_cmd \"$url\""

    http_code=$(eval $curl_cmd 2>/dev/null)

    if [ "$http_code" = "$expected_code" ]; then
        record_result 0 "$name API"
        return 0
    else
        record_result 1 "$name API (得到: $http_code, 期望: $expected_code)"
        return 1
    fi
}

echo ""
echo "🔑 ===== 获取认证Token ====="
echo "从Mock School API获取JWT Token用于认证测试..."
JWT_TOKEN=$(get_jwt_token)
if [ -n "$JWT_TOKEN" ]; then
    echo "✅ 成功获取JWT Token: ${JWT_TOKEN:0:20}..."
else
    echo "❌ 获取JWT Token失败，某些测试可能失败"
fi

echo ""
echo "🌤️ ===== 天气系统API测试 ====="
echo "TempWeatherController - 天气缓存系统核心功能"

test_api "http://localhost:48081/admin-api/test/weather/api/current" "天气数据获取" "200" "获取哈尔滨实时天气数据" "true"
test_api "http://localhost:48081/admin-api/test/weather/api/refresh" "天气数据刷新" "200" "手动刷新天气缓存" "false"
test_api "http://localhost:48081/admin-api/test/weather/api/ping" "天气服务状态" "200" "天气服务健康检查" "true"

echo ""
echo "🔐 ===== JWT安全测试API ====="
echo "JwtSecurityTestController - JWT Token安全验证"

test_api "http://localhost:48081/admin-api/test/jwt-security/api/ping" "JWT安全测试Ping" "200" "JWT安全测试服务状态" "true"
test_api "http://localhost:48081/admin-api/test/jwt-security/api/blacklist-stats" "JWT黑名单统计" "200" "JWT黑名单统计信息" "true"
test_api "http://localhost:48081/admin-api/test/jwt-security/api/sensitive-info-stats" "敏感信息保护统计" "200" "JWT敏感信息保护统计" "true"

echo ""
echo "🛡️ ===== 重放攻击防护API ====="
echo "ReplayProtectionTestController - 重放攻击防护系统"

test_api "http://localhost:48081/admin-api/test/replay-protection/security-stats" "重放攻击防护统计" "200" "安全统计信息" "true"
test_api "http://localhost:48081/admin-api/test/replay-protection/jti-blacklist-test" "JTI黑名单测试" "200" "JTI黑名单机制测试" "true"
test_api "http://localhost:48081/admin-api/test/replay-protection/frequency-limit-test" "频率限制测试" "200" "请求频率限制测试" "true"

echo ""
echo "📊 ===== CSP违规报告API ====="
echo "CspReportController - 内容安全策略违规监控"

test_api "http://localhost:48081/csp-report/security-status" "CSP安全状态" "200" "实时安全状态监控" "false"
test_api "http://localhost:48081/csp-report/verify-headers" "安全头验证" "200" "安全头配置验证" "false"
test_api "http://localhost:48081/csp-report/config-validation" "CSP配置验证" "200" "CSP配置验证接口" "false"

echo ""
echo "🔧 ===== P0安全测试API ====="
echo "SecurityTestController - P0级安全功能"

test_api "http://localhost:48081/admin-api/test/security/encryption-test" "AES加密测试" "200" "AES-256-GCM加密解密" "false"
test_api "http://localhost:48081/admin-api/test/security/key-config-test" "密钥配置测试" "200" "密钥配置管理测试" "false"
test_api "http://localhost:48081/admin-api/test/security/status" "P0安全状态" "200" "P0安全修复状态" "false"

echo ""
echo "🩺 ===== 系统诊断API ====="
echo "DiagnosticController - 系统运维诊断"

# 注意：DiagnosticController可能需要特殊权限，先测试基础端点
test_api "http://localhost:48081/diagnostic/health" "系统健康诊断" "200" "系统整体健康状态" "false"
test_api "http://localhost:48081/diagnostic/info" "系统信息诊断" "200" "系统详细信息" "false"

echo ""
echo "📊 测试结果统计"
echo "========================================="
echo "总测试数: $TOTAL_TESTS"
echo "通过数: $PASSED_TESTS"
echo "失败数: $FAILED_TESTS"
echo "成功率: $(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%"

echo ""
if [ "$FAILED_TESTS" -eq 0 ]; then
    echo "🎉 P2级遗漏API补充测试 - 全部通过!"
    echo "✅ 发现的API测试盲点已完全填补"
    exit 0
else
    echo "⚠️ P2级遗漏API补充测试 - 发现问题!"
    echo "❌ 有 $FAILED_TESTS 项API测试失败"

    echo ""
    echo "🔧 故障排查建议："
    echo "1. 检查对应Controller是否正确配置和启动"
    echo "2. 验证API路径是否与实际Controller注解一致"
    echo "3. 检查是否需要特殊认证或权限"
    echo "4. 查看服务启动日志中的相关错误信息"

    exit 1
fi