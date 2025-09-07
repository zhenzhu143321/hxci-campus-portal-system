#!/bin/bash

# P0-SEC-03 增强CSRF防护测试脚本
# 测试新增的安全特性：速率限制、严格CORS、安全日志等
# Author: Claude Code AI
# Date: 2025-09-07

echo "========================================="
echo "P0-SEC-03 增强CSRF防护安全测试"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${YELLOW}📋 测试内容：${NC}"
echo "1. CSRF Token获取和验证"
echo "2. 速率限制测试（每分钟10次限制）"
echo "3. CORS严格策略验证"
echo "4. 增强安全响应头检查"
echo "5. 安全日志和监控功能"
echo ""

BASE_URL="http://localhost:48081"
CSRF_TOKEN=""
CSRF_HEADER=""

# 测试1: CSRF Token获取
echo -e "${YELLOW}[测试1] 获取CSRF Token${NC}"
echo "请求: GET ${BASE_URL}/csrf-token"

CSRF_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${BASE_URL}/csrf-token")
HTTP_CODE=$(echo "$CSRF_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
RESPONSE_BODY=$(echo "$CSRF_RESPONSE" | sed '/HTTP_CODE:/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ CSRF Token获取成功${NC}"
    echo "$RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
    
    # 提取Token信息（如果响应是JSON格式）
    if echo "$RESPONSE_BODY" | jq '.' >/dev/null 2>&1; then
        CSRF_TOKEN=$(echo "$RESPONSE_BODY" | jq -r '.data.token' 2>/dev/null)
        CSRF_HEADER=$(echo "$RESPONSE_BODY" | jq -r '.data.headerName' 2>/dev/null)
        echo -e "${BLUE}🔑 提取的Token长度: ${#CSRF_TOKEN}${NC}"
    fi
else
    echo -e "${RED}❌ CSRF Token获取失败 (HTTP $HTTP_CODE)${NC}"
fi

# 测试2: 检查增强的响应头
echo ""
echo -e "${YELLOW}[测试2] 检查增强安全响应头${NC}"
echo "请求: HEAD ${BASE_URL}/csrf-token"

HEADERS_RESPONSE=$(curl -s -I "${BASE_URL}/csrf-token")
echo "$HEADERS_RESPONSE"

# 检查关键安全响应头
echo ""
echo -e "${BLUE}🔍 安全响应头检查：${NC}"
if echo "$HEADERS_RESPONSE" | grep -qi "X-CSRF-Protected"; then
    echo -e "${GREEN}✅ X-CSRF-Protected 头存在${NC}"
else
    echo -e "${YELLOW}⚠️ X-CSRF-Protected 头缺失${NC}"
fi

if echo "$HEADERS_RESPONSE" | grep -qi "X-Security-Policy"; then
    echo -e "${GREEN}✅ X-Security-Policy 头存在${NC}"
else
    echo -e "${YELLOW}⚠️ X-Security-Policy 头缺失${NC}"
fi

if echo "$HEADERS_RESPONSE" | grep -qi "X-Rate-Limit-Remaining"; then
    echo -e "${GREEN}✅ X-Rate-Limit-Remaining 头存在${NC}"
else
    echo -e "${YELLOW}⚠️ X-Rate-Limit-Remaining 头缺失${NC}"
fi

# 测试3: 速率限制测试
echo ""
echo -e "${YELLOW}[测试3] CSRF Token速率限制测试${NC}"
echo "发送15个连续请求测试速率限制（应在第11个请求时被拦截）"

RATE_LIMIT_TRIGGERED=false
for i in {1..15}; do
    RESPONSE=$(curl -s -w "HTTP_CODE:%{http_code}" "${BASE_URL}/csrf-token")
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
    
    if [ "$HTTP_CODE" = "429" ]; then
        echo -e "${GREEN}✅ 第${i}次请求触发速率限制 (HTTP 429)${NC}"
        RATE_LIMIT_TRIGGERED=true
        break
    elif [ "$HTTP_CODE" = "200" ]; then
        echo "第${i}次请求: 成功 (HTTP 200)"
    else
        echo -e "${RED}第${i}次请求: 异常 (HTTP $HTTP_CODE)${NC}"
    fi
    
    # 短暂延迟
    sleep 0.1
done

if [ "$RATE_LIMIT_TRIGGERED" = true ]; then
    echo -e "${GREEN}✅ 速率限制功能正常工作${NC}"
else
    echo -e "${YELLOW}⚠️ 未触发速率限制，可能需要检查配置${NC}"
fi

# 测试4: CORS策略测试
echo ""
echo -e "${YELLOW}[测试4] CORS严格策略测试${NC}"

# 测试允许的来源
echo "测试允许的来源 (localhost:3000):"
CORS_RESPONSE=$(curl -s -I -H "Origin: http://localhost:3000" "${BASE_URL}/csrf-token")
if echo "$CORS_RESPONSE" | grep -qi "Access-Control-Allow-Origin"; then
    echo -e "${GREEN}✅ 允许的来源正确处理${NC}"
else
    echo -e "${RED}❌ 允许的来源处理异常${NC}"
fi

# 测试不允许的来源
echo "测试不允许的来源 (evil.com):"
CORS_EVIL_RESPONSE=$(curl -s -I -H "Origin: http://evil.com" "${BASE_URL}/csrf-token")
if echo "$CORS_EVIL_RESPONSE" | grep -qi "Access-Control-Allow-Origin: http://evil.com"; then
    echo -e "${RED}❌ 危险：不允许的来源被错误地允许${NC}"
else
    echo -e "${GREEN}✅ 不允许的来源正确拒绝${NC}"
fi

# 测试5: CSRF配置信息查询
echo ""
echo -e "${YELLOW}[测试5] 查询CSRF防护配置${NC}"
echo "请求: GET ${BASE_URL}/csrf-config"

CONFIG_RESPONSE=$(curl -s "${BASE_URL}/csrf-config")
echo "$CONFIG_RESPONSE" | jq '.' 2>/dev/null || echo "$CONFIG_RESPONSE"

# 测试6: CSRF状态检查
echo ""
echo -e "${YELLOW}[测试6] CSRF状态检查${NC}"
echo "请求: GET ${BASE_URL}/csrf-status"

STATUS_RESPONSE=$(curl -s "${BASE_URL}/csrf-status")
echo "$STATUS_RESPONSE" | jq '.' 2>/dev/null || echo "$STATUS_RESPONSE"

# 测试7: 带CSRF Token的POST请求测试
if [ -n "$CSRF_TOKEN" ] && [ -n "$CSRF_HEADER" ]; then
    echo ""
    echo -e "${YELLOW}[测试7] 带CSRF Token的POST请求测试${NC}"
    echo "模拟一个需要CSRF验证的POST请求"
    
    POST_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "$CSRF_HEADER: $CSRF_TOKEN" \
        -H "Cookie: XSRF-TOKEN=$CSRF_TOKEN" \
        -d '{"test": "data"}' \
        -w "HTTP_CODE:%{http_code}" \
        "${BASE_URL}/admin-api/test/notification/api/ping")
    
    POST_HTTP_CODE=$(echo "$POST_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
    POST_BODY=$(echo "$POST_RESPONSE" | sed '/HTTP_CODE:/d')
    
    echo "HTTP状态码: $POST_HTTP_CODE"
    echo "响应内容: $POST_BODY"
    
    if [ "$POST_HTTP_CODE" != "403" ]; then
        echo -e "${GREEN}✅ 带CSRF Token的请求正常处理${NC}"
    else
        echo -e "${YELLOW}⚠️ 请求被CSRF防护拦截，可能需要检查Token格式${NC}"
    fi
else
    echo ""
    echo -e "${YELLOW}[测试7] 跳过POST请求测试（未获取到有效Token）${NC}"
fi

# 测试总结
echo ""
echo "========================================="
echo -e "${GREEN}📊 P0-SEC-03 CSRF防护增强测试总结${NC}"
echo "========================================="
echo ""
echo "✅ 已实施的增强功能："
echo "1. 严格CORS策略配置（不再使用通配符*）"
echo "2. CSRF Token速率限制（每分钟最多10次）"
echo "3. 增强安全响应头添加"
echo "4. 详细安全事件日志记录"
echo "5. 可配置的Token过期时间"
echo "6. 安全事件检测和阻断"
echo ""
echo "🔒 安全提升："
echo "- 消除CORS通配符安全风险"
echo "- 防止CSRF Token获取接口被DDoS攻击"
echo "- 增强攻击检测和日志记录能力"
echo "- 提供更详细的安全状态信息"
echo ""
echo "📝 生产环境部署建议："
echo "1. 根据实际前端域名配置 security.csrf.allowed-origins"
echo "2. 调整 security.csrf.token-expiry 符合业务需求"
echo "3. 启用安全监控告警机制"
echo "4. 定期审查安全日志"
echo ""

# 检查服务是否运行
if [ "$HTTP_CODE" = "000" ] || [ -z "$HTTP_CODE" ]; then
    echo -e "${RED}⚠️ 注意: 服务可能未启动，请确认以下服务正在运行：${NC}"
    echo "1. 主通知服务 (端口48081)"
    echo "2. Mock School API (端口48082)"
    echo ""
    echo "启动命令："
    echo "cd /opt/hxci-campus-portal/hxci-campus-portal-system/yudao-boot-mini"
    echo "mvn spring-boot:run -pl yudao-server -Dspring.profiles.active=local"
fi

echo -e "${GREEN}✅ P0-SEC-03 CSRF防护增强测试完成！${NC}"