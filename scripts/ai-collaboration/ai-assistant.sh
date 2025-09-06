#!/bin/bash

# AI协作助手总控脚本
# 智能选择最合适的AI模型进行调用
# 使用说明: ./ai-assistant.sh <模型类型> "问题内容" [max_tokens] [temperature]

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# 显示使用帮助
show_help() {
    echo "🤖 AI协作助手 - 统一调用接口"
    echo "================================"
    echo ""
    echo "使用方法:"
    echo "  $0 <模型类型> \"问题内容\" [max_tokens] [temperature]"
    echo ""
    echo "模型类型:"
    echo "  gpt5        - OpenAI GPT-5 (主要编码搭子, 推理模式)"
    echo "  gemini-pro  - Google Gemini 2.5 Pro (架构分析专家, 超长上下文)"
    echo "  gpt4o       - OpenAI GPT-4o (备用编码助手, 标准格式)"
    echo "  auto        - 自动选择最合适的模型"
    echo ""
    echo "参数说明:"
    echo "  max_tokens   - 最大token数 (默认: 2000)"
    echo "  temperature  - 温度值 0-1 (默认: 0.3)"
    echo ""
    echo "使用示例:"
    echo "  $0 gpt5 \"请优化这段Java代码的性能\""
    echo "  $0 gemini-pro \"分析整个项目的架构设计\" 4000 0.2"
    echo "  $0 gpt4o \"解释这个函数的作用\" 500"
    echo "  $0 auto \"我需要技术咨询\""
    echo ""
    echo "🔥 AI编码协作铁律:"
    echo "  1. 技术咨询、代码优化 → 使用 gpt5"
    echo "  2. 整体架构分析、深度洞察 → 使用 gemini-pro"  
    echo "  3. 简单问答、快速响应 → 使用 gpt4o"
    echo ""
}

# 检查参数
if [ $# -lt 2 ]; then
    show_help
    exit 1
fi

MODEL_TYPE="$1"
QUESTION="$2"
MAX_TOKENS="${3:-2000}"
TEMPERATURE="${4:-0.3}"

# 自动选择模型逻辑
auto_select_model() {
    local question_lower=$(echo "$1" | tr '[:upper:]' '[:lower:]')
    
    # 架构分析相关关键词
    if echo "$question_lower" | grep -E "(架构|设计|整体|分析|重构|规划|评估)" >/dev/null; then
        echo "gemini-pro"
        return
    fi
    
    # 代码相关关键词
    if echo "$question_lower" | grep -E "(代码|编程|java|vue|javascript|性能|优化|函数|方法|bug|错误)" >/dev/null; then
        echo "gpt5"
        return
    fi
    
    # 默认使用GPT-4o处理一般问题
    echo "gpt4o"
}

# 处理auto模式
if [ "$MODEL_TYPE" = "auto" ]; then
    SELECTED_MODEL=$(auto_select_model "$QUESTION")
    echo "🧠 自动选择模型: $SELECTED_MODEL"
    echo "📝 选择原因: 基于问题内容的关键词分析"
    echo ""
    MODEL_TYPE="$SELECTED_MODEL"
fi

# 调用对应的模型脚本
case "$MODEL_TYPE" in
    "gpt5")
        echo "🚀 调用主要编码搭子: GPT-5"
        "$SCRIPT_DIR/call-gpt5.sh" "$QUESTION" "$MAX_TOKENS" "$TEMPERATURE"
        ;;
    "gemini-pro")
        echo "🏗️ 调用架构分析专家: Gemini 2.5 Pro"
        "$SCRIPT_DIR/call-gemini-pro.sh" "$QUESTION" "$MAX_TOKENS" "$TEMPERATURE"
        ;;
    "gpt4o")
        echo "⚡ 调用备用编码助手: GPT-4o"
        "$SCRIPT_DIR/call-gpt4o.sh" "$QUESTION" "$MAX_TOKENS" "$TEMPERATURE"
        ;;
    *)
        echo "❌ 错误: 不支持的模型类型 '$MODEL_TYPE'"
        echo ""
        show_help
        exit 1
        ;;
esac

echo ""
echo "✅ AI协作调用完成"
echo "📁 日志文件保存在: $SCRIPT_DIR/logs/"
echo ""
echo "💡 提示: 使用 '$0 --help' 查看详细使用说明"