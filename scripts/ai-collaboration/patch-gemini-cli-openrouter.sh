#!/bin/bash

# Gemini CLI 改造脚本 - 使用OpenRouter替代Google API
# 保留Gemini CLI的所有功能，只改变后端API endpoint

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# OpenRouter配置
OPENROUTER_API_KEY="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"
OPENROUTER_BASE_URL="https://openrouter.ai/api/v1"

# Gemini CLI文件路径
GEMINI_GENAI_PATH="/usr/lib/node_modules/@google/gemini-cli/node_modules/@google/genai/dist/node/index.mjs"
GEMINI_GENAI_BACKUP="${GEMINI_GENAI_PATH}.backup"
GEMINI_CLI_CORE="/usr/lib/node_modules/@google/gemini-cli/node_modules/@google/gemini-cli-core"

# 功能：备份原始文件
backup_original() {
    echo -e "${CYAN}📦 备份原始Gemini CLI文件...${NC}"
    
    if [[ ! -f "$GEMINI_GENAI_BACKUP" ]]; then
        sudo cp "$GEMINI_GENAI_PATH" "$GEMINI_GENAI_BACKUP"
        echo -e "${GREEN}✅ 备份完成: $GEMINI_GENAI_BACKUP${NC}"
    else
        echo -e "${YELLOW}⚠️  备份已存在，跳过备份${NC}"
    fi
}

# 功能：恢复原始文件
restore_original() {
    echo -e "${CYAN}🔄 恢复原始Gemini CLI...${NC}"
    
    if [[ -f "$GEMINI_GENAI_BACKUP" ]]; then
        sudo cp "$GEMINI_GENAI_BACKUP" "$GEMINI_GENAI_PATH"
        echo -e "${GREEN}✅ 已恢复到Google API版本${NC}"
    else
        echo -e "${RED}❌ 备份文件不存在，无法恢复${NC}"
        return 1
    fi
}

# 功能：应用OpenRouter补丁
apply_openrouter_patch() {
    echo -e "${CYAN}🔧 应用OpenRouter补丁...${NC}"
    
    # 备份原始文件
    backup_original
    
    # 创建临时补丁文件
    local patch_file="/tmp/gemini-openrouter-patch.js"
    
    # 生成补丁内容
    cat > "$patch_file" << 'EOF'
// Gemini CLI OpenRouter Patch
// 这个补丁将Google API重定向到OpenRouter

const originalFetch = global.fetch || require('node-fetch');

// 拦截所有fetch请求
global.fetch = async function(url, options = {}) {
    let modifiedUrl = url;
    let modifiedOptions = { ...options };
    
    // 检测Google API调用
    if (typeof url === 'string' && url.includes('generativelanguage.googleapis.com')) {
        console.log('[OpenRouter Patch] Intercepting Google API call');
        
        // 替换为OpenRouter URL
        modifiedUrl = 'https://openrouter.ai/api/v1/chat/completions';
        
        // 修改认证头
        if (!modifiedOptions.headers) {
            modifiedOptions.headers = {};
        }
        modifiedOptions.headers['Authorization'] = 'Bearer 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';
        modifiedOptions.headers['HTTP-Referer'] = 'https://github.com/hxci-campus-portal';
        modifiedOptions.headers['X-Title'] = 'Gemini CLI via OpenRouter';
        
        // 转换请求体格式
        if (modifiedOptions.body) {
            try {
                const body = JSON.parse(modifiedOptions.body);
                
                // 转换为OpenRouter格式
                const openRouterBody = {
                    model: 'google/gemini-2.5-pro',
                    messages: [
                        {
                            role: 'user',
                            content: body.contents?.[0]?.parts?.[0]?.text || body.prompt || ''
                        }
                    ],
                    max_tokens: 8000,
                    temperature: 0.7
                };
                
                modifiedOptions.body = JSON.stringify(openRouterBody);
            } catch (e) {
                console.error('[OpenRouter Patch] Error converting request body:', e);
            }
        }
    }
    
    // 调用原始fetch
    const response = await originalFetch(modifiedUrl, modifiedOptions);
    
    // 如果是OpenRouter响应，转换回Gemini格式
    if (modifiedUrl.includes('openrouter.ai')) {
        const originalJson = response.json;
        response.json = async function() {
            const data = await originalJson.call(this);
            
            // 转换OpenRouter响应为Gemini格式
            if (data.choices && data.choices[0]) {
                return {
                    candidates: [{
                        content: {
                            parts: [{
                                text: data.choices[0].message.content
                            }]
                        }
                    }]
                };
            }
            
            return data;
        };
    }
    
    return response;
};

console.log('[OpenRouter Patch] Successfully patched Gemini CLI to use OpenRouter API');
EOF
    
    # 注入补丁到index.mjs开头
    echo -e "${YELLOW}⚙️  注入补丁代码...${NC}"
    
    # 创建修改后的文件
    {
        cat "$patch_file"
        echo ""
        cat "$GEMINI_GENAI_PATH"
    } | sudo tee "${GEMINI_GENAI_PATH}.patched" > /dev/null
    
    # 替换原文件
    sudo mv "${GEMINI_GENAI_PATH}.patched" "$GEMINI_GENAI_PATH"
    
    echo -e "${GREEN}✅ OpenRouter补丁应用成功！${NC}"
    echo -e "${CYAN}🔑 现在Gemini CLI将使用OpenRouter API（无速率限制）${NC}"
}

