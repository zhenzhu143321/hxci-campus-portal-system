#!/bin/bash

# Gemini API Key 智能轮转管理脚本 (4个可用KEY版本 - 优化版)
# 每个KEY每天50次限制 + 每次调用后自动轮换避免速率限制
# 总计每天可用: 200次 (仅使用测试通过的KEY)

# API Keys 配置 (4个可用KEY)
declare -a API_KEYS=(
    "AIzaSyCH3H-ZY9nl_sYoggHQ4BPEegoz1ngwbsQ"  # KEY1 (可用)
    "AIzaSyDfjAlxYMgWJ6oH4W0iv1yTuu0oG0JkhpY"  # KEY2 (可用)
    "AIzaSyDxOUy1wlWONUbOgeQ4BcHvhm7HyjU1p8Q"  # KEY3 (可用)
    "AIzaSyDFikHJ6ieA_lVgbNgBra5Jy3BzWsvyi9I"  # KEY12 (可用)
)

# KEY标识符 (对应原来的编号)
declare -a KEY_IDS=(
    "KEY1"
    "KEY2" 
    "KEY3"
    "KEY12"
)

# 配置文件路径
CONFIG_DIR="$HOME/.gemini"
CURRENT_KEY_FILE="$CONFIG_DIR/current_key_index_4key"
USAGE_LOG_FILE="$CONFIG_DIR/usage_log_4key.txt"
DAILY_COUNTER_DIR="$CONFIG_DIR/daily_counters_4key"
ROTATION_MODE_FILE="$CONFIG_DIR/rotation_mode_4key"

# 轮换模式配置
DAILY_LIMIT=50   # 每个KEY每天50次
ROTATION_ENABLED=true  # 默认启用轮换模式

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m'

