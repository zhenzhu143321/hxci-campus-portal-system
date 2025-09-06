#!/bin/bash

# 🛡️ HTTP安全头和CSP配置验证脚本
# Phase 1.4 - 自动化安全测试工具
# 
# 功能：
# 1. 验证所有HTTP安全响应头
# 2. 测试CSP策略有效性
# 3. 检查权限策略配置
# 4. 生成安全评估报告
# 
# 作者：Security Team
# 版本：1.0
# 日期：2025-08-25

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置变量
BASE_URL="http://localhost:48081"
MOCK_API_URL="http://localhost:48082"
TEST_TOKEN=""
REPORT_FILE="security_headers_test_report_$(date +%Y%m%d_%H%M%S).md"

# 统计变量
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
WARNINGS=0

echo -e "${BLUE}🛡️ HTTP安全头和CSP配置验证脚本${NC}"
echo -e "${BLUE}================================================${NC}"
echo "开始时间: $(date)"
echo "测试目标: $BASE_URL"
echo "报告文件: $REPORT_FILE"
echo ""

# 初始化报告文件
cat > "$REPORT_FILE" << EOF
# HTTP安全头和CSP配置测试报告

**测试时间**: $(date)  
**测试目标**: $BASE_URL  
**测试脚本**: HTTP安全头验证 v1.0

## 测试概要

EOF

# 辅助函数：记录测试结果
log_test_result() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" == "PASS" ]; then
        echo -e "${GREEN}✅ $test_name: PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "- ✅ **$test_name**: PASS - $details" >> "$REPORT_FILE"
    elif [ "$status" == "FAIL" ]; then
        echo -e "${RED}❌ $test_name: FAIL${NC}"
        echo -e "${RED}   详情: $details${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "- ❌ **$test_name**: FAIL - $details" >> "$REPORT_FILE"
    elif [ "$status" == "WARN" ]; then
        echo -e "${YELLOW}⚠️ $test_name: WARNING${NC}"
        echo -e "${YELLOW}   详情: $details${NC}"
        WARNINGS=$((WARNINGS + 1))
        echo "- ⚠️ **$test_name**: WARNING - $details" >> "$REPORT_FILE"
    fi
}

# 辅助函数：检查HTTP响应头
check_header() {
    local url="$1"
    local header_name="$2"
    local expected_value="$3"
    local test_name="$4"
    
    echo -e "${CYAN}🔍 检查响应头: $header_name${NC}"
    
    # 发送HEAD请求获取响应头
    response=$(curl -s -I -H "Authorization: Bearer $TEST_TOKEN" \
                   -H "tenant-id: 1" \
                   -H "Content-Type: application/json" \
                   "$url" 2>/dev/null || echo "CURL_ERROR")
    
    if [ "$response" == "CURL_ERROR" ]; then
        log_test_result "$test_name" "FAIL" "无法连接到服务器"
        return
    fi
    
    # 提取指定头部
    actual_value=$(echo "$response" | grep -i "^$header_name:" | cut -d' ' -f2- | tr -d '\r\n')
    
    if [ -z "$actual_value" ]; then
        log_test_result "$test_name" "FAIL" "响应头 $header_name 缺失"
        return
    fi
    
    if [ "$expected_value" == "ANY" ]; then
        log_test_result "$test_name" "PASS" "响应头存在: $actual_value"
    elif [[ "$actual_value" =~ $expected_value ]]; then
        log_test_result "$test_name" "PASS" "响应头匹配: $actual_value"
    else
        log_test_result "$test_name" "FAIL" "响应头不匹配 - 期望: $expected_value, 实际: $actual_value"
    fi
}

# 辅助函数：获取测试Token
get_test_token() {
    echo -e "${CYAN}🔑 获取测试Token...${NC}"
    
    # 尝试登录获取Token
    login_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "employeeId": "SYSTEM_ADMIN_001",
            "name": "系统管理员",
            "password": "admin123"
        }' \
        "$MOCK_API_URL/mock-school-api/auth/authenticate" 2>/dev/null || echo "")
    
    if [ -n "$login_response" ]; then
        TEST_TOKEN=$(echo "$login_response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        if [ -n "$TEST_TOKEN" ]; then
            echo -e "${GREEN}✅ Token获取成功${NC}"
        else
            echo -e "${YELLOW}⚠️ Token获取失败，将使用空Token测试${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️ 无法连接认证服务，将使用空Token测试${NC}"
    fi
}

