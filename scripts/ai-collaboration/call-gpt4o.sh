#!/bin/bash

# GPT-4o 标准化调用脚本 (备用编码助手)
# 使用说明: ./call-gpt4o.sh "你的问题内容"
# 返回: 标准格式的回答内容

# 加载配置
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
source "$SCRIPT_DIR/ai-config.env"

# 检查参数
if [ -z "$1" ]; then
    echo "❌ 错误: 缺少问题内容"
    echo "使用方法: $0 '你的问题内容'"
    exit 1
fi

QUESTION="$1"
MAX_TOKENS="${2:-$DEFAULT_MAX_TOKENS}"
TEMPERATURE="${3:-$DEFAULT_TEMPERATURE}"

# 构建API请求
REQUEST_BODY=$(cat <<EOF
{
  "model": "$GPT4O_MODEL",
  "messages": [
    {
      "role": "user",
      "content": "$QUESTION"
    }
  ],
  "max_tokens": $MAX_TOKENS,
  "temperature": $TEMPERATURE
}
EOF
)

echo "🚀 正在调用GPT-4o (备用编码助手)..."
echo "📝 问题: $QUESTION"
echo "⚙️ 参数: max_tokens=$MAX_TOKENS, temperature=$TEMPERATURE"
echo ""

# 发送请求并保存响应
RESPONSE=$(curl -s --connect-timeout $DEFAULT_TIMEOUT \
  -X POST "$OPENROUTER_BASE_URL/chat/completions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "HTTP-Referer: $OPENROUTER_SITE_URL" \
  -H "X-Title: $OPENROUTER_APP_NAME" \
  -d "$REQUEST_BODY")

# 检查请求是否成功
if [ $? -ne 0 ]; then
    echo "❌ 网络请求失败"
    exit 1
fi

# 检查是否有错误
ERROR_MESSAGE=$(echo "$RESPONSE" | jq -r '.error.message // empty')
if [ ! -z "$ERROR_MESSAGE" ]; then
    echo "❌ API错误: $ERROR_MESSAGE"
    exit 1
fi

# 解析GPT-4o标准响应
echo "📊 GPT-4o 响应解析:"
echo "====================="

# 提取基本信息
MODEL=$(echo "$RESPONSE" | jq -r '.model')
USAGE=$(echo "$RESPONSE" | jq -r '.usage')
echo "🤖 模型: $MODEL"
echo "📈 Token使用: $(echo "$USAGE" | jq -c .)"

# 提取主要内容
CONTENT=$(echo "$RESPONSE" | jq -r '.choices[0].message.content')

echo ""
echo "💬 回答内容:"
echo "------------------------"
echo "$CONTENT"

echo ""
echo "📋 完整响应 (JSON格式):"
echo "=========================="
echo "$RESPONSE" | jq .

# 保存响应到日志文件
LOG_FILE="$SCRIPT_DIR/logs/gpt4o-$(date +%Y%m%d-%H%M%S).json"
mkdir -p "$SCRIPT_DIR/logs"
echo "$RESPONSE" | jq . > "$LOG_FILE"
echo ""
echo "💾 完整响应已保存: $LOG_FILE"