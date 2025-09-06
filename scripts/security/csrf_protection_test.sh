#!/bin/bash

# 🛡️ CSRF跨站请求伪造防护测试脚本
# 
# 🚨 中风险安全漏洞修复验证：CVE-HXCI-2025-010
# 验证CSRF防护机制是否正确阻止了跨站请求伪造攻击
#
# 测试场景：
# 1. 正常请求（带CSRF Token）应该成功
# 2. 恶意请求（无CSRF Token）应该被阻止
# 3. 伪造请求（错误CSRF Token）应该被阻止
# 4. GET请求（读操作）不受影响
#
# @author Security Team
# @version 1.0
# @since 2025-08-24

set -e

# 🎨 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 📊 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 🎯 测试配置
BASE_URL="http://localhost:48081"
MOCK_API_URL="http://localhost:48082"

# 🔑 测试账号信息
USERNAME="PRINCIPAL_001"
REALNAME="Principal-Zhang"
PASSWORD="admin123"

JWT_TOKEN=""
CSRF_TOKEN=""
CSRF_HEADER="X-CSRF-TOKEN"

# 📋 日志函数
log_info() {
    echo -e "${CYAN}ℹ️  [INFO] $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ [SUCCESS] $1${NC}"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}❌ [ERROR] $1${NC}"
    ((FAILED_TESTS++))
}

log_warning() {
    echo -e "${YELLOW}⚠️  [WARNING] $1${NC}"
}

log_test() {
    echo -e "${PURPLE}🧪 [TEST] $1${NC}"
    ((TOTAL_TESTS++))
}

# 🔐 获取JWT Token
get_jwt_token() {
    log_info "正在获取JWT Token..."
    
    local response=$(curl -s -X POST "${MOCK_API_URL}/mock-school-api/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d "{
            \"employeeId\": \"${USERNAME}\",
            \"name\": \"${REALNAME}\", 
            \"password\": \"${PASSWORD}\"
        }")
    
    JWT_TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [[ -n "$JWT_TOKEN" ]]; then
        log_success "JWT Token获取成功 (前30字符): ${JWT_TOKEN:0:30}..."
    else
        log_error "JWT Token获取失败"
        exit 1
    fi
}

# 🛡️ 获取CSRF Token
get_csrf_token() {
    log_info "正在获取CSRF Token..."
    
    # 使用curl获取CSRF Token，保存Cookie
    local response=$(curl -s -c /tmp/csrf_cookies.txt "${BASE_URL}/csrf-token")
    
    CSRF_TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [[ -n "$CSRF_TOKEN" ]]; then
        log_success "CSRF Token获取成功 (前30字符): ${CSRF_TOKEN:0:30}..."
    else
        log_error "CSRF Token获取失败"
        log_warning "响应内容: $response"
        exit 1
    fi
}

# 📊 测试函数
run_test() {
    local test_name="$1"
    local method="$2"
    local url="$3"
    local headers="$4"
    local data="$5"
    local expected_status="$6"
    local expected_pattern="$7"
    
    log_test "$test_name"
    
    # 执行请求
    local response=$(curl -s -w "HTTP_STATUS:%{http_code}" $method "$url" $headers -d "$data")
    local http_body=$(echo "$response" | sed 's/HTTP_STATUS:[0-9]*$//')
    local http_status=$(echo "$response" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d':' -f2)
    
    # 验证HTTP状态码
    if [[ "$http_status" == "$expected_status" ]]; then
        # 验证响应内容
        if [[ -z "$expected_pattern" ]] || echo "$http_body" | grep -q "$expected_pattern"; then
            log_success "$test_name - HTTP状态码: $http_status"
            return 0
        else
            log_error "$test_name - 响应内容不符合预期"
            log_warning "期望包含: $expected_pattern"
            log_warning "实际响应: $http_body"
            return 1
        fi
    else
        log_error "$test_name - HTTP状态码错误: $http_status (期望: $expected_status)"
        log_warning "响应内容: $http_body"
        return 1
    fi
}

# 🧪 CSRF防护测试套件
run_csrf_protection_tests() {
    echo -e "${BLUE}🛡️ ========== CSRF防护测试套件 ==========${NC}"
    
    # 1️⃣ 测试CSRF Token获取端点
    run_test \
        "CSRF Token获取端点测试" \
        "-X GET" \
        "${BASE_URL}/csrf-token" \
        "" \
        "" \
        "200" \
        '"token"'
    
    # 2️⃣ 测试GET请求（应该无需CSRF Token）
    run_test \
        "GET请求无CSRF验证测试" \
        "-X GET" \
        "${BASE_URL}/admin-api/test/notification/api/list" \
        "-H 'Authorization: Bearer $JWT_TOKEN' -H 'tenant-id: 1'" \
        "" \
        "200" \
        ""
    
    # 3️⃣ 测试正常POST请求（带正确CSRF Token）
    run_test \
        "正常POST请求（带CSRF Token）测试" \
        "-X POST" \
        "${BASE_URL}/admin-api/test/notification/api/publish-database" \
        "-H 'Authorization: Bearer $JWT_TOKEN' -H 'tenant-id: 1' -H 'Content-Type: application/json' -H '$CSRF_HEADER: $CSRF_TOKEN' -b /tmp/csrf_cookies.txt" \
        '{
            "title": "🛡️ CSRF防护测试通知",
            "content": "这是CSRF防护功能测试的通知内容",
            "level": 4,
            "targetScope": "SCHOOL_WIDE",
            "pushChannels": [1, 5]
        }' \
        "200" \
        '"code":0'
    
    # 4️⃣ 测试恶意POST请求（无CSRF Token）
    run_test \
        "恶意POST请求（无CSRF Token）测试" \
        "-X POST" \
        "${BASE_URL}/admin-api/test/notification/api/publish-database" \
        "-H 'Authorization: Bearer $JWT_TOKEN' -H 'tenant-id: 1' -H 'Content-Type: application/json'" \
        '{
            "title": "🚨 恶意请求测试",
            "content": "这个请求应该被CSRF防护阻止",
            "level": 4,
            "targetScope": "SCHOOL_WIDE"
        }' \
        "403" \
        "CSRF"
    
    # 5️⃣ 测试伪造CSRF Token请求
    run_test \
        "伪造CSRF Token请求测试" \
        "-X POST" \
        "${BASE_URL}/admin-api/test/notification/api/publish-database" \
        "-H 'Authorization: Bearer $JWT_TOKEN' -H 'tenant-id: 1' -H 'Content-Type: application/json' -H '$CSRF_HEADER: fake-csrf-token-12345'" \
        '{
            "title": "🎭 伪造Token测试",
            "content": "这个请求使用了伪造的CSRF Token",
            "level": 4,
            "targetScope": "SCHOOL_WIDE"
        }' \
        "403" \
        "CSRF"
    
    # 6️⃣ 测试其他写操作端点
    run_test \
        "待办完成API CSRF防护测试" \
        "-X POST" \
        "${BASE_URL}/admin-api/test/todo-new/api/1/complete" \
        "-H 'Authorization: Bearer $JWT_TOKEN' -H 'tenant-id: 1' -H '$CSRF_HEADER: $CSRF_TOKEN' -b /tmp/csrf_cookies.txt" \
        "" \
        "200" \
        ""
    
    # 7️⃣ 测试CSRF状态查询端点
    run_test \
        "CSRF状态查询端点测试" \
        "-X GET" \
        "${BASE_URL}/csrf-status" \
        "" \
        "" \
        "200" \
        '"isValid"'
}