# 测试函数：基础连通性
test_connectivity() {
    echo -e "\n${PURPLE}🔌 测试1: 服务连通性${NC}"
    echo -e "\n## 1. 服务连通性测试\n" >> "$REPORT_FILE"
    
    # 测试主服务
    if curl -s --max-time 5 "$BASE_URL" > /dev/null 2>&1; then
        log_test_result "主服务连通性" "PASS" "服务响应正常"
    else
        log_test_result "主服务连通性" "FAIL" "无法连接到主服务"
    fi
    
    # 测试Mock API
    if curl -s --max-time 5 "$MOCK_API_URL" > /dev/null 2>&1; then
        log_test_result "Mock API连通性" "PASS" "Mock API响应正常"
    else
        log_test_result "Mock API连通性" "WARN" "Mock API连接失败，部分测试可能受影响"
    fi
}

# 测试函数：核心安全响应头
test_security_headers() {
    echo -e "\n${PURPLE}🛡️ 测试2: 核心安全响应头${NC}"
    echo -e "\n## 2. 核心安全响应头测试\n" >> "$REPORT_FILE"
    
    local test_url="$BASE_URL/admin-api/test/notification/api/list"
    
    # X-Frame-Options
    check_header "$test_url" "X-Frame-Options" "DENY" "X-Frame-Options防护"
    
    # X-Content-Type-Options
    check_header "$test_url" "X-Content-Type-Options" "nosniff" "MIME嗅探防护"
    
    # X-XSS-Protection
    check_header "$test_url" "X-XSS-Protection" "1; mode=block" "XSS过滤器"
    
    # Referrer-Policy
    check_header "$test_url" "Referrer-Policy" "strict-origin-when-cross-origin" "引用头策略"
    
    # Cache-Control (API端点)
    check_header "$test_url" "Cache-Control" "no-cache.*no-store.*must-revalidate" "API缓存控制"
}

# 测试函数：Content Security Policy
test_csp_headers() {
    echo -e "\n${PURPLE}🔐 测试3: 内容安全策略(CSP)${NC}"
    echo -e "\n## 3. 内容安全策略测试\n" >> "$REPORT_FILE"
    
    local test_url="$BASE_URL/admin-api/test/notification/api/list"
    
    # CSP主策略
    check_header "$test_url" "Content-Security-Policy" "default-src" "CSP基础策略"
    
    # script-src策略
    check_header "$test_url" "Content-Security-Policy" "script-src" "脚本源策略"
    
    # style-src策略
    check_header "$test_url" "Content-Security-Policy" "style-src" "样式源策略"
    
    # connect-src策略
    check_header "$test_url" "Content-Security-Policy" "connect-src" "连接源策略"
    
    # frame-ancestors策略
    check_header "$test_url" "Content-Security-Policy" "frame-ancestors.*none" "框架祖先策略"
    
    # report-uri策略
    check_header "$test_url" "Content-Security-Policy" "report-uri" "CSP违规报告"
}

# 测试函数：权限策略
test_permissions_policy() {
    echo -e "\n${PURPLE}🎯 测试4: 权限策略${NC}"
    echo -e "\n## 4. 权限策略测试\n" >> "$REPORT_FILE"
    
    local test_url="$BASE_URL/admin-api/test/notification/api/list"
    
    # Permissions Policy
    check_header "$test_url" "Permissions-Policy" "camera=\\(\\)" "摄像头权限禁用"
    check_header "$test_url" "Permissions-Policy" "microphone=\\(\\)" "麦克风权限禁用"
    check_header "$test_url" "Permissions-Policy" "geolocation=\\(\\)" "位置权限禁用"
}

