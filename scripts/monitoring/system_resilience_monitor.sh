#!/bin/bash

# 哈尔滨信息工程学院校园门户系统 - 系统弹性持续监控脚本
# 用途: 定期检测系统健康状态和容错能力
# 使用: ./system_resilience_monitor.sh

set -e

# 配置项
MAIN_SERVICE="http://localhost:48081"
MOCK_SERVICE="http://localhost:48082"
LOG_FILE="logs/resilience_monitor_$(date +%Y%m%d).log"
ALERT_THRESHOLD_MS=1000  # 响应时间告警阈值(毫秒)
FAILURE_THRESHOLD=3      # 连续失败次数告警阈值

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 创建日志目录
mkdir -p logs

# 日志记录函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 健康检查函数
health_check() {
    local service_name="$1"
    local service_url="$2"
    local expected_pattern="$3"
    
    local start_time=$(date +%s%3N)
    local response=""
    local http_code=""
    local success=false
    
    # 执行健康检查请求
    if [[ "$service_name" == "认证服务" ]]; then
        # Mock API 认证测试
        response=$(curl -s -w "\n%{http_code}" --max-time 10 -X POST "$service_url/mock-school-api/auth/authenticate" \
            -H "Content-Type: application/json" \
            -d '{"employeeId": "SYSTEM_ADMIN_001", "name": "系统管理员", "password": "admin123"}' 2>/dev/null)
    else
        # 需要JWT Token的服务
        local jwt_token=$(get_jwt_token)
        if [[ -n "$jwt_token" && "$jwt_token" != "null" ]]; then
            response=$(curl -s -w "\n%{http_code}" --max-time 10 -X GET "$service_url" \
                -H "Authorization: Bearer $jwt_token" \
                -H "Content-Type: application/json" \
                -H "tenant-id: 1" 2>/dev/null)
        else
            response="无法获取JWT Token\n500"
        fi
    fi
    
    local end_time=$(date +%s%3N)
    local response_time=$((end_time - start_time))
    
    # 解析响应
    if [[ -n "$response" ]]; then
        http_code=$(echo "$response" | tail -n1)
        response_body=$(echo "$response" | sed '$d')
        
        # 检查响应是否符合预期
        if [[ "$http_code" == "200" ]] && echo "$response_body" | grep -q "$expected_pattern"; then
            success=true
        fi
    else
        http_code="000"
        response_body="连接失败"
    fi
    
    # 输出结果
    local status_icon="❌"
    local status_color="$RED"
    if [[ "$success" == true ]]; then
        status_icon="✅"
        status_color="$GREEN"
        if [[ $response_time -gt $ALERT_THRESHOLD_MS ]]; then
            status_icon="⚠️"
            status_color="$YELLOW"
        fi
    fi
    
    echo -e "${status_color}${status_icon} ${service_name}${NC} - ${response_time}ms (HTTP $http_code)"
    log "$service_name 健康检查: ${success} - ${response_time}ms - HTTP $http_code"
    
    # 返回结果用于汇总
    echo "$success|$response_time|$http_code"
}

# 获取JWT Token
get_jwt_token() {
    curl -s -X POST "$MOCK_SERVICE/mock-school-api/auth/authenticate" \
        -H "Content-Type: application/json" \
        -d '{"employeeId": "SYSTEM_ADMIN_001", "name": "系统管理员", "password": "admin123"}' | \
        jq -r '.data.accessToken' 2>/dev/null || echo ""
}

# 缓存降级测试
test_cache_degradation() {
    log "开始缓存降级测试..."
    
    local jwt_token=$(get_jwt_token)
    if [[ -z "$jwt_token" || "$jwt_token" == "null" ]]; then
        echo -e "${RED}❌ 缓存测试失败: 无法获取JWT Token${NC}"
        return 1
    fi
    
    # 清空缓存
    redis-cli FLUSHALL > /dev/null 2>&1
    
    # 测试缓存失效响应
    local start_time=$(date +%s%3N)
    local response=$(curl -s -X GET "$MAIN_SERVICE/admin-api/test/permission-cache/api/test-class-permission" \
        -H "Authorization: Bearer $jwt_token" \
        -H "Content-Type: application/json" \
        -H "tenant-id: 1" 2>/dev/null)
    local end_time=$(date +%s%3N)
    local cache_miss_time=$((end_time - start_time))
    
    # 测试缓存重建响应
    sleep 1
    start_time=$(date +%s%3N)
    response=$(curl -s -X GET "$MAIN_SERVICE/admin-api/test/permission-cache/api/test-class-permission" \
        -H "Authorization: Bearer $jwt_token" \
        -H "Content-Type: application/json" \
        -H "tenant-id: 1" 2>/dev/null)
    end_time=$(date +%s%3N)
    local cache_hit_time=$((end_time - start_time))
    
    local improvement=$((cache_miss_time - cache_hit_time))
    local improvement_percent=$((improvement * 100 / cache_miss_time))
    
    if [[ $improvement_percent -gt 20 ]]; then
        echo -e "${GREEN}✅ 缓存降级机制正常${NC} - 性能提升 ${improvement_percent}% (${cache_miss_time}ms → ${cache_hit_time}ms)"
        log "缓存降级测试成功: $improvement_percent% 性能提升"
        return 0
    else
        echo -e "${YELLOW}⚠️ 缓存机制可能有问题${NC} - 性能提升仅 ${improvement_percent}%"
        log "缓存降级测试异常: 性能提升仅 $improvement_percent%"
        return 1
    fi
}

