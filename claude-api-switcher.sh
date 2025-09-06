#!/bin/bash

# --- Claude Code API Switcher ---
# 支持多个API提供商的快速切换脚本
# 使用方法: source claude-api-switcher.sh 然后使用 cc 命令

cc() {
  local profile="$1"
  case "$profile" in
    deepseek)
      export ANTHROPIC_BASE_URL="https://api.deepseek.com/anthropic"
      export ANTHROPIC_AUTH_TOKEN="sk-40978824582840b18119904586c09f64"
      export ANTHROPIC_MODEL="deepseek-chat"
      export ANTHROPIC_SMALL_FAST_MODEL="deepseek-chat"
      ;;
    claude)
      # 官方Claude API配置
      export ANTHROPIC_BASE_URL="https://api.anthropic.com"
      export ANTHROPIC_AUTH_TOKEN="你的Claude官方API密钥"
      export ANTHROPIC_MODEL="claude-3-5-sonnet-20241022"
      export ANTHROPIC_SMALL_FAST_MODEL="claude-3-haiku-20240307"
      ;;
    anyrouter)
      export ANTHROPIC_BASE_URL="你调用的网址"
      export ANTHROPIC_AUTH_TOKEN="你的anyrouter_api"
      export ANTHROPIC_MODEL="claude-3-5-sonnet-20241022"
      export ANTHROPIC_SMALL_FAST_MODEL="claude-3-haiku-20240307"
      ;;
    glm)
      export ANTHROPIC_BASE_URL="你调用的智谱网址"
      export ANTHROPIC_AUTH_TOKEN="你的glm_api"
      export ANTHROPIC_MODEL="glm-4"
      export ANTHROPIC_SMALL_FAST_MODEL="glm-4-flash"
      ;;
    show)
      echo "当前API配置:"
      echo "ANTHROPIC_BASE_URL=$ANTHROPIC_BASE_URL"
      echo "ANTHROPIC_MODEL=$ANTHROPIC_MODEL"
      echo "ANTHROPIC_SMALL_FAST_MODEL=$ANTHROPIC_SMALL_FAST_MODEL"
      if [[ -n "$ANTHROPIC_AUTH_TOKEN" ]]; then
        echo "ANTHROPIC_AUTH_TOKEN=${ANTHROPIC_AUTH_TOKEN:0:6}…${ANTHROPIC_AUTH_TOKEN: -4}"
      else
        echo "ANTHROPIC_AUTH_TOKEN=(empty)"
      fi
      return 0
      ;;
    off|clear)
      unset ANTHROPIC_BASE_URL
      unset ANTHROPIC_AUTH_TOKEN
      unset ANTHROPIC_MODEL
      unset ANTHROPIC_SMALL_FAST_MODEL
      echo "已清除当前终端的ANTHROPIC_*环境变量"
      return 0
      ;;
    *)
      echo "使用方法: cc {deepseek|claude|anyrouter|glm|show|off}"
      echo ""
      echo "可用配置:"
      echo "  deepseek   - DeepSeek API (已配置)"
      echo "  claude     - Claude官方API"
      echo "  anyrouter  - AnyRouter代理"
      echo "  glm        - 智谱GLM API"
      echo "  show       - 显示当前配置"
      echo "  off/clear  - 清除环境变量"
      return 1
      ;;
  esac
  echo "已切换到 \"$profile\" → $ANTHROPIC_BASE_URL"
}

# 快捷启动函数 (可选使用)
claude-deepseek() { 
  cc deepseek && command -v claude >/dev/null && claude "$@"
}

claude-official() { 
  cc claude && command -v claude >/dev/null && claude "$@"
}

claude-anyrouter() { 
  cc anyrouter && command -v claude >/dev/null && claude "$@"
}

claude-glm() { 
  cc glm && command -v claude >/dev/null && claude "$@"
}

echo "Claude Code API切换脚本已加载"
echo "使用 'cc show' 查看当前配置"
echo "使用 'cc deepseek' 切换到DeepSeek API"

# --- End ---