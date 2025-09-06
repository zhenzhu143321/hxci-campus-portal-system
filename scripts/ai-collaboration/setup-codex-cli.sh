#!/bin/bash

# Codex CLI + OpenRouter 集成脚本
# 用途：配置Codex CLI使用OpenRouter作为后端
# 作者：Claude Code AI
# 日期：2025-09-06

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# OpenRouter API密钥
OPENROUTER_API_KEY="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"

# 配置目录
CODEX_CONFIG_DIR="$HOME/.codex"
PROJECT_CONFIG_DIR="/opt/hxci-campus-portal/hxci-campus-portal-system/scripts/ai-collaboration"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Codex CLI + OpenRouter 集成工具${NC}"
echo -e "${BLUE}========================================${NC}"

# 函数：检查命令是否存在
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# 函数：安装Codex CLI
install_codex() {
    echo -e "${YELLOW}📦 安装Codex CLI...${NC}"
    
    # 检查npm是否安装
    if ! command_exists npm; then
        echo -e "${RED}❌ npm未安装，请先安装Node.js${NC}"
        echo "运行: sudo apt-get install nodejs npm"
        exit 1
    fi
    
    # 安装Codex CLI
    if command_exists codex; then
        echo -e "${GREEN}✅ Codex CLI已安装，升级到最新版本...${NC}"
        npm update -g @openai/codex
    else
        echo -e "${YELLOW}📥 安装Codex CLI...${NC}"
        npm install -g @openai/codex
    fi
    
    # 验证安装
    if command_exists codex; then
        echo -e "${GREEN}✅ Codex CLI安装成功${NC}"
        codex --version
    else
        echo -e "${RED}❌ Codex CLI安装失败${NC}"
        exit 1
    fi
}

# 函数：配置OpenRouter
configure_openrouter() {
    echo -e "${YELLOW}⚙️  配置OpenRouter作为后端...${NC}"
    
    # 创建配置目录
    mkdir -p "$CODEX_CONFIG_DIR"
    
    # 创建config.json
    cat > "$CODEX_CONFIG_DIR/config.json" <<EOF
{
  "provider": "openrouter",
  "model": "openai/gpt-5",
  "providers": {
    "openrouter": {
      "name": "OpenRouter",
      "baseURL": "https://openrouter.ai/api/v1",
      "envKey": "OPENROUTER_API_KEY"
    }
  },
  "approval_mode": "suggest",
  "max_tokens": 3000,
  "temperature": 0.3
}
EOF
    
    echo -e "${GREEN}✅ config.json已创建${NC}"
    
    # 创建config.toml（某些版本使用）
    cat > "$CODEX_CONFIG_DIR/config.toml" <<EOF
model = "openai/gpt-5"
model_provider = "openrouter"

[model_providers.openrouter]
name = "OpenRouter"
base_url = "https://openrouter.ai/api/v1"
api_key_env = "OPENROUTER_API_KEY"

[features]
multimodal = true
auto_edit = false
suggest_mode = true

[mcp_servers]
# MCP服务器配置（可选）
EOF
    
    echo -e "${GREEN}✅ config.toml已创建${NC}"
    
    # 设置环境变量
    echo -e "${YELLOW}📝 设置环境变量...${NC}"
    
    # 添加到.bashrc
    if ! grep -q "OPENROUTER_API_KEY" ~/.bashrc; then
        echo "" >> ~/.bashrc
        echo "# Codex CLI with OpenRouter" >> ~/.bashrc
        echo "export OPENROUTER_API_KEY=\"$OPENROUTER_API_KEY\"" >> ~/.bashrc
        echo -e "${GREEN}✅ 环境变量已添加到~/.bashrc${NC}"
    else
        echo -e "${YELLOW}⚠️  环境变量已存在${NC}"
    fi
    
    # 立即导出
    export OPENROUTER_API_KEY="$OPENROUTER_API_KEY"
}