# 并发压力简化测试
test_concurrent_load() {
    log "开始并发负载测试..."
    
    local concurrent_count=20
    local success_count=0
    local jwt_token=$(get_jwt_token)
    
    if [[ -z "$jwt_token" || "$jwt_token" == "null" ]]; then
        echo -e "${RED}❌ 并发测试失败: 无法获取JWT Token${NC}"
        return 1
    fi
    
    echo "执行 $concurrent_count 个并发请求..."
    
    # 并发测试
    for i in $(seq 1 $concurrent_count); do
        {
            response=$(curl -s -X GET "$MAIN_SERVICE/admin-api/test/permission-cache/api/ping" \
                -H "Authorization: Bearer $jwt_token" \
                -H "Content-Type: application/json" \
                -H "tenant-id: 1" --max-time 5 2>/dev/null)
            if echo "$response" | jq -e '.code == 0' >/dev/null 2>&1; then
                echo "success" >> "/tmp/concurrent_test_$$"
            fi
        } &
        
        # 控制并发数
        if (( i % 10 == 0 )); then
            wait
        fi
    done
    wait
    
    if [[ -f "/tmp/concurrent_test_$$" ]]; then
        success_count=$(wc -l < "/tmp/concurrent_test_$$" 2>/dev/null || echo "0")
        rm -f "/tmp/concurrent_test_$$"
    fi
    
    local success_rate=$((success_count * 100 / concurrent_count))
    
    if [[ $success_rate -ge 90 ]]; then
        echo -e "${GREEN}✅ 并发处理正常${NC} - 成功率 ${success_rate}% (${success_count}/${concurrent_count})"
        log "并发测试成功: $success_rate% 成功率"
        return 0
    elif [[ $success_rate -ge 70 ]]; then
        echo -e "${YELLOW}⚠️ 并发处理性能下降${NC} - 成功率 ${success_rate}%"
        log "并发测试告警: $success_rate% 成功率"
        return 1
    else
        echo -e "${RED}❌ 并发处理能力不足${NC} - 成功率 ${success_rate}%"
        log "并发测试失败: $success_rate% 成功率"
        return 1
    fi
}

# 主监控函数
main_monitor() {
    echo -e "${BLUE}=== 哈尔滨信息工程学院校园门户系统弹性监控 ===${NC}"
    echo "监控时间: $(date)"
    echo ""
    
    log "开始系统弹性监控检查"
    
    # 核心服务健康检查
    echo -e "${BLUE}🔍 核心服务健康检查${NC}"
    
    # 认证服务检查
    auth_result=$(health_check "认证服务" "$MOCK_SERVICE" '"success":true')
    
    # 主服务检查
    main_result=$(health_check "主通知服务" "$MAIN_SERVICE/admin-api/test/permission-cache/api/ping" '"code":0')
    
    # 通知API检查
    notif_result=$(health_check "通知API" "$MAIN_SERVICE/admin-api/test/notification/api/list" '"code":200')
    
    echo ""
    
    # 系统容错能力检查
    echo -e "${BLUE}🛡️ 系统容错能力检查${NC}"
    
    # 缓存降级测试
    cache_test_success=false
    if test_cache_degradation; then
        cache_test_success=true
    fi
    
    # 并发负载测试
    concurrent_test_success=false
    if test_concurrent_load; then
        concurrent_test_success=true
    fi
    
    echo ""
    
    # 汇总报告
    echo -e "${BLUE}📊 监控结果汇总${NC}"
    
    local total_checks=5
    local passed_checks=0
    
    # 统计通过的检查项
    IFS='|' read -r auth_success auth_time auth_code <<< "$auth_result"
    [[ "$auth_success" == "true" ]] && ((passed_checks++))
    
    IFS='|' read -r main_success main_time main_code <<< "$main_result"
    [[ "$main_success" == "true" ]] && ((passed_checks++))
    
    IFS='|' read -r notif_success notif_time notif_code <<< "$notif_result"
    [[ "$notif_success" == "true" ]] && ((passed_checks++))
    
    [[ "$cache_test_success" == "true" ]] && ((passed_checks++))
    [[ "$concurrent_test_success" == "true" ]] && ((passed_checks++))
    
    local health_percentage=$((passed_checks * 100 / total_checks))
    
    # 系统健康评级
    local health_grade=""
    local health_color=""
    if [[ $health_percentage -ge 90 ]]; then
        health_grade="优秀 (A)"
        health_color="$GREEN"
    elif [[ $health_percentage -ge 80 ]]; then
        health_grade="良好 (B)"
        health_color="$GREEN"
    elif [[ $health_percentage -ge 70 ]]; then
        health_grade="一般 (C)"
        health_color="$YELLOW"
    else
        health_grade="需要关注 (D)"
        health_color="$RED"
    fi
    
    echo -e "${health_color}🎯 系统健康度: ${health_percentage}% - ${health_grade}${NC}"
    echo "通过检查项: ${passed_checks}/${total_checks}"
    
    log "系统健康监控完成: $health_percentage% ($passed_checks/$total_checks)"
    
    # 告警检查
    if [[ $health_percentage -lt 80 ]]; then
        echo -e "${RED}🚨 告警: 系统健康度低于80%，建议立即检查${NC}"
        log "系统健康告警: $health_percentage% 低于80%阈值"
    fi
    
    echo ""
    echo "详细日志: $LOG_FILE"
    echo "下次检查建议: $(date -d '+1 hour' '+%H:%M')"
}

# 执行监控
main_monitor