#!/bin/bash
# AI协作代码上下文收集器 - 强制性代码收集工具
# 确保智能体调用AI时必须传递完整代码上下文

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="/opt/hxci-campus-portal/hxci-campus-portal-system"
CONTEXT_TEMP_DIR="$SCRIPT_DIR/context-temp"
LOG_DIR="$SCRIPT_DIR/logs"
CONTEXT_LOG="$LOG_DIR/context-collection-$(date +%Y%m%d_%H%M%S).log"

# 确保必要目录存在
mkdir -p "$CONTEXT_TEMP_DIR" "$LOG_DIR"

# 上下文收集状态
COLLECTED_FILES=()
MISSING_FILES=()
CONTEXT_COMPLETE=false

# 日志函数
log() {
    echo -e "$1" | tee -a "$CONTEXT_LOG"
}

# 错误处理
error_exit() {
    log "${RED}❌ 错误: $1${NC}"
    exit 1
}

# 警告提示
warn() {
    log "${YELLOW}⚠️ 警告: $1${NC}"
}

# 成功提示
success() {
    log "${GREEN}✅ $1${NC}"
}

# 信息提示
info() {
    log "${BLUE}ℹ️ $1${NC}"
}

# 显示使用帮助
show_help() {
    cat << EOF
AI协作代码上下文收集器 - 确保完整代码上下文传递

使用方法:
    $0 [选项] <任务类型> <主要文件路径>

任务类型:
    java-api        - Java API开发任务
    vue-component   - Vue组件开发任务
    full-stack      - 全栈开发任务
    bug-fix         - Bug修复任务
    review          - 代码审查任务

选项:
    -h, --help      显示帮助信息
    -v, --validate  仅验证上下文完整性
    -f, --force     强制收集所有相关文件
    -o, --output    指定输出文件路径

示例:
    $0 java-api /path/to/MockAuthController.java
    $0 vue-component /path/to/Home.vue
    $0 bug-fix /path/to/problematic-file.java

EOF
    exit 0
}

# 验证文件存在性
validate_file() {
    local file_path="$1"
    if [[ ! -f "$file_path" ]]; then
        MISSING_FILES+=("$file_path")
        warn "文件不存在: $file_path"
        return 1
    fi
    return 0
}

# 收集Java API相关文件
collect_java_api_context() {
    local main_file="$1"
    local file_name=$(basename "$main_file" .java)
    
    info "📦 收集Java API上下文: $file_name"
    
    # P0: 必须收集的核心文件
    local p0_files=(
        "$main_file"  # 主文件
    )
    
    # 自动检测相关Service
    if [[ "$file_name" == *Controller ]]; then
        local service_name="${file_name/Controller/Service}"
        local service_impl="${file_name/Controller/ServiceImpl}"
        
        # 查找Service接口和实现
        local service_files=$(find "$PROJECT_ROOT" -name "${service_name}.java" -o -name "${service_impl}.java" 2>/dev/null)
        for file in $service_files; do
            p0_files+=("$file")
        done
    fi
    
    # 自动检测相关DTO/Entity
    local dto_pattern=$(grep -h "import.*dto\." "$main_file" 2>/dev/null | sed 's/.*dto\.\(.*\);/\1/' | head -5)
    for dto in $dto_pattern; do
        local dto_files=$(find "$PROJECT_ROOT" -name "${dto}.java" 2>/dev/null | head -1)
        [[ -n "$dto_files" ]] && p0_files+=("$dto_files")
    done
    
    # 收集P0级文件
    info "📋 P0级必收集文件 (${#p0_files[@]}个):"
    for file in "${p0_files[@]}"; do
        if validate_file "$file"; then
            COLLECTED_FILES+=("$file")
            echo "  ✓ $(basename "$file")"
        fi
    done
    
    # P1: 重要的配置和工具类
    info "📋 P1级相关文件扫描..."
    
    # 查找相关配置文件
    local config_files=$(grep -l "$(basename "$file_name" .java)" "$PROJECT_ROOT"/**/application*.yaml 2>/dev/null | head -3)
    for config in $config_files; do
        if [[ -f "$config" ]]; then
            COLLECTED_FILES+=("$config")
            echo "  ✓ 配置文件: $(basename "$config")"
        fi
    done
}

# 收集Vue组件相关文件
collect_vue_component_context() {
    local main_file="$1"
    local component_name=$(basename "$main_file" .vue)
    
    info "🎨 收集Vue组件上下文: $component_name"
    
    # P0: 必须收集的核心文件
    local p0_files=(
        "$main_file"  # 主组件文件
    )
    
    # 查找相关API文件
    local api_imports=$(grep -h "from.*api/" "$main_file" 2>/dev/null | sed "s/.*from.*['\"].*\/\(.*\)['\"].*/\1/" | head -5)
    for api in $api_imports; do
        local api_file="$PROJECT_ROOT/hxci-campus-portal/src/api/${api}.ts"
        [[ -f "$api_file" ]] && p0_files+=("$api_file")
    done
    
    # 查找相关Store文件
    local store_imports=$(grep -h "from.*stores/" "$main_file" 2>/dev/null | sed "s/.*from.*['\"].*\/\(.*\)['\"].*/\1/" | head -5)
    for store in $store_imports; do
        local store_file="$PROJECT_ROOT/hxci-campus-portal/src/stores/${store}.ts"
        [[ -f "$store_file" ]] && p0_files+=("$store_file")
    done
    
    # 收集文件
    for file in "${p0_files[@]}"; do
        if validate_file "$file"; then
            COLLECTED_FILES+=("$file")
            echo "  ✓ $(basename "$file")"
        fi
    done
}

