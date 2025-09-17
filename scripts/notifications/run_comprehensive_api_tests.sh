#!/bin/bash

# 全面后端API测试主控脚本
# 基于Sequential Thinking深度分析制定的全面测试计划
# 整合现有scripts/notifications脚本 + 新增补充测试脚本

echo "🚀 哈尔滨信息工程学院校园门户系统"
echo "全面后端API测试执行器 v1.0"
echo "======================================================="
echo "测试范围: 主通知服务(48081) + Mock School API(48082)"
echo "测试重点: 待办通知组件优化 + 认证系统安全修复验证"
echo "======================================================="

# 脚本位置和配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/../../documentation/test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 创建日志目录
mkdir -p "$LOG_DIR"

# 测试统计
TOTAL_PHASES=0
PASSED_PHASES=0
FAILED_PHASES=0

# 日志文件
MAIN_LOG="$LOG_DIR/comprehensive_test_${TIMESTAMP}.log"

# 记录阶段结果函数
record_phase_result() {
    local result=$1
    local phase_name=$2
    local phase_log=$3

    TOTAL_PHASES=$((TOTAL_PHASES + 1))

    echo "----------------------------------------" | tee -a "$MAIN_LOG"
    if [ "$result" = "0" ]; then
        PASSED_PHASES=$((PASSED_PHASES + 1))
        echo "✅ $phase_name - 测试通过" | tee -a "$MAIN_LOG"
        echo "📄 详细日志: $phase_log" | tee -a "$MAIN_LOG"
    else
        FAILED_PHASES=$((FAILED_PHASES + 1))
        echo "❌ $phase_name - 测试失败 (退出码: $result)" | tee -a "$MAIN_LOG"
        echo "📄 详细日志: $phase_log" | tee -a "$MAIN_LOG"
    fi
}

# 执行测试阶段函数
execute_test_phase() {
    local script_name=$1
    local phase_name=$2
    local phase_description=$3
    local is_required=$4  # true/false - 是否为必需的阻塞测试

    echo "" | tee -a "$MAIN_LOG"
    echo "🎯 执行阶段: $phase_name" | tee -a "$MAIN_LOG"
    echo "📝 描述: $phase_description" | tee -a "$MAIN_LOG"
    echo "📍 脚本: $script_name" | tee -a "$MAIN_LOG"

    local phase_log="$LOG_DIR/${phase_name// /_}_${TIMESTAMP}.log"

    if [ -f "$SCRIPT_DIR/$script_name" ]; then
        echo "⚡ 开始执行..." | tee -a "$MAIN_LOG"
        cd "$SCRIPT_DIR"

        # 执行测试脚本并记录输出
        if ./"$script_name" > "$phase_log" 2>&1; then
            record_phase_result 0 "$phase_name" "$phase_log"
            return 0
        else
            local exit_code=$?
            record_phase_result "$exit_code" "$phase_name" "$phase_log"

            if [ "$is_required" = "true" ]; then
                echo "🚨 关键测试失败，停止后续测试执行" | tee -a "$MAIN_LOG"
                echo "请先解决P0级基础问题后再继续" | tee -a "$MAIN_LOG"
                exit $exit_code
            fi
            return $exit_code
        fi
    else
        echo "⚠️ 测试脚本不存在: $script_name" | tee -a "$MAIN_LOG"
        record_phase_result 1 "$phase_name" "脚本文件缺失"
        return 1
    fi
}

# 显示测试选项菜单
show_test_menu() {
    echo ""
    echo "🎛️ 测试执行选项:"
    echo "1. 完整测试 (推荐) - 执行所有P0-P4级别测试"
    echo "2. 快速验证 - 仅执行P0+P1核心功能测试"
    echo "3. P0基础测试 - 仅执行服务健康检查"
    echo "4. P1功能测试 - 仅执行核心业务功能测试"
    echo "5. 待办优化专项测试 - 仅测试新实现的待办通知功能"
    echo "6. 权限系统专项测试 - 仅测试角色权限矩阵"
    echo "0. 退出"
    echo ""
    read -p "请选择测试模式 (1-6, 0退出): " choice
}

# P0级基础测试执行函数
execute_p0_tests() {
    echo ""
    echo "🔥 ===== P0级: 基础连通性测试 (阻塞性) ====="
    echo "必须100%通过才能继续后续测试"

    execute_test_phase "P0_service_health_check.sh" "P0.1 服务健康检查" "端口连通性和API响应时间验证" "true"
    execute_test_phase "api_test_fixed.sh" "P0.2 认证流程验证" "JWT+CSRF双重认证完整性测试" "true"
}

# P1级功能测试执行函数
execute_p1_tests() {
    echo ""
    echo "🎯 ===== P1级: 权限和业务逻辑测试 (核心功能) ====="

    execute_test_phase "test_roles.sh" "P1.1 权限矩阵验证" "6种角色权限边界完整性测试" "false"
    execute_test_phase "publish_level1_emergency.sh" "P1.2 Level1紧急通知" "校长发布紧急通知流程测试" "false"
    execute_test_phase "publish_level2_important.sh" "P1.3 Level2重要通知" "重要通知发布和权限验证" "false"
    execute_test_phase "publish_level3_regular.sh" "P1.4 Level3常规通知" "常规通知发布流程测试" "false"
    execute_test_phase "publish_level4_complete_fixed.sh" "P1.5 Level4提醒通知" "提醒通知完整功能测试" "false"
    execute_test_phase "publish_todo_notification.sh" "P1.6 待办通知优化" "待办通知组件优化功能验证" "false"
}

