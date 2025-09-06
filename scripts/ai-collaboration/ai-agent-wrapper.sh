#!/bin/bash
# AI智能体调用包装器 - 强制代码上下文传递
# 拦截所有AI调用，确保包含完整代码上下文

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTEXT_COLLECTOR="$SCRIPT_DIR/ai-context-collector.sh"
AI_ASSISTANT="$SCRIPT_DIR/ai-assistant.sh"
LOG_DIR="$SCRIPT_DIR/logs"
AGENT_LOG="$LOG_DIR/agent-calls-$(date +%Y%m%d).log"

# 确保日志目录存在
mkdir -p "$LOG_DIR"

# 全局变量
CONTEXT_VERIFIED=false
CALL_ID="$(date +%Y%m%d_%H%M%S)_$$"

# 日志函数
log() {
    local level="$1"
    shift
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] [$CALL_ID] [$level] $*" | tee -a "$AGENT_LOG"
}

# 错误处理
error_exit() {
    log "ERROR" "${RED}❌ $1${NC}"
    
    # 记录违规行为
    cat >> "$LOG_DIR/violations.log" << EOF
[$(date '+%Y-%m-%d %H:%M:%S')] 违规调用AI - 未传递代码上下文
调用ID: $CALL_ID
错误: $1
调用栈: ${BASH_SOURCE[*]}
EOF
    
    exit 1
}

# 警告
warn() {
    log "WARN" "${YELLOW}⚠️ $1${NC}"
}

# 成功
success() {
    log "INFO" "${GREEN}✅ $1${NC}"
}

# 信息
info() {
    log "INFO" "${BLUE}ℹ️ $1${NC}"
}

# 显示拦截警告
show_intercept_warning() {
    echo -e "${MAGENTA}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${MAGENTA}║           🚨 AI调用拦截器 - 代码上下文检查 🚨              ║${NC}"
    echo -e "${MAGENTA}╠══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${MAGENTA}║  此调用已被拦截，正在验证代码上下文完整性...                ║${NC}"
    echo -e "${MAGENTA}║  违反代码上下文传递铁律将被记录并警告!                      ║${NC}"
    echo -e "${MAGENTA}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# 分析调用内容
analyze_call_content() {
    local content="$1"
    local has_code=false
    local code_files=0
    local code_lines=0
    
    info "🔍 分析调用内容..."
    
    # 检查是否包含代码块
    if echo "$content" | grep -q '```'; then
        has_code=true
        code_files=$(echo "$content" | grep -c "文件:" || true)
        code_lines=$(echo "$content" | grep '```' -A 1000 | wc -l)
    fi
    
    # 检查是否包含文件路径
    local file_paths=$(echo "$content" | grep -oE "/[a-zA-Z0-9_/.]+\.(java|ts|vue|js|yaml)" | wc -l)
    
    # 输出分析结果
    echo "  • 包含代码块: $([ "$has_code" = true ] && echo "✅ 是" || echo "❌ 否")"
    echo "  • 代码文件数: $code_files"
    echo "  • 代码行数: $code_lines"
    echo "  • 文件路径数: $file_paths"
    
    # 判断是否需要代码上下文
    if echo "$content" | grep -qiE "(代码|实现|修复|优化|重构|bug|error|函数|方法|类|接口)"; then
        info "📌 检测到编程相关问题，需要代码上下文"
        
        if [[ $code_files -lt 1 ]] || [[ $code_lines -lt 50 ]]; then
            warn "代码上下文不足! 文件数: $code_files, 代码行数: $code_lines"
            return 1
        fi
    fi
    
    return 0
}

# 自动补充上下文
auto_supplement_context() {
    local original_content="$1"
    local task_type="$2"
    
    info "🔧 自动补充代码上下文..."
    
    # 从内容中提取可能的文件路径
    local detected_files=$(echo "$original_content" | grep -oE "/[a-zA-Z0-9_/.]+\.(java|ts|vue|js|yaml)" | head -1)
    
    if [[ -n "$detected_files" ]]; then
        info "检测到文件: $detected_files"
        
        # 调用上下文收集器
        local context_report=$($CONTEXT_COLLECTOR "$task_type" "$detected_files" 2>&1)
        
        if [[ $? -eq 0 ]]; then
            success "自动补充上下文成功!"
            # 合并原始内容和收集的上下文
            echo "$context_report"
            echo ""
            echo "---原始请求---"
            echo "$original_content"
            return 0
        else
            warn "自动补充上下文失败: $context_report"
        fi
    else
        warn "未能从请求中检测到文件路径"
    fi
    
    return 1
}

# 执行质量检查
run_quality_checks() {
    local content="$1"
    
    info "📊 执行质量检查..."
    
    local checks_passed=0
    local checks_total=6
    
    # 检查1: 包含项目说明
    if echo "$content" | grep -q "哈尔滨信息工程学院"; then
        ((checks_passed++))
        echo "  ✅ 包含项目背景"
    else
        echo "  ⚠️ 缺少项目背景"
    fi
    
    # 检查2: 包含技术栈说明
    if echo "$content" | grep -qE "Spring Boot|Vue"; then
        ((checks_passed++))
        echo "  ✅ 包含技术栈"
    else
        echo "  ⚠️ 缺少技术栈"
    fi
    
    # 检查3: 包含具体文件
    if echo "$content" | grep -qE "\.(java|ts|vue|yaml)"; then
        ((checks_passed++))
        echo "  ✅ 包含具体文件"
    else
        echo "  ❌ 缺少具体文件"
    fi
    
    # 检查4: 包含代码内容
    if echo "$content" | grep -q '```'; then
        ((checks_passed++))
        echo "  ✅ 包含代码内容"
    else
        echo "  ❌ 缺少代码内容"
    fi
    
    # 检查5: 明确的问题描述
    if echo "$content" | grep -qE "请|如何|为什么|什么|帮助|分析|实现"; then
        ((checks_passed++))
        echo "  ✅ 包含明确问题"
    else
        echo "  ⚠️ 问题描述不明确"
    fi
    
    # 检查6: 期望输出说明
    if echo "$content" | grep -qE "期望|输出|格式|返回|结果"; then
        ((checks_passed++))
        echo "  ✅ 包含期望输出"
    else
        echo "  ⚠️ 缺少期望输出"
    fi
    
    # 计算质量分数
    local quality_score=$((checks_passed * 100 / checks_total))
    
    echo ""
    if [[ $quality_score -ge 80 ]]; then
        success "质量检查: $checks_passed/$checks_total (${quality_score}%) 优秀! ✅"
        return 0
    elif [[ $quality_score -ge 60 ]]; then
        warn "质量检查: $checks_passed/$checks_total (${quality_score}%) 合格 ⚠️"
        return 0
    else
        error_exit "质量检查: $checks_passed/$checks_total (${quality_score}%) 不合格! ❌"
    fi
}

# 违规记录和警告
record_violation() {
    local violation_type="$1"
    local details="$2"
    
    local violations_file="$LOG_DIR/violations-$(date +%Y%m).csv"
    
    # 创建CSV头（如果文件不存在）
    if [[ ! -f "$violations_file" ]]; then
        echo "时间,调用ID,违规类型,详情" > "$violations_file"
    fi
    
    # 记录违规
    echo "$(date '+%Y-%m-%d %H:%M:%S'),$CALL_ID,$violation_type,\"$details\"" >> "$violations_file"
    
    # 显示警告
    echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║                    ⚠️ 违规警告 ⚠️                           ║${NC}"
    echo -e "${RED}╠══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${RED}║ 违规类型: $violation_type"
    echo -e "${RED}║ 详情: $details"
    echo -e "${RED}║ 此违规已记录，多次违规将触发强制培训!                      ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
}

# 主函数
main() {
    local model="${1:-auto}"
    local content="$2"
    local max_tokens="${3:-2000}"
    local temperature="${4:-0.3}"
    
    # 显示拦截警告
    show_intercept_warning
    
    # 记录调用
    log "INFO" "AI调用请求 - 模型: $model"
    
    # 分析内容
    if ! analyze_call_content "$content"; then
        warn "代码上下文不足，尝试自动补充..."
        
        # 尝试自动补充
        local task_type="java-api"  # 可以根据内容智能判断
        if echo "$content" | grep -q "\.vue"; then
            task_type="vue-component"
        fi
        
        local supplemented_content
        if supplemented_content=$(auto_supplement_context "$content" "$task_type"); then
            content="$supplemented_content"
            success "已自动补充代码上下文"
        else
            # 记录违规
            record_violation "缺少代码上下文" "调用AI时未传递完整代码"
            
            # 询问是否继续
            echo -e "${YELLOW}是否强制继续（不推荐）？[y/N]${NC}"
            read -r continue_anyway
            if [[ "$continue_anyway" != "y" ]]; then
                error_exit "调用已取消 - 缺少代码上下文"
            fi
        fi
    fi
    
    # 执行质量检查
    run_quality_checks "$content"
    
    # 调用实际的AI助手
    info "🚀 调用AI助手..."
    echo ""
    
    # 执行调用
    "$AI_ASSISTANT" "$model" "$content" "$max_tokens" "$temperature"
    
    # 记录成功
    success "AI调用完成"
    
    # 生成调用报告
    local report_file="$LOG_DIR/call-report-$CALL_ID.json"
    cat > "$report_file" << EOF
{
  "call_id": "$CALL_ID",
  "timestamp": "$(date -Iseconds)",
  "model": "$model",
  "context_verified": $CONTEXT_VERIFIED,
  "content_length": ${#content},
  "max_tokens": $max_tokens,
  "temperature": $temperature,
  "status": "success"
}
EOF
    
    info "调用报告已保存: $report_file"
}

# 执行
main "$@"