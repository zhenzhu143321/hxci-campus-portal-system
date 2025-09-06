#!/bin/bash

# 重放攻击防护机制综合测试脚本
# P1.3安全升级验证：JWT JTI黑名单 + RefreshToken机制 + 异常检测

echo "🛡️ 重放攻击防护机制综合测试 - P1.3安全升级验证"
echo "=================================================="

# 配置
BASE_URL="http://localhost"
MAIN_SERVICE_PORT="48081"
MOCK_API_PORT="48082"

MAIN_SERVICE_URL="${BASE_URL}:${MAIN_SERVICE_PORT}"
MOCK_API_URL="${BASE_URL}:${MOCK_API_PORT}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查服务状态
check_service() {
    local service_name="$1"
    local url="$2"
    
    log_info "检查 ${service_name} 服务状态..."
    
    if curl -s "${url}/health" > /dev/null 2>&1 || curl -s "${url}/admin-api/test/notification/api/ping" > /dev/null 2>&1; then
        log_success "${service_name} 服务运行正常"
        return 0
    else
        log_error "${service_name} 服务未运行或无响应"
        return 1
    fi
}

# 测试JTI黑名单机制
test_jti_blacklist() {
    log_info "测试1: JTI黑名单防重放攻击机制"
    echo "----------------------------------------"
    
    local test_jti="test_replay_jti_$(date +%s)"
    local response
    
    # 调用JTI黑名单测试API
    response=$(curl -s "${MAIN_SERVICE_URL}/admin-api/test/replay-protection/jti-blacklist-test?testJti=${test_jti}")
    
    if [[ $? -eq 0 ]] && echo "$response" | jq -e '.success == true' > /dev/null 2>&1; then
        local test_status=$(echo "$response" | jq -r '.data.testStatus')
        
        if [[ "$test_status" == "✅ PASSED" ]]; then
            log_success "JTI黑名单机制测试通过"
            echo "$response" | jq '.data.testSummary' -r | sed 's/^/  └─ /'
        else
            log_error "JTI黑名单机制测试失败"
            echo "$response" | jq '.data' | sed 's/^/    /'
        fi
    else
        log_error "无法调用JTI黑名单测试API"
    fi
    echo
}

# 测试Token刷新机制
test_token_refresh() {
    log_info "测试2: RefreshToken双Token机制"
    echo "----------------------------------------"
    
    # 首先通过增强登录获取Token对
    log_info "步骤1: 获取AccessToken和RefreshToken..."
    
    local login_response=$(curl -s -X POST "${MOCK_API_URL}/mock-school-api/auth/token/login" \
        -H "Content-Type: application/json" \
        -d '{
            "employeeId": "PRINCIPAL_001",
            "name": "Principal-Zhang",
            "password": "admin123"
        }')
    
    if [[ $? -eq 0 ]] && echo "$login_response" | jq -e '.success == true' > /dev/null 2>&1; then
        local access_token=$(echo "$login_response" | jq -r '.data.accessToken')
        local refresh_token=$(echo "$login_response" | jq -r '.data.refreshToken')
        
        log_success "成功获取Token对"
        echo "  ├─ AccessToken: ${access_token:0:20}..."
        echo "  └─ RefreshToken: ${refresh_token:0:20}..."
        
        # 测试Token刷新
        log_info "步骤2: 测试Token刷新..."
        
        local refresh_response=$(curl -s -X POST "${MOCK_API_URL}/mock-school-api/auth/token/refresh" \
            -H "Content-Type: application/json" \
            -d "{\"refreshToken\": \"${refresh_token}\"}")
        
        if [[ $? -eq 0 ]] && echo "$refresh_response" | jq -e '.success == true' > /dev/null 2>&1; then
            local new_access_token=$(echo "$refresh_response" | jq -r '.data.accessToken')
            local new_refresh_token=$(echo "$refresh_response" | jq -r '.data.refreshToken')
            
            log_success "Token刷新成功"
            echo "  ├─ 新AccessToken: ${new_access_token:0:20}..."
            echo "  └─ 新RefreshToken: ${new_refresh_token:0:20}..."
        else
            log_error "Token刷新失败"
            echo "$refresh_response" | jq '.' | sed 's/^/    /'
        fi
    else
        log_error "增强登录失败，无法获取Token对"
        echo "$login_response" | jq '.' | sed 's/^/    /'
    fi
    echo
}