# 待办优化专项测试
execute_todo_optimization_tests() {
    echo ""
    echo "📝 ===== 待办通知组件优化专项测试 ====="

    execute_test_phase "publish_todo_notification.sh" "待办通知发布" "待办通知核心发布功能测试" "false"

    # 如果存在待办相关的其他测试脚本，也在这里执行
    if [ -f "$SCRIPT_DIR/P1_todo_optimization_test.sh" ]; then
        execute_test_phase "P1_todo_optimization_test.sh" "待办状态隔离" "用户状态隔离和乐观锁机制测试" "false"
    else
        echo "⚠️ 待办优化深度测试脚本还未创建 (P1_todo_optimization_test.sh)" | tee -a "$MAIN_LOG"
    fi
}

# P2级性能测试执行函数
execute_p2_tests() {
    echo ""
    echo "⚡ ===== P2级: 性能和集成测试 (稳定性) ====="

    # 使用缓存清理工具测试权限缓存性能
    if [ -f "$SCRIPT_DIR/cache_clear_utils.sh" ]; then
        echo "🗄️ 执行权限缓存性能测试..." | tee -a "$MAIN_LOG"
        source "$SCRIPT_DIR/cache_clear_utils.sh"
        if test_cache_performance > "$LOG_DIR/P2_cache_performance_${TIMESTAMP}.log" 2>&1; then
            record_phase_result 0 "P2.1 权限缓存性能" "$LOG_DIR/P2_cache_performance_${TIMESTAMP}.log"
        else
            record_phase_result 1 "P2.1 权限缓存性能" "$LOG_DIR/P2_cache_performance_${TIMESTAMP}.log"
        fi
    fi

    # 如果存在并发测试脚本，执行并发测试
    if [ -f "$SCRIPT_DIR/P2_concurrent_load_test.sh" ]; then
        execute_test_phase "P2_concurrent_load_test.sh" "P2.2 并发压力测试" "5000+ QPS并发处理能力验证" "false"
    else
        echo "⚠️ 并发压力测试脚本还未创建 (P2_concurrent_load_test.sh)" | tee -a "$MAIN_LOG"
    fi
}

# 生成测试摘要报告
generate_test_summary() {
    echo ""
    echo "📊 ================= 测试执行摘要 ==================" | tee -a "$MAIN_LOG"
    echo "测试开始时间: $(date -d "@$(stat -c %Y "$MAIN_LOG" 2>/dev/null || echo $(date +%s))" +"%Y-%m-%d %H:%M:%S" 2>/dev/null || date +"%Y-%m-%d %H:%M:%S")" | tee -a "$MAIN_LOG"
    echo "测试结束时间: $(date +"%Y-%m-%d %H:%M:%S")" | tee -a "$MAIN_LOG"
    echo "总测试阶段: $TOTAL_PHASES" | tee -a "$MAIN_LOG"
    echo "通过阶段: $PASSED_PHASES" | tee -a "$MAIN_LOG"
    echo "失败阶段: $FAILED_PHASES" | tee -a "$MAIN_LOG"

    if [ "$TOTAL_PHASES" -gt 0 ]; then
        SUCCESS_RATE=$(( (PASSED_PHASES * 100) / TOTAL_PHASES ))
        echo "成功率: ${SUCCESS_RATE}%" | tee -a "$MAIN_LOG"
    fi

    echo ""
    echo "📄 详细日志位置: $LOG_DIR" | tee -a "$MAIN_LOG"
    echo "📄 主日志文件: $MAIN_LOG" | tee -a "$MAIN_LOG"

    echo ""
    if [ "$FAILED_PHASES" -eq 0 ]; then
        echo "🎉 恭喜！所有测试阶段都已通过" | tee -a "$MAIN_LOG"
        echo "✅ 校园通知系统API功能完整，性能良好" | tee -a "$MAIN_LOG"
    else
        echo "⚠️ 发现了 $FAILED_PHASES 个问题需要处理" | tee -a "$MAIN_LOG"
        echo "🔧 请查看具体的测试日志进行问题排查" | tee -a "$MAIN_LOG"
    fi
}

# 主执行流程
main() {
    # 初始化日志文件
    echo "🚀 校园门户系统全面API测试开始 - $(date)" > "$MAIN_LOG"

    # 显示菜单并获取用户选择
    show_test_menu

    case $choice in
        1) # 完整测试
            echo "🎯 执行完整测试套件..." | tee -a "$MAIN_LOG"
            execute_p0_tests
            execute_p1_tests
            execute_p2_tests
            ;;
        2) # 快速验证
            echo "⚡ 执行快速验证测试..." | tee -a "$MAIN_LOG"
            execute_p0_tests
            execute_p1_tests
            ;;
        3) # P0基础测试
            echo "🔍 执行P0基础测试..." | tee -a "$MAIN_LOG"
            execute_p0_tests
            ;;
        4) # P1功能测试
            echo "🎯 执行P1功能测试..." | tee -a "$MAIN_LOG"
            execute_p1_tests
            ;;
        5) # 待办优化专项测试
            echo "📝 执行待办优化专项测试..." | tee -a "$MAIN_LOG"
            execute_todo_optimization_tests
            ;;
        6) # 权限系统专项测试
            echo "🛡️ 执行权限系统专项测试..." | tee -a "$MAIN_LOG"
            execute_test_phase "test_roles.sh" "权限矩阵验证" "6种角色权限边界完整性测试" "false"
            ;;
        0) # 退出
            echo "👋 退出测试执行器"
            exit 0
            ;;
        *) # 无效选择
            echo "❌ 无效选择，请重新运行脚本"
            exit 1
            ;;
    esac

    # 生成测试摘要
    generate_test_summary

    # 根据测试结果决定退出码
    if [ "$FAILED_PHASES" -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# 脚本入口点
main "$@"