# 函数：创建快捷命令
create_shortcuts() {
    echo -e "${YELLOW}🔧 创建快捷命令...${NC}"
    
    # 创建codex-analyze命令
    cat > "$PROJECT_CONFIG_DIR/codex-analyze" <<'EOF'
#!/bin/bash
# 代码分析快捷命令

export OPENROUTER_API_KEY="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"

if [ -z "$1" ]; then
    echo "用法: codex-analyze <分析需求>"
    echo "示例: codex-analyze '分析这个项目的架构'"
    exit 1
fi

codex "$1"
EOF
    
    chmod +x "$PROJECT_CONFIG_DIR/codex-analyze"
    
    # 创建codex-fix命令
    cat > "$PROJECT_CONFIG_DIR/codex-fix" <<'EOF'
#!/bin/bash
# Bug修复快捷命令

export OPENROUTER_API_KEY="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"

if [ -z "$1" ]; then
    echo "用法: codex-fix <bug描述>"
    echo "示例: codex-fix '修复登录认证失败的问题'"
    exit 1
fi

codex "找出并修复这个问题: $1"
EOF
    
    chmod +x "$PROJECT_CONFIG_DIR/codex-fix"
    
    # 创建codex-refactor命令
    cat > "$PROJECT_CONFIG_DIR/codex-refactor" <<'EOF'
#!/bin/bash
# 代码重构快捷命令

export OPENROUTER_API_KEY="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"

if [ -z "$1" ]; then
    echo "用法: codex-refactor <重构需求>"
    echo "示例: codex-refactor '重构通知模块提高性能'"
    exit 1
fi

codex "重构代码: $1"
EOF
    
    chmod +x "$PROJECT_CONFIG_DIR/codex-refactor"
    
    echo -e "${GREEN}✅ 快捷命令已创建${NC}"
}

# 函数：测试配置
test_configuration() {
    echo -e "${YELLOW}🧪 测试配置...${NC}"
    
    # 测试API连接
    echo -e "${BLUE}测试OpenRouter连接...${NC}"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "Authorization: Bearer $OPENROUTER_API_KEY" \
        -H "Content-Type: application/json" \
        https://openrouter.ai/api/v1/models)
    
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✅ OpenRouter API连接成功${NC}"
    else
        echo -e "${RED}❌ OpenRouter API连接失败 (HTTP $response)${NC}"
        exit 1
    fi
    
    # 测试Codex命令
    echo -e "${BLUE}测试Codex CLI...${NC}"
    
    if codex --version >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Codex CLI运行正常${NC}"
    else
        echo -e "${RED}❌ Codex CLI运行失败${NC}"
        exit 1
    fi
}

# 函数：显示使用说明
show_usage() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   🎉 安装配置完成！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}📖 使用方法：${NC}"
    echo ""
    echo -e "${YELLOW}1. 基础使用:${NC}"
    echo "   codex '分析项目架构'"
    echo "   codex '修复这个bug: [描述]'"
    echo "   codex '为这个功能写测试'"
    echo ""
    echo -e "${YELLOW}2. 快捷命令:${NC}"
    echo "   $PROJECT_CONFIG_DIR/codex-analyze '分析需求'"
    echo "   $PROJECT_CONFIG_DIR/codex-fix 'bug描述'"
    echo "   $PROJECT_CONFIG_DIR/codex-refactor '重构需求'"
    echo ""
    echo -e "${YELLOW}3. 高级功能:${NC}"
    echo "   codex --auto-edit '自动修复所有lint错误'"
    echo "   codex --suggest '建议改进方案'"
    echo ""
    echo -e "${YELLOW}4. 配置文件:${NC}"
    echo "   ~/.codex/config.json - 主配置文件"
    echo "   ~/.codex/config.toml - 备用配置"
    echo ""
    echo -e "${GREEN}💡 提示: 记得先export环境变量${NC}"
    echo "   export OPENROUTER_API_KEY=\"$OPENROUTER_API_KEY\""
    echo ""
}

# 主流程
main() {
    echo ""
    
    # 检查操作系统
    if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
        echo -e "${YELLOW}⚠️  Windows系统检测到，建议使用WSL${NC}"
    fi
    
    # 安装Codex CLI
    install_codex
    
    # 配置OpenRouter
    configure_openrouter
    
    # 创建快捷命令
    create_shortcuts
    
    # 测试配置
    test_configuration
    
    # 显示使用说明
    show_usage
    
    echo -e "${GREEN}✨ 一切就绪！开始使用Codex CLI吧！${NC}"
}

# 运行主流程
main "$@"