# 📈 生成测试报告
generate_report() {
    echo -e "\n${BLUE}📊 ========== CSRF防护测试报告 ==========${NC}"
    echo -e "${CYAN}📅 测试时间: $(date)${NC}"
    echo -e "${CYAN}🔧 测试环境: ${BASE_URL}${NC}"
    echo -e "${CYAN}📋 总测试数: ${TOTAL_TESTS}${NC}"
    echo -e "${GREEN}✅ 通过测试: ${PASSED_TESTS}${NC}"
    echo -e "${RED}❌ 失败测试: ${FAILED_TESTS}${NC}"
    
    local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo -e "${PURPLE}📈 成功率: ${success_rate}%${NC}"
    
    if [[ $FAILED_TESTS -eq 0 ]]; then
        echo -e "\n${GREEN}🎉 CSRF防护测试全部通过！系统已成功防护跨站请求伪造攻击。${NC}"
        echo -e "${GREEN}🛡️ 中风险安全漏洞 CVE-HXCI-2025-010 修复验证成功！${NC}"
        return 0
    else
        echo -e "\n${RED}⚠️  部分CSRF防护测试失败，请检查配置并修复问题。${NC}"
        return 1
    fi
}

# 🔧 清理临时文件
cleanup() {
    rm -f /tmp/csrf_cookies.txt
    log_info "清理临时文件完成"
}

# 🚀 主函数
main() {
    echo -e "${BLUE}🛡️ ========== CSRF防护测试开始 ==========${NC}"
    echo -e "${CYAN}📋 测试目标: 验证CSRF跨站请求伪造防护机制${NC}"
    echo -e "${CYAN}🎯 漏洞编号: CVE-HXCI-2025-010${NC}"
    echo -e "${CYAN}🔧 测试范围: 写操作API的CSRF Token验证${NC}\n"
    
    # 设置陷阱，确保清理临时文件
    trap cleanup EXIT
    
    # 执行测试步骤
    get_jwt_token
    get_csrf_token
    run_csrf_protection_tests
    
    # 生成测试报告
    generate_report
    
    return $?
}

# 执行主函数
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi