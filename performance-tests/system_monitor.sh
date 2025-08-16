#!/bin/bash
# =======================================================
# 智能通知系统性能监控脚本
# 实时监控系统资源使用情况
# =======================================================

# Windows环境下的系统监控
monitor_system() {
    local output_file="$1"
    local duration_minutes="$2"
    local interval_seconds="$3"
    
    echo "开始监控系统性能，持续时间: ${duration_minutes}分钟"
    echo "监控间隔: ${interval_seconds}秒"
    echo "结果保存至: ${output_file}"
    
    # 创建CSV头部
    echo "Timestamp,CPU_Usage(%),Memory_Usage_MB,Java_Processes,Port_48081_Connections,Port_48082_Connections" > "$output_file"
    
    local end_time=$(($(date +%s) + duration_minutes * 60))
    
    while [ $(date +%s) -lt $end_time ]; do
        local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
        
        # CPU使用率 (Windows)
        local cpu_usage=$(wmic cpu get loadpercentage /value 2>nul | grep "LoadPercentage" | cut -d'=' -f2 | tr -d '\r\n')
        [ -z "$cpu_usage" ] && cpu_usage="N/A"
        
        # 内存使用 (Windows - 获取可用内存，然后计算使用量)
        local total_mem=$(wmic computersystem get TotalPhysicalMemory /value 2>nul | grep "TotalPhysicalMemory" | cut -d'=' -f2 | tr -d '\r\n')
        local avail_mem=$(wmic OS get AvailablePhysicalMemory /value 2>nul | grep "AvailablePhysicalMemory" | cut -d'=' -f2 | tr -d '\r\n')
        if [ -n "$total_mem" ] && [ -n "$avail_mem" ]; then
            local used_mem=$(( (total_mem - avail_mem) / 1024 / 1024 ))
        else
            local used_mem="N/A"
        fi
        
        # Java进程数
        local java_procs=$(tasklist /FI "IMAGENAME eq java.exe" 2>nul | find /c "java.exe")
        
        # 端口连接数 (Windows)
        local port_48081_conns=$(netstat -an | findstr :48081 | find /c "ESTABLISHED" 2>nul)
        local port_48082_conns=$(netstat -an | findstr :48082 | find /c "ESTABLISHED" 2>nul)
        
        # 写入CSV
        echo "${timestamp},${cpu_usage},${used_mem},${java_procs},${port_48081_conns},${port_48082_conns}" >> "$output_file"
        
        echo "$(date '+%H:%M:%S') - CPU: ${cpu_usage}%, RAM: ${used_mem}MB, Java进程: ${java_procs}, 48081连接: ${port_48081_conns}, 48082连接: ${port_48082_conns}"
        
        sleep $interval_seconds
    done
    
    echo "监控完成，数据已保存至: ${output_file}"
}

# 使用方法示例
if [ "$#" -eq 3 ]; then
    monitor_system "$1" "$2" "$3"
else
    echo "用法: $0 <输出文件> <持续时间(分钟)> <监控间隔(秒)>"
    echo "示例: $0 performance_monitor.csv 10 5"
fi