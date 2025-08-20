#!/bin/bash

# 8080端口demo服务监控脚本
# 功能：监控nginx在8080端口的服务状态，自动重启异常服务
# 作者：哈尔滨信息工程学院校园门户系统
# 创建时间：2025-08-18

LOG_FILE="/var/log/campus-portal/8080-monitor.log"
SERVICE_URL="http://localhost:8080"
SERVICE_NAME="nginx demo-8080"

# 创建日志目录
sudo mkdir -p /var/log/campus-portal

# 日志函数
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | sudo tee -a "$LOG_FILE"
}

# 检查服务是否响应
check_service() {
    local http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "$SERVICE_URL")
    
    if [ "$http_code" = "200" ]; then
        return 0  # 服务正常
    else
        return 1  # 服务异常
    fi
}

# 检查nginx进程
check_nginx_process() {
    if pgrep -x "nginx" > /dev/null; then
        return 0  # nginx进程存在
    else
        return 1  # nginx进程不存在
    fi
}

# 检查8080端口监听
check_port_listen() {
    if sudo ss -tln | grep -q ":8080"; then
        return 0  # 端口正在监听
    else
        return 1  # 端口未监听
    fi
}

# 重启nginx服务
restart_nginx() {
    log_message "尝试重启nginx服务..."
    
    # 停止nginx
    sudo systemctl stop nginx
    sleep 2
    
    # 杀死残留进程
    sudo pkill -f nginx > /dev/null 2>&1
    sleep 1
    
    # 测试配置
    if sudo nginx -t > /dev/null 2>&1; then
        # 启动nginx
        sudo systemctl start nginx
        sleep 3
        
        if check_service; then
            log_message "nginx服务重启成功，8080端口恢复正常"
            return 0
        else
            log_message "nginx服务重启后仍无法访问8080端口"
            return 1
        fi
    else
        log_message "nginx配置文件有错误，无法重启"
        return 1
    fi
}

# 发送告警通知（可扩展）
send_alert() {
    local message="$1"
    log_message "告警: $message"
    
    # 这里可以添加邮件、短信等告警方式
    # echo "$message" | mail -s "8080服务告警" admin@example.com
}

# 主监控逻辑
main_monitor() {
    log_message "开始监控8080端口demo服务..."
    
    # 检查服务状态
    if check_service; then
        log_message "8080端口服务状态正常 (HTTP 200)"
        return 0
    fi
    
    log_message "8080端口服务异常，开始诊断..."
    
    # 检查nginx进程
    if ! check_nginx_process; then
        log_message "nginx进程不存在，尝试启动..."
        sudo systemctl start nginx
        sleep 3
    fi
    
    # 检查8080端口监听
    if ! check_port_listen; then
        log_message "8080端口未监听，检查nginx配置..."
        sudo systemctl reload nginx
        sleep 2
    fi
    
    # 再次检查服务
    if check_service; then
        log_message "服务已恢复正常"
        return 0
    fi
    
    # 服务仍异常，尝试完全重启
    log_message "常规修复失败，尝试完全重启nginx..."
    if restart_nginx; then
        return 0
    else
        send_alert "8080端口demo服务重启失败，需要人工介入"
        return 1
    fi
}

# 脚本参数处理
case "${1:-monitor}" in
    "monitor")
        main_monitor
        ;;
    "check")
        if check_service; then
            echo "✅ 8080端口服务正常"
            exit 0
        else
            echo "❌ 8080端口服务异常"
            exit 1
        fi
        ;;
    "restart")
        restart_nginx
        ;;
    "status")
        echo "=== 8080端口服务状态 ==="
        echo -n "HTTP响应: "
        if check_service; then echo "✅ 正常"; else echo "❌ 异常"; fi
        
        echo -n "nginx进程: "
        if check_nginx_process; then echo "✅ 运行中"; else echo "❌ 未运行"; fi
        
        echo -n "8080端口: "
        if check_port_listen; then echo "✅ 监听中"; else echo "❌ 未监听"; fi
        
        echo "最近日志:"
        tail -5 "$LOG_FILE" 2>/dev/null || echo "暂无日志"
        ;;
    *)
        echo "用法: $0 [monitor|check|restart|status]"
        echo "  monitor  - 监控并自动修复服务（默认）"
        echo "  check    - 仅检查服务状态"
        echo "  restart  - 强制重启nginx服务"
        echo "  status   - 显示详细状态信息"
        exit 1
        ;;
esac