# 测试函数：HTTPS传输安全
test_transport_security() {
    echo -e "\n${PURPLE}🔐 测试5: HTTPS传输安全${NC}"
    echo -e "\n## 5. HTTPS传输安全测试\n" >> "$REPORT_FILE"
    
    # 注意：在HTTP环境下，HSTS通常不会设置
    local test_url="$BASE_URL/admin-api/test/notification/api/list"
    
    echo -e "${CYAN}🔍 检查HSTS配置...${NC}"
    response=$(curl -s -I -H "Authorization: Bearer $TEST_TOKEN" \
                   -H "tenant-id: 1" \
                   "$test_url" 2>/dev/null || echo "CURL_ERROR")
    
    hsts_header=$(echo "$response" | grep -i "^Strict-Transport-Security:" || echo "")
    
    if [ -n "$hsts_header" ]; then
        log_test_result "HSTS配置" "PASS" "HSTS头部存在: $hsts_header"
    else
        log_test_result "HSTS配置" "WARN" "HSTS未配置（HTTP环境下正常）"
    fi
}

# 测试函数：CSP违规报告端点
test_csp_reporting() {
    echo -e "\n${PURPLE}🚨 测试6: CSP违规报告机制${NC}"
    echo -e "\n## 6. CSP违规报告机制测试\n" >> "$REPORT_FILE"
    
    # 测试CSP报告端点
    local report_url="$BASE_URL/csp-report"
    
    echo -e "${CYAN}🔍 测试CSP报告端点...${NC}"
    
    # 发送测试违规报告
    test_report='{
        "csp-report": {
            "document-uri": "http://localhost:48081/test",
            "violated-directive": "script-src",
            "blocked-uri": "http://evil.example.com/malicious.js",
            "original-policy": "default-src '\''self'\''"
        }
    }'
    
    report_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$test_report" \
        "$report_url" 2>/dev/null || echo "CURL_ERROR")
    
    if [ "$report_response" == "CURL_ERROR" ]; then
        log_test_result "CSP报告端点" "FAIL" "无法连接到CSP报告端点"
    elif echo "$report_response" | grep -q '"received":true'; then
        log_test_result "CSP报告端点" "PASS" "CSP报告端点工作正常"
    else
        log_test_result "CSP报告端点" "FAIL" "CSP报告端点响应异常"
    fi
    
    # 测试安全状态监控端点
    status_response=$(curl -s "$report_url/security-status" 2>/dev/null || echo "CURL_ERROR")
    
    if echo "$status_response" | grep -q '"systemHealth":"HEALTHY"'; then
        log_test_result "安全状态监控" "PASS" "安全状态监控正常"
    else
        log_test_result "安全状态监控" "WARN" "安全状态监控可能异常"
    fi
}

# 测试函数：跨域资源共享(CORS)
test_cors_configuration() {
    echo -e "\n${PURPLE}🌐 测试7: CORS配置验证${NC}"
    echo -e "\n## 7. CORS配置验证\n" >> "$REPORT_FILE"
    
    local test_url="$BASE_URL/admin-api/test/notification/api/list"
    
    echo -e "${CYAN}🔍 测试CORS预检请求...${NC}"
    
    # OPTIONS预检请求
    cors_response=$(curl -s -X OPTIONS \
        -H "Origin: http://localhost:3000" \
        -H "Access-Control-Request-Method: POST" \
        -H "Access-Control-Request-Headers: Authorization,Content-Type" \
        "$test_url" 2>/dev/null || echo "CURL_ERROR")
    
    if [ "$cors_response" != "CURL_ERROR" ]; then
        # 检查CORS响应头
        access_control_allow_origin=$(echo "$cors_response" | grep -i "Access-Control-Allow-Origin" || echo "")
        access_control_allow_methods=$(echo "$cors_response" | grep -i "Access-Control-Allow-Methods" || echo "")
        
        if [ -n "$access_control_allow_origin" ]; then
            log_test_result "CORS Origin允许" "PASS" "CORS Origin配置正常"
        else
            log_test_result "CORS Origin允许" "WARN" "CORS Origin配置可能缺失"
        fi
        
        if [ -n "$access_control_allow_methods" ]; then
            log_test_result "CORS方法允许" "PASS" "CORS方法配置正常"
        else
            log_test_result "CORS方法允许" "WARN" "CORS方法配置可能缺失"
        fi
    else
        log_test_result "CORS预检请求" "FAIL" "CORS预检请求失败"
    fi
}

