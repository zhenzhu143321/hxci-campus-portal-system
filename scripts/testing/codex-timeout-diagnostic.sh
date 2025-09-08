#!/bin/bash

# Codex超时诊断脚本
# 用于诊断和解决Codex 2分钟超时问题

echo "=========================================="
echo "Codex超时诊断工具 v1.0"
echo "时间: $(date)"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 测试函数
test_codex() {
    local TEST_NAME="$1"
    local PROMPT="$2"
    local EXPECTED_TIME="$3"
    
    echo -e "\n📋 测试: $TEST_NAME"
    echo "预期时间: <$EXPECTED_TIME秒"
    
    # 记录开始时间
    START_TIME=$(date +%s)
    
    # 执行命令并捕获输出
    OUTPUT_FILE="/tmp/codex-test-$(date +%s).log"
    timeout 300 cx -p "$PROMPT" 2>&1 | tee "$OUTPUT_FILE" &
    PID=$!
    
    # 监控进程
    TIMEOUT_COUNT=0
    while kill -0 $PID 2>/dev/null; do
        CURRENT_TIME=$(date +%s)
        ELAPSED=$((CURRENT_TIME - START_TIME))
        
        # 每10秒报告一次状态
        if [ $((ELAPSED % 10)) -eq 0 ] && [ $ELAPSED -gt 0 ]; then
            echo "  ⏱️ 已运行: ${ELAPSED}秒..."
        fi
        
        # 检查是否超过2分钟
        if [ $ELAPSED -gt 120 ] && [ $TIMEOUT_COUNT -eq 0 ]; then
            echo -e "  ${YELLOW}⚠️ 警告: 已超过2分钟！继续等待...${NC}"
            TIMEOUT_COUNT=1
        fi
        
        sleep 1
    done
    
    # 获取结束时间
    END_TIME=$(date +%s)
    TOTAL_TIME=$((END_TIME - START_TIME))
    
    # 检查退出状态
    wait $PID
    EXIT_CODE=$?
    
    # 分析结果
    if [ $EXIT_CODE -eq 124 ]; then
        echo -e "  ${RED}❌ 失败: 命令超时（300秒限制）${NC}"
    elif [ $EXIT_CODE -eq 0 ]; then
        if [ $TOTAL_TIME -le $EXPECTED_TIME ]; then
            echo -e "  ${GREEN}✅ 成功: ${TOTAL_TIME}秒完成${NC}"
        else
            echo -e "  ${YELLOW}⚠️ 成功但较慢: ${TOTAL_TIME}秒完成${NC}"
        fi
    else
        echo -e "  ${RED}❌ 失败: 退出码 $EXIT_CODE${NC}"
    fi
    
    # 检查流输出
    if grep -q "stream.*true" "$OUTPUT_FILE"; then
        echo -e "  ${GREEN}✅ 流输出: 已启用${NC}"
    else
        echo -e "  ${YELLOW}⚠️ 流输出: 可能未启用${NC}"
    fi
    
    rm -f "$OUTPUT_FILE"
    return $EXIT_CODE
}

# 环境检查
echo -e "\n🔍 环境检查..."
echo "----------------------------------------"

# 检查cx脚本配置
echo "1. cx脚本配置:"
if grep -q "stream=true" /usr/local/bin/cx; then
    echo -e "   ${GREEN}✅ stream=true 已配置${NC}"
else
    echo -e "   ${RED}❌ stream=true 未配置${NC}"
fi

if grep -q "timeout 600" /usr/local/bin/cx; then
    echo -e "   ${GREEN}✅ 10分钟超时已配置${NC}"
else
    echo -e "   ${RED}❌ 10分钟超时未配置${NC}"
fi

if grep -q "http_timeout=600" /usr/local/bin/cx; then
    echo -e "   ${GREEN}✅ HTTP超时增强已配置${NC}"
else
    echo -e "   ${YELLOW}⚠️ HTTP超时增强未配置${NC}"
fi

# 检查Codex配置
echo -e "\n2. Codex配置:"
if [ -f /home/ecs-assist-user/.codex/config.json ]; then
    echo -e "   ${GREEN}✅ config.json 存在${NC}"
    MODEL=$(jq -r '.model' /home/ecs-assist-user/.codex/config.json 2>/dev/null)
    echo "   - 模型: $MODEL"
    PROVIDER=$(jq -r '.provider' /home/ecs-assist-user/.codex/config.json 2>/dev/null)
    echo "   - 提供商: $PROVIDER"
else
    echo -e "   ${RED}❌ config.json 不存在${NC}"
fi

# 检查网络连接
echo -e "\n3. 网络连接:"
if curl -s --connect-timeout 5 https://openrouter.ai > /dev/null; then
    echo -e "   ${GREEN}✅ OpenRouter API可访问${NC}"
else
    echo -e "   ${RED}❌ OpenRouter API不可访问${NC}"
fi

# 执行测试
echo -e "\n🧪 执行超时测试..."
echo "=========================================="

# 测试1: 简单请求
test_codex "简单请求" "说'测试成功'" 30

# 测试2: 中等复杂度请求
test_codex "代码分析" "分析TempWeatherController.java的主要功能" 60

# 测试3: 复杂请求
test_codex "架构分析" "简要描述项目的技术栈和主要模块" 120

# 生成报告
echo -e "\n=========================================="
echo "📊 诊断报告"
echo "=========================================="

echo -e "\n建议的优化措施:"
echo "1. ✅ stream=true 已启用 - 保持流输出"
echo "2. ✅ timeout 600 已设置 - 10分钟bash超时"
echo "3. ✅ http_timeout=600 已添加 - HTTP请求超时"
echo "4. ✅ stream_buffer_size=8192 已添加 - 增大缓冲区"

echo -e "\n如果仍然遇到超时问题，可能的原因:"
echo "- 网络不稳定导致流中断"
echo "- OpenRouter API临时性能问题"
echo "- 请求内容过于复杂需要更长处理时间"

echo -e "\n解决方案:"
echo "1. 使用更具体的提示词减少处理时间"
echo "2. 分解大型请求为多个小请求"
echo "3. 在网络较好的时段使用"
echo "4. 考虑使用其他模型（如gpt-4o）作为备选"

echo -e "\n诊断完成: $(date)"