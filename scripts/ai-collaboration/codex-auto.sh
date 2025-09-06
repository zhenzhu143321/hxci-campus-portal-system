#!/bin/bash

# Codex CLI 自动化执行工具 - 类似Gemini的非交互式使用
# 作者：Claude Code AI
# 日期：2025-09-06

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录（强制在项目内执行）
PROJECT_ROOT="/opt/hxci-campus-portal/hxci-campus-portal-system"

# OpenRouter API密钥
export OPENROUTER_API_KEY="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"

# 使用说明
show_usage() {
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}   Codex CLI 自动化执行工具${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo ""
    echo "用法: codex-auto <命令> [选项] <任务描述>"
    echo ""
    echo "命令:"
    echo "  analyze      分析代码（只读模式）"
    echo "  fix          修复问题（自动编辑模式）"
    echo "  refactor     重构代码（自动编辑模式）"
    echo "  test         生成测试（自动编辑模式）"
    echo "  docs         生成文档（自动编辑模式）"
    echo "  exec         执行任意任务（自定义模式）"
    echo ""
    echo "示例:"
    echo "  codex-auto analyze '分析三重Token认证架构'"
    echo "  codex-auto fix '修复JWT认证失败的问题'"
    echo "  codex-auto refactor '重构NotificationController'"
    echo "  codex-auto test '为认证模块写单元测试'"
    echo "  codex-auto docs '生成API文档'"
    echo "  codex-auto exec --full-auto '优化数据库查询'"
    echo ""
    echo "选项:"
    echo "  --read-only      只读模式，不修改文件"
    echo "  --full-auto      全自动模式（谨慎使用）"
    echo "  --json           输出JSON格式"
    echo "  --output <file>  保存输出到文件"
    echo ""
}

# 切换到项目目录
cd_to_project() {
    if [ "$PWD" != "$PROJECT_ROOT" ]; then
        echo -e "${YELLOW}📁 切换到项目目录: $PROJECT_ROOT${NC}"
        cd "$PROJECT_ROOT"
    fi
    echo -e "${GREEN}✅ 当前目录: $PWD${NC}"
}

# 分析代码（只读模式）
analyze_code() {
    local prompt="$1"
    echo -e "${BLUE}🔍 分析代码: $prompt${NC}"
    cd_to_project
    
    codex exec \
        --sandbox read-only \
        --skip-git-repo-check \
        --color always \
        "$prompt"
}

# 修复问题（自动编辑模式）
fix_issue() {
    local prompt="$1"
    echo -e "${YELLOW}🔧 修复问题: $prompt${NC}"
    cd_to_project
    
    codex exec \
        --sandbox workspace-write \
        --full-auto \
        --skip-git-repo-check \
        --color always \
        "修复这个问题: $prompt"
}

# 重构代码
refactor_code() {
    local prompt="$1"
    echo -e "${BLUE}♻️ 重构代码: $prompt${NC}"
    cd_to_project
    
    codex exec \
        --sandbox workspace-write \
        --full-auto \
        --skip-git-repo-check \
        --color always \
        "重构: $prompt"
}

# 生成测试
generate_tests() {
    local prompt="$1"
    echo -e "${GREEN}🧪 生成测试: $prompt${NC}"
    cd_to_project
    
    codex exec \
        --sandbox workspace-write \
        --full-auto \
        --skip-git-repo-check \
        --color always \
        "编写测试: $prompt"
}

# 生成文档
generate_docs() {
    local prompt="$1"
    echo -e "${BLUE}📝 生成文档: $prompt${NC}"
    cd_to_project
    
    codex exec \
        --sandbox workspace-write \
        --full-auto \
        --skip-git-repo-check \
        --color always \
        "生成文档: $prompt"
}

# 执行自定义任务
exec_custom() {
    shift # 移除'exec'参数
    local args=""
    local prompt=""
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            --read-only)
                args="$args --sandbox read-only"
                shift
                ;;
            --full-auto)
                args="$args --full-auto"
                shift
                ;;
            --json)
                args="$args --json"
                shift
                ;;
            --output)
                args="$args --output-last-message $2"
                shift 2
                ;;
            *)
                prompt="$*"
                break
                ;;
        esac
    done
    
    echo -e "${YELLOW}⚡ 执行任务: $prompt${NC}"
    cd_to_project
    
    eval "codex exec --skip-git-repo-check --color always $args \"$prompt\""
}

# 主函数
main() {
    # 检查参数
    if [ $# -eq 0 ]; then
        show_usage
        exit 0
    fi
    
    # 检查Codex是否安装
    if ! command -v codex &> /dev/null; then
        echo -e "${RED}❌ Codex CLI未安装${NC}"
        echo "请先运行: npm install -g @openai/codex"
        exit 1
    fi
    
    # 获取命令
    command="$1"
    shift
    
    # 执行相应命令
    case "$command" in
        analyze)
            analyze_code "$*"
            ;;
        fix)
            fix_issue "$*"
            ;;
        refactor)
            refactor_code "$*"
            ;;
        test)
            generate_tests "$*"
            ;;
        docs)
            generate_docs "$*"
            ;;
        exec)
            exec_custom "$@"
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            echo -e "${RED}❌ 未知命令: $command${NC}"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

# 运行主函数
main "$@"