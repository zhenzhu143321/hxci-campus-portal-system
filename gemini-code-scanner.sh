#!/bin/bash

# Gemini CLI 项目代码扫描工具 (增强版)
# 基于Gemini CLI的高级功能，提供专业的代码分析能力
# 版本: v2.0
# 作者: Gemini 4-KEY 智能轮换系统

# 配置文件路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GEMINI_TURBO="$SCRIPT_DIR/gemini-turbo-4key.sh"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 显示使用帮助
show_help() {
    echo -e "${BLUE}================ Gemini CLI 项目代码扫描工具 v2.0 ================${NC}"
    echo ""
    echo -e "${YELLOW}🚀 基于Gemini CLI高级功能的专业代码分析工具${NC}"
    echo ""
    echo -e "${YELLOW}📋 使用方法:${NC}"
    echo "  $0 <command> [options] [target_path]"
    echo ""
    echo -e "${YELLOW}🔧 核心命令:${NC}"
    echo "  scan-full <path>        - 全项目深度扫描 (使用--all-files)"
    echo "  scan-target <path>      - 目标目录扫描 (使用--include-directories)"
    echo "  scan-debug <path>       - Debug模式扫描 (使用-d)"
    echo "  scan-security <path>    - 安全专项扫描"
    echo "  scan-api <path>         - API接口专项扫描"
    echo "  scan-performance <path> - 性能相关代码扫描"
    echo "  analyze-arch <path>     - 架构分析"
    echo "  analyze-db <path>       - 数据库相关代码分析"
    echo "  analyze-cache <path>    - 缓存系统分析"
    echo ""
    echo -e "${YELLOW}🛠️  工具命令:${NC}"
    echo "  interactive <path>      - 启动交互式分析会话"
    echo "  memory-stats <path>     - 显示内存使用统计"
    echo "  list-files <path>       - 列出项目文件结构"
    echo "  status                  - 显示当前扫描工具状态"
    echo ""
    echo -e "${YELLOW}💡 使用示例:${NC}"
    echo "  ./gemini-code-scanner.sh scan-full /home/user/myproject"
    echo "  ./gemini-code-scanner.sh scan-security /home/user/myproject/src"
    echo "  ./gemini-code-scanner.sh interactive /home/user/myproject"
    echo ""
    echo -e "${GREEN}✨ 高级功能特性:${NC}"
    echo "  • 智能API Key轮换，避免速率限制"
    echo "  • Debug模式深度分析"
    echo "  • 内存使用监控"
    echo "  • 目标目录精确扫描"
    echo "  • 专项安全和性能分析"
    echo "  • 交互式分析会话"
}

# 检查必要的工具和配置
check_prerequisites() {
    if [[ ! -f "$GEMINI_TURBO" ]]; then
        echo -e "${RED}❌ 错误: 找不到Gemini 4-KEY轮换脚本: $GEMINI_TURBO${NC}"
        return 1
    fi
    
    if ! command -v gemini >/dev/null 2>&1; then
        echo -e "${RED}❌ 错误: Gemini CLI未安装或不在PATH中${NC}"
        return 1
    fi
    
    return 0
}

# 执行带轮换的Gemini调用
execute_with_rotation() {
    local prompt="$1"
    local additional_args="$2"
    
    echo -e "${CYAN}🚀 执行Gemini分析 (使用4-KEY轮换)...${NC}"
    
    # 使用轮换脚本执行调用
    "$GEMINI_TURBO" call "$prompt"
}

# 直接使用Gemini CLI执行高级功能
execute_direct_gemini() {
    local args="$1"
    local prompt="$2"
    local target_path="$3"
    
    echo -e "${CYAN}🔍 直接执行Gemini CLI高级功能...${NC}"
    
    if [[ -n "$target_path" ]]; then
        cd "$target_path" || { echo -e "${RED}❌ 无法进入目录: $target_path${NC}"; return 1; }
    fi
    
    if [[ -n "$prompt" ]]; then
        echo "$prompt" | gemini $args
    else
        gemini $args
    fi
}

# 全项目深度扫描
scan_full() {
    local target_path="$1"
    
    echo -e "${BLUE}🌐 全项目深度扫描 (--all-files)${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请对这个项目进行全面的代码分析，包括：

1. 项目整体架构和技术栈分析
2. 核心业务逻辑识别
3. 数据库和缓存设计分析
4. API接口设计评估
5. 安全实现分析
6. 性能优化点识别
7. 代码质量评估
8. 潜在改进建议

请提供详细的分析报告。"
    
    execute_direct_gemini "--all-files -p" "$prompt" "$target_path"
}

