#!/bin/bash

# 🚀 HTTP安全头快速验证脚本
# 用于验证Phase 1.4配置是否生效

echo "🛡️ HTTP安全头快速验证"
echo "=========================="

# 检查服务状态
echo "1. 检查服务状态..."
if curl -s --max-time 5 http://localhost:48081 > /dev/null; then
    echo "✅ 主服务 (48081) 正常"
else
    echo "❌ 主服务 (48081) 无响应 - 请先启动服务"
    exit 1
fi

if curl -s --max-time 5 http://localhost:48082 > /dev/null; then
    echo "✅ Mock API (48082) 正常"
else
    echo "⚠️ Mock API (48082) 无响应"
fi

echo ""
echo "2. 测试核心安全响应头..."

# 测试API端点
TEST_URL="http://localhost:48081/admin-api/test/notification/api/list"

echo "测试URL: $TEST_URL"
echo ""

# 获取响应头
HEADERS=$(curl -s -I "$TEST_URL")

# 检查关键安全头
echo "🔍 安全头检查结果:"

# X-Frame-Options
if echo "$HEADERS" | grep -qi "X-Frame-Options.*DENY"; then
    echo "✅ X-Frame-Options: DENY"
else
    echo "❌ X-Frame-Options: 缺失或配置错误"
fi

# X-Content-Type-Options
if echo "$HEADERS" | grep -qi "X-Content-Type-Options.*nosniff"; then
    echo "✅ X-Content-Type-Options: nosniff"
else
    echo "❌ X-Content-Type-Options: 缺失或配置错误"
fi

# X-XSS-Protection
if echo "$HEADERS" | grep -qi "X-XSS-Protection.*1.*mode=block"; then
    echo "✅ X-XSS-Protection: 1; mode=block"
else
    echo "❌ X-XSS-Protection: 缺失或配置错误"
fi

# Content-Security-Policy
if echo "$HEADERS" | grep -qi "Content-Security-Policy"; then
    echo "✅ Content-Security-Policy: 已配置"
    # 显示CSP策略内容
    CSP_POLICY=$(echo "$HEADERS" | grep -i "Content-Security-Policy" | head -1)
    echo "   策略: ${CSP_POLICY:0:100}..."
else
    echo "❌ Content-Security-Policy: 缺失"
fi

# Referrer-Policy
if echo "$HEADERS" | grep -qi "Referrer-Policy"; then
    echo "✅ Referrer-Policy: 已配置"
else
    echo "❌ Referrer-Policy: 缺失"
fi

# Permissions-Policy
if echo "$HEADERS" | grep -qi "Permissions-Policy"; then
    echo "✅ Permissions-Policy: 已配置"
else
    echo "❌ Permissions-Policy: 缺失"
fi

# Cache-Control (API端点)
if echo "$HEADERS" | grep -qi "Cache-Control.*no-cache"; then
    echo "✅ Cache-Control: API缓存已禁用"
else
    echo "⚠️ Cache-Control: 可能未正确配置"
fi

echo ""
echo "3. 测试CSP违规报告端点..."

# 测试CSP报告端点
CSP_REPORT_URL="http://localhost:48081/csp-report/security-status"
if curl -s "$CSP_REPORT_URL" | grep -q "systemHealth"; then
    echo "✅ CSP报告端点工作正常"
else
    echo "❌ CSP报告端点可能有问题"
fi

echo ""
echo "4. 生成简要报告..."

# 计算配置完成度
TOTAL_CHECKS=7
PASSED_CHECKS=0

echo "$HEADERS" | grep -qi "X-Frame-Options.*DENY" && PASSED_CHECKS=$((PASSED_CHECKS + 1))
echo "$HEADERS" | grep -qi "X-Content-Type-Options.*nosniff" && PASSED_CHECKS=$((PASSED_CHECKS + 1))
echo "$HEADERS" | grep -qi "X-XSS-Protection.*1.*mode=block" && PASSED_CHECKS=$((PASSED_CHECKS + 1))
echo "$HEADERS" | grep -qi "Content-Security-Policy" && PASSED_CHECKS=$((PASSED_CHECKS + 1))
echo "$HEADERS" | grep -qi "Referrer-Policy" && PASSED_CHECKS=$((PASSED_CHECKS + 1))
echo "$HEADERS" | grep -qi "Permissions-Policy" && PASSED_CHECKS=$((PASSED_CHECKS + 1))
echo "$HEADERS" | grep -qi "Cache-Control.*no-cache" && PASSED_CHECKS=$((PASSED_CHECKS + 1))

COMPLETION_RATE=$((PASSED_CHECKS * 100 / TOTAL_CHECKS))

echo "📊 安全配置完成度: $PASSED_CHECKS/$TOTAL_CHECKS ($COMPLETION_RATE%)"

if [ $COMPLETION_RATE -ge 80 ]; then
    echo "🎉 安全配置状态: 优秀"
elif [ $COMPLETION_RATE -ge 60 ]; then
    echo "👍 安全配置状态: 良好"
else
    echo "⚠️ 安全配置状态: 需要改进"
fi

echo ""
echo "如需详细测试，请运行: ./security_headers_test.sh"
echo "=========================="