# 生成上下文报告
generate_context_report() {
    local output_file="$CONTEXT_TEMP_DIR/context-$(date +%Y%m%d_%H%M%S).md"
    
    info "📝 生成上下文报告..."
    
    cat > "$output_file" << EOF
# AI协作代码上下文报告

生成时间: $(date '+%Y-%m-%d %H:%M:%S')
项目: 哈尔滨信息工程学院校园门户系统
技术栈: Spring Boot 3.4.5 + Vue 3

## 📦 收集的文件 (${#COLLECTED_FILES[@]}个)

EOF
    
    # 添加收集的文件内容
    for file in "${COLLECTED_FILES[@]}"; do
        local relative_path="${file#$PROJECT_ROOT/}"
        local file_ext="${file##*.}"
        
        cat >> "$output_file" << EOF
### 文件: $relative_path

\`\`\`$file_ext
$(cat "$file")
\`\`\`

---

EOF
    done
    
    # 添加缺失文件警告
    if [[ ${#MISSING_FILES[@]} -gt 0 ]]; then
        cat >> "$output_file" << EOF
## ⚠️ 缺失的文件 (${#MISSING_FILES[@]}个)

EOF
        for file in "${MISSING_FILES[@]}"; do
            echo "- $file" >> "$output_file"
        done
    fi
    
    success "上下文报告已生成: $output_file"
    echo "$output_file"
}

# 验证上下文完整性
validate_context_completeness() {
    info "🔍 验证上下文完整性..."
    
    local total_files=${#COLLECTED_FILES[@]}
    local missing_count=${#MISSING_FILES[@]}
    
    if [[ $missing_count -eq 0 && $total_files -gt 0 ]]; then
        CONTEXT_COMPLETE=true
        success "上下文完整性验证通过! 收集了 $total_files 个文件"
    else
        CONTEXT_COMPLETE=false
        if [[ $missing_count -gt 0 ]]; then
            error_exit "上下文不完整! 缺失 $missing_count 个必要文件"
        fi
        if [[ $total_files -eq 0 ]]; then
            error_exit "未收集到任何文件!"
        fi
    fi
}

# 强制检查清单
run_mandatory_checklist() {
    info "📋 执行强制检查清单..."
    
    local checks_passed=0
    local checks_total=5
    
    # 检查1: 主文件是否存在
    if [[ ${#COLLECTED_FILES[@]} -gt 0 ]]; then
        checks_passed=$((checks_passed + 1))
        echo "  ✅ 主文件已收集"
    else
        echo "  ❌ 主文件未收集"
    fi
    
    # 检查2: 是否包含相关依赖
    if [[ ${#COLLECTED_FILES[@]} -gt 1 ]]; then
        checks_passed=$((checks_passed + 1))
        echo "  ✅ 相关依赖已收集"
    else
        echo "  ❌ 未收集相关依赖"
    fi
    
    # 检查3: 代码完整性
    local total_lines=0
    for file in "${COLLECTED_FILES[@]}"; do
        if [[ -f "$file" ]]; then
            lines=$(wc -l < "$file")
            total_lines=$((total_lines + lines))
        fi
    done
    if [[ $total_lines -gt 100 ]]; then
        checks_passed=$((checks_passed + 1))
        echo "  ✅ 代码内容充分 (${total_lines}行)"
    else
        echo "  ⚠️ 代码内容较少 (${total_lines}行)"
    fi
    
    # 检查4: 无缺失文件
    if [[ ${#MISSING_FILES[@]} -eq 0 ]]; then
        checks_passed=$((checks_passed + 1))
        echo "  ✅ 无缺失文件"
    else
        echo "  ❌ 存在缺失文件 (${#MISSING_FILES[@]}个)"
    fi
    
    # 检查5: 包含配置或测试
    local has_config=false
    for file in "${COLLECTED_FILES[@]}"; do
        if [[ "$file" == *application*.yaml ]] || [[ "$file" == *.properties ]]; then
            has_config=true
            break
        fi
    done
    if [[ "$has_config" == true ]]; then
        checks_passed=$((checks_passed + 1))
        echo "  ✅ 包含配置文件"
    else
        echo "  ⚠️ 未包含配置文件"
    fi
    
    # 总结
    echo ""
    if [[ $checks_passed -eq $checks_total ]]; then
        success "强制检查清单: $checks_passed/$checks_total 全部通过! ✅"
    elif [[ $checks_passed -ge 3 ]]; then
        warn "强制检查清单: $checks_passed/$checks_total 基本通过 ⚠️"
    else
        error_exit "强制检查清单: $checks_passed/$checks_total 未通过! ❌"
    fi
}

# 主流程
main() {
    # 参数解析
    if [[ $# -lt 2 ]] || [[ "$1" == "-h" ]] || [[ "$1" == "--help" ]]; then
        show_help
    fi
    
    local task_type="$1"
    local main_file="$2"
    
    # 验证主文件
    if [[ ! -f "$main_file" ]]; then
        error_exit "主文件不存在: $main_file"
    fi
    
    info "🚀 开始AI协作代码上下文收集"
    info "任务类型: $task_type"
    info "主文件: $(basename "$main_file")"
    echo ""
    
    # 根据任务类型收集上下文
    case "$task_type" in
        java-api)
            collect_java_api_context "$main_file"
            ;;
        vue-component)
            collect_vue_component_context "$main_file"
            ;;
        full-stack)
            collect_java_api_context "$main_file"
            # 可以添加更多全栈相关的收集逻辑
            ;;
        bug-fix|review)
            # Bug修复和代码审查需要更广泛的上下文
            collect_java_api_context "$main_file"
            ;;
        *)
            error_exit "未知的任务类型: $task_type"
            ;;
    esac
    
    # 验证完整性
    validate_context_completeness
    
    # 执行强制检查清单
    run_mandatory_checklist
    
    # 生成报告
    local report_file=$(generate_context_report)
    
    # 最终输出
    echo ""
    info "📊 上下文收集统计:"
    echo "  • 收集文件数: ${#COLLECTED_FILES[@]}"
    echo "  • 缺失文件数: ${#MISSING_FILES[@]}"
    echo "  • 报告位置: $report_file"
    
    # 如果上下文完整，自动调用AI助手
    if [[ "$CONTEXT_COMPLETE" == true ]]; then
        echo ""
        success "上下文收集完成! 可以安全调用AI助手"
        echo ""
        echo "推荐调用命令:"
        echo "  $SCRIPT_DIR/ai-assistant.sh auto \"$(cat $report_file)\""
    else
        warn "上下文不完整，不建议调用AI助手"
    fi
}

# 执行主流程
main "$@"