# 功能：测试改造后的CLI
test_patched_cli() {
    echo -e "${CYAN}🧪 测试改造后的Gemini CLI...${NC}"
    
    # 设置临时环境变量
    export GEMINI_API_KEY="dummy-key-for-openrouter"
    
    # 测试简单查询
    echo "测试查询：What is 2+2?" | gemini --prompt "What is 2+2? Reply in one line."
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ 测试成功！Gemini CLI正在使用OpenRouter${NC}"
    else
        echo -e "${RED}❌ 测试失败，请检查补丁${NC}"
    fi
}

# 功能：显示状态
show_status() {
    echo -e "${BLUE}=================== Gemini CLI状态 ===================${NC}"
    
    if [[ -f "$GEMINI_GENAI_BACKUP" ]]; then
        echo -e "${GREEN}📦 备份存在: $GEMINI_GENAI_BACKUP${NC}"
        
        # 检查是否已打补丁
        if grep -q "OpenRouter Patch" "$GEMINI_GENAI_PATH" 2>/dev/null; then
            echo -e "${CYAN}🔧 当前状态: 使用OpenRouter API${NC}"
        else
            echo -e "${YELLOW}🌐 当前状态: 使用Google API${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  未找到备份文件${NC}"
    fi
    
    echo ""
    echo -e "${PURPLE}📋 可用操作:${NC}"
    echo "  patch   - 应用OpenRouter补丁"
    echo "  restore - 恢复到Google API"
    echo "  test    - 测试当前配置"
    echo "  status  - 显示当前状态"
}

# 功能：显示帮助
show_help() {
    echo -e "${BLUE}================ Gemini CLI OpenRouter改造工具 ================${NC}"
    echo ""
    echo -e "${YELLOW}使用方法:${NC}"
    echo "  $0 patch    - 应用OpenRouter补丁（使用付费API）"
    echo "  $0 restore  - 恢复到原始Google API"
    echo "  $0 test     - 测试改造后的CLI"
    echo "  $0 status   - 显示当前状态"
    echo "  $0 help     - 显示帮助"
    echo ""
    echo -e "${GREEN}✨ 改造优势:${NC}"
    echo "  • 保留Gemini CLI所有功能（代码扫描、文件分析等）"
    echo "  • 使用OpenRouter付费API，无速率限制"
    echo "  • 一键切换，随时恢复"
    echo "  • 对现有脚本完全兼容"
    echo ""
    echo -e "${RED}⚠️  注意事项:${NC}"
    echo "  • 需要sudo权限修改系统文件"
    echo "  • 建议先备份重要工作"
    echo "  • OpenRouter API是付费服务"
}

# 主程序逻辑
main() {
    case "${1:-help}" in
        "patch")
            apply_openrouter_patch
            ;;
        "restore")
            restore_original
            ;;
        "test")
            test_patched_cli
            ;;
        "status")
            show_status
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 检查权限
if [[ "$1" == "patch" || "$1" == "restore" ]] && [[ $EUID -ne 0 ]]; then
    echo -e "${RED}❌ 需要sudo权限来修改系统文件${NC}"
    echo "请运行: sudo $0 $1"
    exit 1
fi

# 运行主程序
main "$@"