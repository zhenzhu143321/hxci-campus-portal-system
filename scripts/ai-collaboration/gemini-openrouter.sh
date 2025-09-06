#!/bin/bash

# Gemini via OpenRouter API调用脚本 - 无速率限制版本
# 使用OpenRouter付费API替代Google免费API
# 支持google/gemini-2.5-pro和google/gemini-2.5-flash模型

# OpenRouter API配置
OPENROUTER_API_KEY="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"
OPENROUTER_BASE_URL="https://openrouter.ai/api/v1"

# 模型选择
DEFAULT_MODEL="google/gemini-2.5-pro"  # 或 google/gemini-2.5-flash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 函数：调用Gemini via OpenRouter
call_gemini() {
    local prompt="$1"
    local model="${2:-$DEFAULT_MODEL}"
    local max_tokens="${3:-4000}"
    local temperature="${4:-0.7}"
    
    echo -e "${CYAN}🚀 调用Gemini via OpenRouter (无速率限制)...${NC}"
    echo -e "${BLUE}模型: $model${NC}"
    
    # 构造请求JSON
    local request_json=$(cat <<EOF
{
    "model": "$model",
    "messages": [
        {
            "role": "user",
            "content": "$prompt"
        }
    ],
    "max_tokens": $max_tokens,
    "temperature": $temperature,
    "stream": false
}
EOF
)
    
    # 发送API请求
    local response=$(curl -s -X POST "$OPENROUTER_BASE_URL/chat/completions" \
        -H "Authorization: Bearer $OPENROUTER_API_KEY" \
        -H "Content-Type: application/json" \
        -H "HTTP-Referer: https://github.com/hxcisunli/hxci-campus-portal" \
        -H "X-Title: HXCI Campus Portal Analysis" \
        -d "$request_json")
    
    # 检查错误
    if [[ $(echo "$response" | jq -r '.error' 2>/dev/null) != "null" ]]; then
        echo -e "${RED}❌ API调用失败:${NC}"
        echo "$response" | jq -r '.error.message' 2>/dev/null
        return 1
    fi
    
    # 提取并显示响应
    local content=$(echo "$response" | jq -r '.choices[0].message.content' 2>/dev/null)
    
    if [[ -n "$content" && "$content" != "null" ]]; then
        echo -e "${GREEN}✅ Gemini响应:${NC}"
        echo "$content"
        
        # 显示使用统计
        local usage=$(echo "$response" | jq -r '.usage' 2>/dev/null)
        if [[ "$usage" != "null" ]]; then
            echo -e "\n${CYAN}📊 Token使用统计:${NC}"
            echo "$usage" | jq '.'
        fi
    else
        echo -e "${RED}❌ 无法解析响应${NC}"
        echo "$response"
        return 1
    fi
}

# 函数：分析项目代码
analyze_project() {
    local analysis_type="$1"
    local project_path="${2:-/opt/hxci-campus-portal/hxci-campus-portal-system}"
    
    echo -e "${PURPLE}🔍 分析类型: $analysis_type${NC}"
    echo -e "${PURPLE}📂 项目路径: $project_path${NC}"
    
    local prompt=""
    
    case "$analysis_type" in
        "token-adapter")
            prompt="分析哈尔滨信息工程学院校园门户系统的三重Token适配器开发状态：
1. 当前登录流程实现状况
2. Basic+JWT+CSRF三重认证架构完成度
3. P0-CRITICAL-5任务（真实学校API适配器实现）进展
4. Mock/Real双模式切换机制
5. RestTemplate HTTP客户端开发状态
6. Token格式兼容性解决方案
7. 技术债务和阻塞项评估
8. 下一步实施建议

项目背景：Spring Boot 3.4.5 + Vue 3 + 三重Token认证系统
当前任务：P0-CRITICAL-5 真实学校API适配器核心实现
学校API：https://work.greathiit.com/api/user/loginWai
测试账号：学生(2023010105/888888)，教师(10031/888888)"
            ;;
            
        "architecture")
            prompt="深度分析项目架构：
1. 前后端分离架构评估
2. 模块化单体设计合理性
3. P0级权限缓存系统性能
4. 三重Token认证安全性
5. 技术债务评估
6. 架构改进建议"
            ;;
            
        "security")
            prompt="安全风险分析：
1. JWT Token安全性评估
2. CSRF防护机制完整性
3. 权限矩阵验证准确性
4. SQL注入防护措施
5. XSS攻击防护状态
6. 安全改进建议"
            ;;
            
        *)
            prompt="$analysis_type"
            ;;
    esac
    
    call_gemini "$prompt" "$DEFAULT_MODEL" 8000 0.7
}

# 函数：显示帮助
show_help() {
    echo -e "${BLUE}================ Gemini via OpenRouter (无速率限制) ================${NC}"
    echo ""
    echo -e "${YELLOW}使用方法:${NC}"
    echo "  $0 call \"提问内容\"                    - 直接调用Gemini"
    echo "  $0 analyze token-adapter             - 分析三重Token适配器"
    echo "  $0 analyze architecture              - 分析项目架构"  
    echo "  $0 analyze security                  - 安全风险分析"
    echo "  $0 help                              - 显示帮助"
    echo ""
    echo -e "${GREEN}✨ 优势:${NC}"
    echo "  • 使用OpenRouter付费API，无速率限制"
    echo "  • 支持超长上下文（2M tokens）"
    echo "  • 稳定可靠，适合生产环境"
    echo "  • 支持流式输出和批量处理"
    echo ""
    echo -e "${CYAN}📋 支持的模型:${NC}"
    echo "  • google/gemini-2.5-pro (推荐)"
    echo "  • google/gemini-2.5-flash (快速版)"
}

# 主程序逻辑
main() {
    case "${1:-help}" in
        "call")
            shift
            call_gemini "$@"
            ;;
        "analyze")
            shift
            analyze_project "$@"
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 运行主程序
main "$@"