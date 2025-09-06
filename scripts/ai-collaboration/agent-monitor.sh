#!/bin/bash
# 智能体行为监控系统 - 监控AI调用质量和违规行为
# 自动生成报告和培训建议

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/logs"
REPORT_DIR="$SCRIPT_DIR/reports"
VIOLATIONS_FILE="$LOG_DIR/violations-$(date +%Y%m).csv"

# 确保目录存在
mkdir -p "$LOG_DIR" "$REPORT_DIR"

# 统计变量
TOTAL_CALLS=0
SUCCESSFUL_CALLS=0
FAILED_CALLS=0
VIOLATIONS_COUNT=0
QUALITY_SCORES=()

# 显示标题
show_header() {
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║          智能体AI协作行为监控系统 v1.0                      ║${NC}"
    echo -e "${CYAN}╠══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${CYAN}║  监控AI调用质量 · 追踪违规行为 · 生成培训建议              ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# 分析调用日志
analyze_call_logs() {
    echo -e "${BLUE}📊 分析调用日志...${NC}"
    
    # 统计今日调用
    local today=$(date +%Y%m%d)
    local today_logs="$LOG_DIR/agent-calls-${today}.log"
    
    if [[ -f "$today_logs" ]]; then
        TOTAL_CALLS=$(grep -c "AI调用请求" "$today_logs" || echo 0)
        SUCCESSFUL_CALLS=$(grep -c "AI调用完成" "$today_logs" || echo 0)
        FAILED_CALLS=$((TOTAL_CALLS - SUCCESSFUL_CALLS))
        
        echo "  • 今日总调用: $TOTAL_CALLS"
        echo "  • 成功调用: $SUCCESSFUL_CALLS"
        echo "  • 失败调用: $FAILED_CALLS"
    else
        echo "  • 今日暂无调用记录"
    fi
}

# 分析违规记录
analyze_violations() {
    echo -e "${YELLOW}⚠️ 分析违规记录...${NC}"
    
    if [[ -f "$VIOLATIONS_FILE" ]]; then
        # 跳过CSV头
        VIOLATIONS_COUNT=$(tail -n +2 "$VIOLATIONS_FILE" | wc -l)
        
        if [[ $VIOLATIONS_COUNT -gt 0 ]]; then
            echo "  • 本月违规次数: $VIOLATIONS_COUNT"
            
            # 统计违规类型
            echo "  • 违规类型分布:"
            tail -n +2 "$VIOLATIONS_FILE" | cut -d',' -f3 | sort | uniq -c | while read count type; do
                echo "    - $type: $count次"
            done
        else
            echo "  • 本月无违规记录 ✅"
        fi
    else
        echo "  • 暂无违规记录文件"
    fi
}

# 分析调用质量
analyze_quality() {
    echo -e "${GREEN}📈 分析调用质量...${NC}"
    
    local report_files=$(find "$LOG_DIR" -name "call-report-*.json" -mtime -1 2>/dev/null)
    
    if [[ -n "$report_files" ]]; then
        local total_quality=0
        local count=0
        
        for report in $report_files; do
            # 简单判断：有context_verified的认为质量较高
            if grep -q '"context_verified": true' "$report"; then
                total_quality=$((total_quality + 100))
            else
                total_quality=$((total_quality + 50))
            fi
            count=$((count + 1))
        done
        
        if [[ $count -gt 0 ]]; then
            local avg_quality=$((total_quality / count))
            echo "  • 平均质量分数: ${avg_quality}%"
            
            if [[ $avg_quality -ge 80 ]]; then
                echo -e "  • 质量评级: ${GREEN}优秀 ⭐⭐⭐⭐⭐${NC}"
            elif [[ $avg_quality -ge 60 ]]; then
                echo -e "  • 质量评级: ${YELLOW}良好 ⭐⭐⭐⭐${NC}"
            else
                echo -e "  • 质量评级: ${RED}需改进 ⭐⭐${NC}"
            fi
        fi
    else
        echo "  • 暂无质量数据"
    fi
}

# 生成培训建议
generate_training_suggestions() {
    echo -e "${MAGENTA}📚 培训建议...${NC}"
    
    local suggestions=()
    
    # 基于违规次数
    if [[ $VIOLATIONS_COUNT -gt 5 ]]; then
        suggestions+=("🔴 强制培训: 代码上下文传递最佳实践")
    elif [[ $VIOLATIONS_COUNT -gt 2 ]]; then
        suggestions+=("🟡 建议培训: AI协作规范复习")
    fi
    
    # 基于失败率
    if [[ $TOTAL_CALLS -gt 0 ]]; then
        local failure_rate=$((FAILED_CALLS * 100 / TOTAL_CALLS))
        if [[ $failure_rate -gt 20 ]]; then
            suggestions+=("🔴 强制培训: AI调用故障排除")
        fi
    fi
    
    # 输出建议
    if [[ ${#suggestions[@]} -gt 0 ]]; then
        for suggestion in "${suggestions[@]}"; do
            echo "  • $suggestion"
        done
    else
        echo -e "  • ${GREEN}无需培训，表现优秀! ✅${NC}"
    fi
}

# 生成详细报告
generate_detailed_report() {
    local report_file="$REPORT_DIR/agent-monitor-$(date +%Y%m%d_%H%M%S).md"
    
    echo -e "${BLUE}📝 生成详细报告...${NC}"
    
    cat > "$report_file" << EOF
# 智能体AI协作行为监控报告

生成时间: $(date '+%Y-%m-%d %H:%M:%S')

## 📊 调用统计

| 指标 | 数值 |
|------|------|
| 总调用次数 | $TOTAL_CALLS |
| 成功调用 | $SUCCESSFUL_CALLS |
| 失败调用 | $FAILED_CALLS |
| 成功率 | $([ $TOTAL_CALLS -gt 0 ] && echo "$((SUCCESSFUL_CALLS * 100 / TOTAL_CALLS))%" || echo "N/A") |

## ⚠️ 违规记录

- 本月违规总数: $VIOLATIONS_COUNT
- 需要关注: $([ $VIOLATIONS_COUNT -gt 3 ] && echo "是 🔴" || echo "否 ✅")

## 📈 质量评估

基于最近24小时的调用分析：
- 代码上下文完整性: $([ $VIOLATIONS_COUNT -eq 0 ] && echo "100%" || echo "$((100 - VIOLATIONS_COUNT * 10))%")
- 调用规范遵守度: $([ $FAILED_CALLS -eq 0 ] && echo "100%" || echo "$((SUCCESSFUL_CALLS * 100 / TOTAL_CALLS))%")

## 🎯 改进建议

EOF
    
    # 添加具体建议
    if [[ $VIOLATIONS_COUNT -gt 0 ]]; then
        cat >> "$report_file" << EOF
### 代码上下文传递改进

1. **强制要求**: 每次调用AI前必须使用 \`ai-context-collector.sh\` 收集完整上下文
2. **检查清单**: 确保包含主文件、相关Service、DTO、配置文件
3. **自动化**: 使用 \`ai-agent-wrapper.sh\` 自动拦截和验证

EOF
    fi
    
    if [[ $FAILED_CALLS -gt 0 ]]; then
        cat >> "$report_file" << EOF
### 调用稳定性改进

1. **错误处理**: 增加重试机制和错误恢复
2. **参数验证**: 调用前验证所有必需参数
3. **日志记录**: 详细记录失败原因便于分析

EOF
    fi
    
    cat >> "$report_file" << EOF
## 📚 培训计划

EOF
    
    if [[ $VIOLATIONS_COUNT -gt 5 ]]; then
        cat >> "$report_file" << EOF
### 🔴 强制培训项目

1. **代码上下文传递铁律** (2小时)
   - 完整文件收集方法
   - 依赖关系分析
   - 上下文组织最佳实践

2. **AI协作质量标准** (1小时)
   - 质量检查清单
   - 常见错误案例
   - 改进措施

EOF
    elif [[ $VIOLATIONS_COUNT -gt 0 ]]; then
        cat >> "$report_file" << EOF
### 🟡 建议培训项目

1. **AI协作规范复习** (30分钟)
   - 快速回顾核心规则
   - 常见问题解答

EOF
    else
        cat >> "$report_file" << EOF
### ✅ 无需培训

智能体表现优秀，继续保持!

EOF
    fi
    
    echo "  • 报告已生成: $report_file"
}

# 显示仪表板
show_dashboard() {
    echo ""
    echo -e "${CYAN}┌────────────────── 实时监控仪表板 ──────────────────┐${NC}"
    
    # 健康状态指示器
    local health_status="🟢 健康"
    if [[ $VIOLATIONS_COUNT -gt 5 ]]; then
        health_status="🔴 警告"
    elif [[ $VIOLATIONS_COUNT -gt 2 ]]; then
        health_status="🟡 注意"
    fi
    
    echo -e "│ 系统状态: $health_status                                    │"
    echo -e "│                                                      │"
    
    # 调用统计条形图
    echo -e "│ 调用统计:                                           │"
    
    local bar_length=30
    if [[ $TOTAL_CALLS -gt 0 ]]; then
        local success_bars=$((SUCCESSFUL_CALLS * bar_length / TOTAL_CALLS))
        local fail_bars=$((FAILED_CALLS * bar_length / TOTAL_CALLS))
        
        printf "│ 成功 ["
        printf "${GREEN}%${success_bars}s${NC}" | tr ' ' '█'
        printf "%$((bar_length - success_bars))s] %3d%%     │\n" "" $((SUCCESSFUL_CALLS * 100 / TOTAL_CALLS))
        
        printf "│ 失败 ["
        printf "${RED}%${fail_bars}s${NC}" | tr ' ' '█'
        printf "%$((bar_length - fail_bars))s] %3d%%     │\n" "" $((FAILED_CALLS * 100 / TOTAL_CALLS))
    else
        echo "│ [暂无数据]                                          │"
    fi
    
    echo -e "│                                                      │"
    
    # 违规趋势
    echo -e "│ 违规趋势: $([ $VIOLATIONS_COUNT -eq 0 ] && echo "📉 无违规" || echo "📈 $VIOLATIONS_COUNT 次")                                │"
    
    echo -e "${CYAN}└──────────────────────────────────────────────────────┘${NC}"
}

# 实时监控模式
realtime_monitor() {
    while true; do
        clear
        show_header
        analyze_call_logs
        echo ""
        analyze_violations
        echo ""
        analyze_quality
        echo ""
        generate_training_suggestions
        show_dashboard
        
        echo ""
        echo -e "${CYAN}按 Ctrl+C 退出实时监控，每10秒自动刷新...${NC}"
        sleep 10
    done
}

# 主函数
main() {
    local mode="${1:-report}"
    
    show_header
    
    case "$mode" in
        report)
            # 生成报告模式
            analyze_call_logs
            echo ""
            analyze_violations
            echo ""
            analyze_quality
            echo ""
            generate_training_suggestions
            echo ""
            generate_detailed_report
            show_dashboard
            ;;
        
        monitor)
            # 实时监控模式
            realtime_monitor
            ;;
        
        clean)
            # 清理旧日志
            echo "清理30天前的日志..."
            find "$LOG_DIR" -name "*.log" -mtime +30 -delete
            find "$REPORT_DIR" -name "*.md" -mtime +30 -delete
            echo "清理完成 ✅"
            ;;
        
        *)
            echo "用法: $0 [report|monitor|clean]"
            echo "  report  - 生成监控报告（默认）"
            echo "  monitor - 实时监控模式"
            echo "  clean   - 清理旧日志"
            exit 1
            ;;
    esac
}

# 执行
main "$@"