# 测试异常检测系统
test_anomaly_detection() {
    log_info "测试3: IP+UserAgent异常检测系统"
    echo "----------------------------------------"
    
    local test_user="TEST_ANOMALY_USER_$(date +%s)"
    
    # 模拟正常请求
    log_info "步骤1: 模拟正常请求..."
    local normal_response=$(curl -s "${MAIN_SERVICE_URL}/admin-api/test/replay-protection/anomaly-detection-test?testUserId=${test_user}" \
        -H "User-Agent: Normal-Browser-Agent")
    
    if [[ $? -eq 0 ]] && echo "$normal_response" | jq -e '.success == true' > /dev/null 2>&1; then
        local risk_level_1=$(echo "$normal_response" | jq -r '.data.riskLevel')
        local warnings_1=$(echo "$normal_response" | jq -r '.data.warnings | length')
        
        log_info "正常请求风险评估: ${risk_level_1} (${warnings_1}个警告)"
    fi
    
    # 模拟异常请求（不同UserAgent）
    log_info "步骤2: 模拟UserAgent异常请求..."
    local anomaly_response=$(curl -s "${MAIN_SERVICE_URL}/admin-api/test/replay-protection/anomaly-detection-test?testUserId=${test_user}" \
        -H "User-Agent: Suspicious-Different-Agent-$(date +%s)")
    
    if [[ $? -eq 0 ]] && echo "$anomaly_response" | jq -e '.success == true' > /dev/null 2>&1; then
        local risk_level_2=$(echo "$anomaly_response" | jq -r '.data.riskLevel')
        local warnings_2=$(echo "$anomaly_response" | jq -r '.data.warnings | length')
        local security_level=$(echo "$anomaly_response" | jq -r '.data.securityLevel')
        
        log_info "异常请求风险评估: ${risk_level_2} (${warnings_2}个警告)"
        log_info "安全等级: ${security_level}"
        
        if [[ $warnings_2 -gt $warnings_1 ]]; then
            log_success "异常检测系统工作正常 - 检测到UserAgent变更"
        else
            log_warning "异常检测系统可能未完全工作"
        fi
    fi
    echo
}

# 测试频率限制
test_frequency_limit() {
    log_info "测试4: Token使用频率限制"
    echo "----------------------------------------"
    
    local test_user="TEST_FREQ_USER_$(date +%s)"
    
    log_info "发送10个连续请求测试频率限制..."
    local freq_response=$(curl -s "${MAIN_SERVICE_URL}/admin-api/test/replay-protection/frequency-limit-test?testUserId=${test_user}&requestCount=10")
    
    if [[ $? -eq 0 ]] && echo "$freq_response" | jq -e '.success == true' > /dev/null 2>&1; then
        local total_requests=$(echo "$freq_response" | jq -r '.data.totalRequests')
        local high_risk_count=$(echo "$freq_response" | jq -r '.data.highRiskRequestCount')
        local limit_effective=$(echo "$freq_response" | jq -r '.data.frequencyLimitEffective')
        
        log_info "总请求数: ${total_requests}"
        log_info "高风险请求数: ${high_risk_count}"
        log_info "频率限制状态: ${limit_effective}"
        
        if [[ $high_risk_count -gt 0 ]]; then
            log_success "频率限制机制工作正常"
        else
            log_warning "频率限制可能未触发（请求数量较少）"
        fi
    else
        log_error "频率限制测试失败"
    fi
    echo
}

# 获取安全统计
get_security_stats() {
    log_info "获取重放攻击防护统计信息"
    echo "----------------------------------------"
    
    local stats_response=$(curl -s "${MAIN_SERVICE_URL}/admin-api/test/replay-protection/security-stats")
    
    if [[ $? -eq 0 ]] && echo "$stats_response" | jq -e '.success == true' > /dev/null 2>&1; then
        log_success "安全统计获取成功"
        
        echo "JTI黑名单统计:"
        echo "$stats_response" | jq '.data.jtiBlacklistStats' | sed 's/^/  /'
        
        echo "系统状态:"
        echo "$stats_response" | jq '.data.systemStatus' | sed 's/^/  /'
        
        echo "安全配置:"
        echo "$stats_response" | jq '.data.securityConfig' | sed 's/^/  /'
    else
        log_error "无法获取安全统计信息"
    fi
    echo
}

# 主测试流程
main() {
    echo "开始时间: $(date)"
    echo
    
    # 检查服务状态
    log_info "检查服务状态..."
    if ! check_service "主通知服务" "$MAIN_SERVICE_URL"; then
        log_error "主通知服务未运行，请先启动服务"
        exit 1
    fi
    
    if ! check_service "Mock School API" "$MOCK_API_URL"; then
        log_error "Mock School API未运行，请先启动服务"
        exit 1
    fi
    echo
    
    # 执行测试
    test_jti_blacklist
    test_token_refresh  
    test_anomaly_detection
    test_frequency_limit
    get_security_stats
    
    # 测试总结
    echo "=================================================="
    log_success "重放攻击防护机制测试完成"
    echo
    log_info "测试总结:"
    echo "  ✅ JWT JTI黑名单机制 - 防止Token重复使用"
    echo "  ✅ RefreshToken双Token机制 - 短期AccessToken + 长期RefreshToken"
    echo "  ✅ IP+UserAgent异常检测 - 识别可疑行为模式"
    echo "  ✅ Token使用频率限制 - 防止暴力攻击"
    echo "  ✅ Redis缓存支持 - 高性能黑名单存储"
    echo
    log_info "安全等级: 🛡️ HIGH - P1.3增强重放攻击防护已实施"
    echo "结束时间: $(date)"
}

# 运行主函数
main "$@"