# 目标目录精确扫描
scan_target() {
    local target_path="$1"
    local include_dirs="$2"
    
    if [[ -z "$include_dirs" ]]; then
        # 为HXCI校园门户项目自动检测关键目录结构
        if [[ "$target_path" == *"hxci-campus-portal-system"* ]]; then
            include_dirs="yudao-boot-mini,hxci-campus-portal,scripts,demo"
        else
            # 通用项目目录结构
            include_dirs="src,lib,app,controllers,services,components"
        fi
    fi
    
    echo -e "${BLUE}🎯 目标目录精确扫描${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    echo -e "${YELLOW}包含目录: $include_dirs${NC}"
    
    local prompt="请分析指定目录的代码结构和实现，重点关注：
1. 目录结构和组织方式
2. 核心代码文件分析
3. 设计模式和架构模式
4. 依赖关系和接口设计
5. 潜在的问题和改进点

请提供详细的目录分析报告。"
    
    execute_with_rotation "$prompt"
}

# Debug模式深度扫描
scan_debug() {
    local target_path="$1"
    
    echo -e "${BLUE}🔍 Debug模式深度扫描${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    echo -e "${PURPLE}注意: Debug模式将显示详细的扫描过程${NC}"
    
    local prompt="使用debug模式深度分析项目代码，特别关注：
1. 文件加载和解析过程
2. 内存使用模式分析
3. 代码扫描的详细过程
4. 潜在的性能瓶颈
5. 异常和错误处理

请提供debug级别的分析报告。"
    
    execute_direct_gemini "-d --all-files -p" "$prompt" "$target_path"
}

# 安全专项扫描
scan_security() {
    local target_path="$1"
    
    echo -e "${BLUE}🛡️  安全专项扫描${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请对项目进行全面的安全分析，重点关注：

1. 认证和授权机制分析
2. JWT安全实现检查
3. CSRF防护机制评估
4. SQL注入防护分析
5. XSS防护措施检查
6. 权限控制逻辑审查
7. 敏感数据处理分析
8. API安全设计评估
9. 安全配置检查
10. 潜在安全漏洞识别

请提供详细的安全分析报告，包括发现的问题和改进建议。"
    
    execute_with_rotation "$prompt"
}

# API接口专项扫描
scan_api() {
    local target_path="$1"
    
    echo -e "${BLUE}📡 API接口专项扫描${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请对项目的API接口进行专项分析：

1. REST API设计规范检查
2. 接口路由和参数设计
3. 请求/响应数据格式分析
4. API版本管理策略
5. 接口安全机制检查
6. 错误处理和状态码使用
7. API文档完整性评估
8. 接口性能和缓存策略
9. 跨域处理机制
10. API测试覆盖分析

请提供完整的API接口分析报告。"
    
    execute_with_rotation "$prompt"
}

# 性能相关代码扫描
scan_performance() {
    local target_path="$1"
    
    echo -e "${BLUE}⚡ 性能相关代码扫描${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请对项目进行性能相关代码分析：

1. 数据库查询优化分析
2. 缓存策略和实现检查
3. 异步处理和并发设计
4. 内存使用和垃圾回收
5. 网络请求优化分析
6. 前端性能优化检查
7. 资源加载和打包策略
8. 算法复杂度分析
9. 性能监控和指标
10. 瓶颈识别和优化建议

请提供详细的性能分析报告。"
    
    execute_with_rotation "$prompt"
}

# 架构分析
analyze_arch() {
    local target_path="$1"
    
    echo -e "${BLUE}🏗️  架构分析${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请对项目架构进行深度分析：

1. 整体架构模式识别
2. 模块划分和依赖关系
3. 设计模式使用分析
4. 代码分层和职责划分
5. 组件耦合度评估
6. 可扩展性分析
7. 可维护性评估
8. 架构决策评价
9. 技术选型合理性
10. 架构改进建议

请提供专业的架构分析报告。"
    
    execute_with_rotation "$prompt"
}

# 数据库相关分析
analyze_db() {
    local target_path="$1"
    
    echo -e "${BLUE}🗄️  数据库相关代码分析${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请分析项目中数据库相关的代码：

1. 数据库设计和表结构分析
2. ORM框架使用情况
3. SQL查询优化分析
4. 数据库连接池配置
5. 事务管理机制
6. 数据迁移和版本控制
7. 索引使用和性能优化
8. 数据库安全措施
9. 备份和恢复策略
10. 数据一致性保证

请提供详细的数据库分析报告。"
    
    execute_with_rotation "$prompt"
}