# 初始化配置
init_config() {
    mkdir -p "$CONFIG_DIR" "$DAILY_COUNTER_DIR"
    
    local today=$(date +%Y-%m-%d)
    
    # 初始化每日计数器文件
    for ((i=0; i<${#API_KEYS[@]}; i++)); do
        local counter_file="$DAILY_COUNTER_DIR/key_${i}_${today}.txt"
        if [[ ! -f "$counter_file" ]]; then
            echo "0" > "$counter_file"
        fi
    done
    
    # 清理昨天的计数器文件
    find "$DAILY_COUNTER_DIR" -name "key_*_*.txt" ! -name "*_${today}.txt" -delete 2>/dev/null
    
    # 初始化当前KEY文件
    if [[ ! -f "$CURRENT_KEY_FILE" ]]; then
        echo "0" > "$CURRENT_KEY_FILE"
    fi
    
    # 初始化轮换模式设置
    if [[ ! -f "$ROTATION_MODE_FILE" ]]; then
        echo "enabled" > "$ROTATION_MODE_FILE"
    fi
}

# 获取轮换模式状态
is_rotation_enabled() {
    if [[ -f "$ROTATION_MODE_FILE" ]]; then
        local mode=$(cat "$ROTATION_MODE_FILE")
        [[ "$mode" == "enabled" ]]
    else
        true  # 默认启用
    fi
}

# 设置轮换模式
set_rotation_mode() {
    local mode=$1
    if [[ "$mode" == "on" || "$mode" == "enabled" ]]; then
        echo "enabled" > "$ROTATION_MODE_FILE"
        echo -e "${GREEN}✅ 轮换模式已启用 (每次调用后自动轮换KEY)${NC}"
    elif [[ "$mode" == "off" || "$mode" == "disabled" ]]; then
        echo "disabled" > "$ROTATION_MODE_FILE"
        echo -e "${YELLOW}⚠️  轮换模式已禁用 (保持当前KEY直到用完)${NC}"
    else
        echo -e "${RED}❌ 无效模式，使用: on/off 或 enabled/disabled${NC}"
    fi
}

# 获取KEY的今日使用次数
get_daily_usage() {
    local key_index=$1
    local today=$(date +%Y-%m-%d)
    local counter_file="$DAILY_COUNTER_DIR/key_${key_index}_${today}.txt"
    if [[ -f "$counter_file" ]]; then
        cat "$counter_file"
    else
        echo "0"
    fi
}

# 增加KEY的今日使用次数
increment_daily_usage() {
    local key_index=$1
    local today=$(date +%Y-%m-%d)
    local counter_file="$DAILY_COUNTER_DIR/key_${key_index}_${today}.txt"
    local current_usage=$(get_daily_usage "$key_index")
    echo $((current_usage + 1)) > "$counter_file"
}

# 检测KEY是否可用
test_key_availability() {
    local key=$1
    local old_key="$GEMINI_API_KEY"
    export GEMINI_API_KEY="$key"
    
    # 使用timeout确保不会无限等待
    if echo "test" | timeout 10s gemini --prompt "简短回复：OK" >/dev/null 2>&1; then
        export GEMINI_API_KEY="$old_key"
        return 0  # 可用
    else
        export GEMINI_API_KEY="$old_key"
        return 1  # 不可用
    fi
}

# 智能选择最佳KEY
smart_select_key() {
    local best_key_index=0
    local min_usage=$DAILY_LIMIT
    
    # 遍历所有KEY，选择使用次数最少的可用KEY
    for ((i=0; i<${#API_KEYS[@]}; i++)); do
        local usage=$(get_daily_usage "$i")
        
        # 如果这个KEY今天还没用完，并且使用次数比当前最少的还要少
        if [[ $usage -lt $DAILY_LIMIT ]] && [[ $usage -lt $min_usage ]]; then
            # 快速测试KEY可用性
            if test_key_availability "${API_KEYS[$i]}"; then
                best_key_index=$i
                min_usage=$usage
            fi
        fi
    done
    
    # 如果找到了可用的KEY
    if [[ $min_usage -lt $DAILY_LIMIT ]]; then
        echo "$best_key_index"
        return 0
    else
        return 1  # 所有KEY都用完了
    fi
}

# 轮换到下一个可用KEY
rotate_to_next_key() {
    echo -e "${CYAN}🔄 轮换到下一个可用KEY...${NC}"
    
    local current_index=$(cat "$CURRENT_KEY_FILE")
    local next_index=$((current_index + 1))
    
    # 从下一个KEY开始，找到第一个可用的KEY
    for ((i=0; i<${#API_KEYS[@]}; i++)); do
        local try_index=$(( (next_index + i) % ${#API_KEYS[@]} ))
        local usage=$(get_daily_usage "$try_index")
        
        if [[ $usage -lt $DAILY_LIMIT ]]; then
            echo -e "${BLUE}🎯 尝试 ${KEY_IDS[$try_index]} (已用: $usage/50)${NC}"
            if test_key_availability "${API_KEYS[$try_index]}"; then
                echo "$try_index" > "$CURRENT_KEY_FILE"
                export GEMINI_API_KEY="${API_KEYS[$try_index]}"
                echo 'export GEMINI_API_KEY="'${API_KEYS[$try_index]}'"' > ~/.bashrc_gemini_4key
                echo -e "${GREEN}✅ 轮换到 ${KEY_IDS[$try_index]}${NC}"
                return 0
            else
                echo -e "${RED}❌ ${KEY_IDS[$try_index]} 不可用${NC}"
            fi
        fi
    done
    
    echo -e "${RED}❌ 所有KEY都已达到今日限制或不可用${NC}"
    return 1
}

# 显示状态信息
show_status() {
    init_config
    
    echo -e "${BLUE}=================== Gemini 4-KEY 智能轮换系统 ===================${NC}"
    
    local current_index=$(cat "$CURRENT_KEY_FILE")
    local current_key_id="${KEY_IDS[$current_index]}"
    local current_key="${API_KEYS[$current_index]}"
    
    echo -e "${GREEN}当前使用: $current_key_id - ${current_key:0:15}***${NC}"
    
    if is_rotation_enabled; then
        echo -e "${PURPLE}轮换模式: ${GREEN}启用${NC} (每次调用后自动轮换)"
    else
        echo -e "${PURPLE}轮换模式: ${YELLOW}禁用${NC} (保持当前KEY直到用完)"
    fi
    
    echo -e "${YELLOW}日期: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
    echo ""
    
    echo -e "${PURPLE}📊 各KEY使用情况 (每KEY限制: 50次/天):${NC}"
    echo -e "${BLUE}KEY编号  今日使用  剩余次数  使用率    状态${NC}"
    echo "─────────────────────────────────────────────────────"
    
    local total_used=0
    local available_keys=0
    
    for ((i=0; i<${#API_KEYS[@]}; i++)); do
        local usage=$(get_daily_usage "$i")
        local remaining=$((DAILY_LIMIT - usage))
        local usage_percent=$((usage * 100 / DAILY_LIMIT))
        local key_id="${KEY_IDS[$i]}"
        
        total_used=$((total_used + usage))
        
        local status_color="${GREEN}"
        local status_text="可用"
        
        if [[ $usage -ge $DAILY_LIMIT ]]; then
            status_color="${RED}"
            status_text="已满"
        else
            available_keys=$((available_keys + 1))
            if [[ $i -eq $current_index ]]; then
                status_color="${CYAN}"
                status_text="可用 (当前)"
            fi
        fi
        
        printf "${status_color}%-8s %-9s %-9s %-6s%%  %s${NC}\n" \
            "$key_id" "$usage" "$remaining" "$usage_percent" "$status_text"
    done
    
    echo "─────────────────────────────────────────────────────"
    local total_available=$((${#API_KEYS[@]} * DAILY_LIMIT))
    local total_remaining=$((total_available - total_used))
    local total_usage_percent=$((total_used * 100 / total_available))
    
    echo -e "${CYAN}📈 总计使用: $total_used/$total_available ($total_usage_percent%)${NC}"
    echo -e "${CYAN}🎯 剩余可用: $total_remaining 次${NC}"
    echo -e "${CYAN}🟢 可用KEY数: $available_keys/${#API_KEYS[@]}${NC}"
    
    echo ""
    echo -e "${GREEN}🌟 4-KEY轮换模式优势:${NC}"
    echo -e "${GREEN}  • 仅使用测试通过的可用KEY${NC}"
    echo -e "${GREEN}  • 无速率限制烦恼 (每次调用都换KEY)${NC}"
    echo -e "${GREEN}  • 最大化利用所有可用KEY${NC}"
    echo -e "${GREEN}  • 自动负载均衡${NC}"
    
    if [[ -f "$USAGE_LOG_FILE" ]]; then
        echo ""
        echo -e "${PURPLE}📝 最近使用记录:${NC}"
        tail -5 "$USAGE_LOG_FILE" | while read line; do
            echo -e "${GRAY}  $line${NC}"
        done
    fi
}

# 执行调用
execute_call() {
    local prompt="$1"
    
    init_config
    
    local current_index=$(cat "$CURRENT_KEY_FILE")
    local current_usage=$(get_daily_usage "$current_index")
    local current_key_id="${KEY_IDS[$current_index]}"
    
    # 检查当前KEY是否已达到限制
    if [[ $current_usage -ge $DAILY_LIMIT ]]; then
        echo -e "${YELLOW}⚠️  当前KEY已达到今日限制，自动选择其他KEY...${NC}"
        if ! rotate_to_next_key; then
            echo -e "${RED}❌ 所有KEY都已达到今日限制${NC}"
            return 1
        fi
        current_index=$(cat "$CURRENT_KEY_FILE")
        current_key_id="${KEY_IDS[$current_index]}"
    fi
    
    echo -e "${CYAN}🚀 执行调用... (使用 $current_key_id)${NC}"
    
    # 记录调用
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] 调用开始 $current_key_id (今日: $current_usage/50)" >> "$USAGE_LOG_FILE"
    
    # 执行实际调用
    export GEMINI_API_KEY="${API_KEYS[$current_index]}"
    local call_result
    if echo "" | gemini --prompt "$prompt"; then
        call_result="成功"
        # 增加使用次数
        increment_daily_usage "$current_index"
        local new_usage=$(get_daily_usage "$current_index")
        echo "[$timestamp] 调用完成 $current_key_id (今日: $new_usage/50)" >> "$USAGE_LOG_FILE"
    else
        call_result="失败"
        echo "[$timestamp] 调用失败 $current_key_id" >> "$USAGE_LOG_FILE"
    fi
    
    # 如果启用了轮换模式，调用后自动轮换
    if is_rotation_enabled && [[ "$call_result" == "成功" ]]; then
        echo -e "${PURPLE}🔄 轮换模式已启用，自动轮换到下一个KEY...${NC}"
        rotate_to_next_key
    fi
}

# 切换到指定KEY
switch_to_key() {
    local target_key_id="$1"
    
    init_config
    
    # 查找对应的KEY索引
    local target_index=-1
    for ((i=0; i<${#KEY_IDS[@]}; i++)); do
        if [[ "${KEY_IDS[$i]}" == "$target_key_id" ]]; then
            target_index=$i
            break
        fi
    done
    
    if [[ $target_index -eq -1 ]]; then
        echo -e "${RED}❌ 无效的KEY ID: $target_key_id${NC}"
        echo -e "${CYAN}可用的KEY ID: ${KEY_IDS[*]}${NC}"
        return 1
    fi
    
    echo -e "${BLUE}🔄 切换到 $target_key_id...${NC}"
    
    local usage=$(get_daily_usage "$target_index")
    if [[ $usage -ge $DAILY_LIMIT ]]; then
        echo -e "${RED}❌ $target_key_id 今日已达到限制 ($usage/$DAILY_LIMIT)${NC}"
        echo -e "${YELLOW}🔄 尝试智能选择其他KEY...${NC}"
        if ! rotate_to_next_key; then
            return 1
        fi
        return 0
    fi
    
    # 测试KEY可用性
    if ! test_key_availability "${API_KEYS[$target_index]}"; then
        echo -e "${RED}❌ $target_key_id 不可用，尝试智能选择...${NC}"
        if ! rotate_to_next_key; then
            return 1
        fi
        return 0
    fi
    
    # 切换成功
    echo "$target_index" > "$CURRENT_KEY_FILE"
    export GEMINI_API_KEY="${API_KEYS[$target_index]}"
    echo 'export GEMINI_API_KEY="'${API_KEYS[$target_index]}'"' > ~/.bashrc_gemini_4key
    
    echo -e "${GREEN}✅ 已切换到 $target_key_id${NC}"
    echo -e "${GREEN}📊 今日使用: $usage/50${NC}"
}

# 重置今日计数器
reset_daily_counters() {
    local today=$(date +%Y-%m-%d)
    
    echo -e "${YELLOW}🧹 重置今日计数器...${NC}"
    
    for ((i=0; i<${#API_KEYS[@]}; i++)); do
        local counter_file="$DAILY_COUNTER_DIR/key_${i}_${today}.txt"
        echo "0" > "$counter_file"
        echo -e "${GREEN}  重置 ${KEY_IDS[$i]} 计数器${NC}"
    done
    
    echo -e "${GREEN}✅ 所有计数器已重置${NC}"
}

# 测试轮换功能
test_rotation() {
    local test_count=${1:-4}  # 默认测试4次，每个KEY一次
    
    echo -e "${CYAN}🧪 连续调用测试 (${test_count}次) - 观察轮换效果${NC}"
    
    for ((i=1; i<=test_count; i++)); do
        echo -e "${BLUE}===== 第 $i 次调用 =====${NC}"
        execute_call "简短回复：测试$i"
        echo ""
        sleep 2  # 短暂延迟避免过于频繁
    done
    
    echo -e "${GREEN}🎉 测试完成！${NC}"
    show_status
}

# 显示帮助信息
show_help() {
    echo -e "${BLUE}================ Gemini 4-KEY 智能轮换系统 ================${NC}"
    echo ""
    echo -e "${YELLOW}🌟 新版特性:${NC}"
    echo -e "${GREEN}  • 仅使用4个测试通过的可用KEY (KEY1, KEY2, KEY3, KEY12)${NC}"
    echo -e "${GREEN}  • 每次调用后自动轮换到下一个可用KEY${NC}"
    echo -e "${GREEN}  • 彻底避免速率限制问题 (5次/分钟)${NC}"
    echo -e "${GREEN}  • 最大化利用所有可用KEY (400次/天)${NC}"
    echo ""
    echo -e "${YELLOW}📋 使用方法:${NC}"
    echo "  $0 auto               - 智能自动选择最优KEY"
    echo "  $0 switch <KEY_ID>    - 快速切换到指定KEY (KEY1/KEY2/KEY3/KEY12)"
    echo "  $0 status             - 显示详细使用状态"
    echo "  $0 call \"prompt\"      - 安全执行调用 (支持轮换)"
    echo "  $0 rotation <on/off>  - 启用/禁用轮换模式"
    echo "  $0 test <次数>        - 连续调用测试轮换功能"
    echo "  $0 reset              - 重置今日计数器"
    echo "  $0 help               - 显示帮助"
    echo ""
    echo -e "${YELLOW}🔄 轮换模式说明:${NC}"
    echo -e "${GREEN}  启用时: 每次调用完成后自动轮换到下一个可用KEY${NC}"
    echo -e "${YELLOW}  禁用时: 保持使用当前KEY直到达到限制才切换${NC}"
    echo ""
    echo -e "${YELLOW}💡 推荐使用:${NC}"
    echo "  ./gemini-turbo-4key.sh rotation on        # 启用轮换模式"
    echo "  ./gemini-turbo-4key.sh call \"你的问题\"    # 自动轮换调用"
    echo "  ./gemini-turbo-4key.sh test 4             # 测试轮换4次"
    echo ""
    echo -e "${GREEN}💡 每日总额度: 4 × 50 = 200次调用${NC}"
    echo -e "${CYAN}🔄 轮换优势: 无速率限制，最大化利用率${NC}"
}

# 主程序逻辑
main() {
    case "${1:-help}" in
        "auto")
            init_config
            if smart_key_index=$(smart_select_key); then
                echo "$smart_key_index" > "$CURRENT_KEY_FILE"
                export GEMINI_API_KEY="${API_KEYS[$smart_key_index]}"
                echo 'export GEMINI_API_KEY="'${API_KEYS[$smart_key_index]}'"' > ~/.bashrc_gemini_4key
                echo -e "${GREEN}✅ 智能选择: ${KEY_IDS[$smart_key_index]}${NC}"
                show_status
            else
                echo -e "${RED}❌ 所有KEY都已达到今日限制或不可用${NC}"
            fi
            ;;
        "switch")
            switch_to_key "$2"
            ;;
        "status")
            show_status
            ;;
        "call")
            execute_call "$2"
            ;;
        "rotation")
            set_rotation_mode "$2"
            ;;
        "test")
            test_rotation "$2"
            ;;
        "reset")
            reset_daily_counters
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# 运行主程序
main "$@"