# 测试函数：安全评分计算
calculate_security_score() {
    echo -e "\n${PURPLE}🏆 计算安全评分${NC}"
    echo -e "\n## 安全评分\n" >> "$REPORT_FILE"
    
    local score=0
    local max_score=100
    
    # 基于通过的测试计算分数
    if [ $TOTAL_TESTS -gt 0 ]; then
        score=$(( (PASSED_TESTS * 100) / TOTAL_TESTS ))
    fi
    
    # 安全等级评定
    local security_level=""
    if [ $score -ge 90 ]; then
        security_level="优秀 (A)"
        echo -e "${GREEN}🏆 安全评分: $score/100 - $security_level${NC}"
    elif [ $score -ge 80 ]; then
        security_level="良好 (B)"
        echo -e "${GREEN}🥈 安全评分: $score/100 - $security_level${NC}"
    elif [ $score -ge 70 ]; then
        security_level="一般 (C)"
        echo -e "${YELLOW}🥉 安全评分: $score/100 - $security_level${NC}"
    else
        security_level="需要改进 (D)"
        echo -e "${RED}⚠️ 安全评分: $score/100 - $security_level${NC}"
    fi
    
    echo "**总体安全评分**: $score/100 - $security_level" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
}

# 生成最终报告
generate_final_report() {
    echo -e "\n${BLUE}📊 生成最终报告${NC}"
    
    cat >> "$REPORT_FILE" << EOF

## 测试统计

- **总测试数**: $TOTAL_TESTS
- **通过测试**: $PASSED_TESTS
- **失败测试**: $FAILED_TESTS  
- **警告数量**: $WARNINGS
- **成功率**: $(( (PASSED_TESTS * 100) / TOTAL_TESTS ))%

## 安全建议

### 高优先级修复项
EOF

    if [ $FAILED_TESTS -gt 0 ]; then
        echo "- 请优先处理标记为 ❌ FAIL 的安全配置问题" >> "$REPORT_FILE"
        echo "- 建议立即修复核心安全响应头缺失问题" >> "$REPORT_FILE"
    fi

    if [ $WARNINGS -gt 0 ]; then
        echo "" >> "$REPORT_FILE"
        echo "### 中优先级改进项" >> "$REPORT_FILE"
        echo "- 请关注标记为 ⚠️ WARNING 的配置项" >> "$REPORT_FILE"
        echo "- 考虑在生产环境中启用更严格的安全策略" >> "$REPORT_FILE"
    fi

    cat >> "$REPORT_FILE" << EOF

### 一般建议
- 定期检查和更新CSP策略，适配新的业务需求
- 监控CSP违规报告，及时发现潜在的安全威胁
- 在生产环境中启用HTTPS和HSTS配置
- 考虑实施更严格的权限策略

## 测试环境信息

- **测试时间**: $(date)
- **测试工具**: HTTP安全头验证脚本 v1.0
- **目标服务**: $BASE_URL
- **Token认证**: $([ -n "$TEST_TOKEN" ] && echo "已启用" || echo "未启用")

---
*报告由自动化安全测试脚本生成*
EOF
}

# 主函数
main() {
    # 获取测试Token
    get_test_token
    
    # 执行所有测试
    test_connectivity
    test_security_headers
    test_csp_headers
    test_permissions_policy
    test_transport_security
    test_csp_reporting
    test_cors_configuration
    
    # 计算安全评分
    calculate_security_score
    
    # 生成最终报告
    generate_final_report
    
    # 显示总结
    echo -e "\n${BLUE}================================================${NC}"
    echo -e "${BLUE}🛡️ HTTP安全头测试完成${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo -e "总测试数: ${TOTAL_TESTS}"
    echo -e "${GREEN}通过: ${PASSED_TESTS}${NC}"
    echo -e "${RED}失败: ${FAILED_TESTS}${NC}"
    echo -e "${YELLOW}警告: ${WARNINGS}${NC}"
    echo -e "详细报告: ${REPORT_FILE}"
    echo -e "完成时间: $(date)"
    
    # 返回适当的退出码
    if [ $FAILED_TESTS -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

# 脚本帮助信息
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help     显示此帮助信息"
    echo "  -u, --url URL  指定测试目标URL (默认: http://localhost:48081)"
    echo "  -v, --verbose  详细输出模式"
    echo ""
    echo "示例:"
    echo "  $0                           # 使用默认配置测试"
    echo "  $0 -u http://example.com     # 测试指定URL"
    echo "  $0 -v                        # 详细输出模式"
}

# 处理命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--url)
            BASE_URL="$2"
            shift 2
            ;;
        -v|--verbose)
            set -x
            shift
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 执行主函数
main