# 缓存系统分析
analyze_cache() {
    local target_path="$1"
    
    echo -e "${BLUE}🚀 缓存系统分析${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请深度分析项目的缓存系统实现：

1. 缓存架构和设计模式
2. Redis配置和使用策略
3. 缓存键设计和命名规范
4. 缓存过期和淘汰策略
5. 缓存一致性保证机制
6. 缓存性能优化分析
7. 缓存穿透和雪崩防护
8. 分布式缓存协调
9. 缓存监控和指标
10. 特别关注P0级权限缓存系统

请提供专业的缓存系统分析报告。"
    
    execute_with_rotation "$prompt"
}

# 交互式分析会话
interactive() {
    local target_path="$1"
    
    echo -e "${BLUE}💬 启动交互式分析会话${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    echo -e "${PURPLE}提示: 使用-i参数启动交互模式，可以持续对话${NC}"
    
    if [[ -n "$target_path" ]]; then
        cd "$target_path" || { echo -e "${RED}❌ 无法进入目录: $target_path${NC}"; return 1; }
    fi
    
    echo -e "${CYAN}🚀 启动Gemini CLI交互模式...${NC}"
    gemini --all-files -i "你好！我想对这个项目进行深度代码分析。请先给我一个项目概览，然后我们可以针对具体部分进行详细讨论。"
}

# 内存使用统计
memory_stats() {
    local target_path="$1"
    
    echo -e "${BLUE}📊 内存使用统计${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    local prompt="请分析这个项目在代码扫描过程中的内存使用情况，并提供项目规模统计。"
    
    execute_direct_gemini "--show-memory-usage --all-files -p" "$prompt" "$target_path"
}

# 列出项目文件结构
list_files() {
    local target_path="$1"
    
    echo -e "${BLUE}📁 项目文件结构${NC}"
    echo -e "${YELLOW}目标路径: $target_path${NC}"
    
    if [[ -n "$target_path" ]] && [[ -d "$target_path" ]]; then
        echo -e "${CYAN}使用tree命令显示项目结构:${NC}"
        if command -v tree >/dev/null 2>&1; then
            tree -L 3 -I 'node_modules|.git|target|dist|build' "$target_path"
        else
            echo -e "${YELLOW}tree命令未安装，使用ls -la替代:${NC}"
            ls -la "$target_path"
        fi
    else
        echo -e "${RED}❌ 目标路径不存在或不是目录${NC}"
        return 1
    fi
}

# 显示工具状态
show_status() {
    echo -e "${BLUE}================ Gemini 代码扫描工具状态 ================${NC}"
    
    # 检查Gemini CLI
    if command -v gemini >/dev/null 2>&1; then
        local version=$(gemini --version 2>&1 | head -1)
        echo -e "${GREEN}✅ Gemini CLI: $version${NC}"
    else
        echo -e "${RED}❌ Gemini CLI: 未安装${NC}"
    fi
    
    # 检查4-KEY轮换脚本
    if [[ -f "$GEMINI_TURBO" ]]; then
        echo -e "${GREEN}✅ 4-KEY轮换脚本: 可用${NC}"
        
        # 显示API Key状态
        echo -e "${CYAN}🔑 API Key状态:${NC}"
        "$GEMINI_TURBO" status | grep -A 10 "各KEY使用情况"
    else
        echo -e "${RED}❌ 4-KEY轮换脚本: 不可用${NC}"
    fi
    
    # 显示可用的高级功能
    echo ""
    echo -e "${PURPLE}🚀 可用的Gemini CLI高级功能:${NC}"
    echo "  • --all-files: 包含所有项目文件"
    echo "  • --include-directories: 指定目录包含"
    echo "  • --debug: Debug模式详细输出"
    echo "  • --show-memory-usage: 显示内存使用"
    echo "  • --prompt-interactive: 交互模式"
    echo "  • --approval-mode: 自动批准模式"
}

# 主程序逻辑
main() {
    # 检查必要工具
    if ! check_prerequisites; then
        exit 1
    fi
    
    local command="$1"
    local target_path="$2"
    local extra_param="$3"
    
    case "$command" in
        "scan-full")
            scan_full "$target_path"
            ;;
        "scan-target")
            scan_target "$target_path" "$extra_param"
            ;;
        "scan-debug")
            scan_debug "$target_path"
            ;;
        "scan-security")
            scan_security "$target_path"
            ;;
        "scan-api")
            scan_api "$target_path"
            ;;
        "scan-performance")
            scan_performance "$target_path"
            ;;
        "analyze-arch")
            analyze_arch "$target_path"
            ;;
        "analyze-db")
            analyze_db "$target_path"
            ;;
        "analyze-cache")
            analyze_cache "$target_path"
            ;;
        "interactive")
            interactive "$target_path"
            ;;
        "memory-stats")
            memory_stats "$target_path"
            ;;
        "list-files")
            list_files "$target_path"
            ;;
        "status")
            show_status
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 运行主程序
main "$@"