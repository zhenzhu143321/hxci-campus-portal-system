#!/bin/bash

# =============================================================================
# Shadowsocks 修复版一键代理搭建脚本
# 用途: 解决服务启动失败问题，确保代理正常工作
# 版本: v2.0 - 修复版
# 作者: Claude Code AI
# 创建时间: 2025-08-16
# =============================================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${CYAN}[DEBUG]${NC} $1"
}

# 显示横幅
show_banner() {
    echo -e "${GREEN}"
    echo "=================================================="
    echo "     Shadowsocks 修复版代理搭建脚本 v2.0"
    echo "     解决 club.claudecode.site 访问问题"
    echo "=================================================="
    echo -e "${NC}"
}

# 检查系统
check_system() {
    log_info "检查系统环境..."
    
    if [[ $EUID -ne 0 ]]; then
        log_error "请使用 root 权限运行此脚本"
        exit 1
    fi
    
    if ! command -v apt &> /dev/null; then
        log_error "此脚本仅支持 Ubuntu/Debian 系统"
        exit 1
    fi
    
    log_success "系统检查通过"
}

# 清理现有配置
cleanup_existing() {
    log_info "清理现有的Shadowsocks配置..."
    
    # 停止所有相关服务
    systemctl stop shadowsocks.service 2>/dev/null || true
    systemctl stop shadowsocks-libev.service 2>/dev/null || true
    
    # 禁用系统服务
    systemctl disable shadowsocks.service 2>/dev/null || true
    systemctl disable shadowsocks-libev.service 2>/dev/null || true
    
    # 杀死所有ss-local进程
    pkill -f ss-local 2>/dev/null || true
    
    # 等待进程完全退出
    sleep 3
    
    # 删除旧的服务文件
    rm -f /etc/systemd/system/shadowsocks.service
    
    # 重新加载systemd
    systemctl daemon-reload
    
    log_success "清理完成"
}

# 安装依赖
install_dependencies() {
    log_info "检查并安装依赖..."
    
    # 更新包列表
    apt update -y
    
    # 安装shadowsocks-libev（如果未安装）
    if ! command -v ss-local &> /dev/null; then
        log_info "安装 shadowsocks-libev..."
        apt install -y shadowsocks-libev
    else
        log_info "shadowsocks-libev 已安装"
    fi
    
    # 安装proxychains4（如果未安装）
    if ! command -v proxychains4 &> /dev/null; then
        log_info "安装 proxychains4..."
        apt install -y proxychains4
    else
        log_info "proxychains4 已安装"
    fi
    
    # 安装其他工具
    apt install -y curl wget vim net-tools jq
    
    log_success "依赖检查完成"
}

# 配置 Shadowsocks
configure_shadowsocks() {
    log_info "配置 Shadowsocks 客户端..."
    
    # 创建配置目录
    mkdir -p /etc/shadowsocks
    
    # 创建配置文件
    cat > /etc/shadowsocks/config.json << 'EOF'
{
    "server": "8.216.35.163",
    "server_port": 8388,
    "local_address": "127.0.0.1",
    "local_port": 1080,
    "password": "143321",
    "method": "chacha20-ietf-poly1305",
    "timeout": 300,
    "fast_open": false,
    "reuse_port": true
}
EOF
    
    # 验证JSON格式
    if python3 -m json.tool /etc/shadowsocks/config.json > /dev/null 2>&1; then
        log_success "Shadowsocks 配置文件创建成功"
    else
        log_error "配置文件JSON格式错误"
        exit 1
    fi
}

# 手动启动方式（替代systemd）
setup_manual_start() {
    log_info "设置手动启动方式..."
    
    # 创建启动脚本
    cat > /usr/local/bin/ss-start << 'EOF'
#!/bin/bash

# Shadowsocks 启动脚本
CONFIG_FILE="/etc/shadowsocks/config.json"
PID_FILE="/var/run/shadowsocks.pid"
LOG_FILE="/var/log/shadowsocks.log"

case "$1" in
    start)
        echo "启动 Shadowsocks..."
        if [ -f "$PID_FILE" ]; then
            PID=$(cat $PID_FILE)
            if ps -p $PID > /dev/null 2>&1; then
                echo "Shadowsocks 已在运行 (PID: $PID)"
                exit 0
            else
                rm -f $PID_FILE
            fi
        fi
        
        # 检查端口占用
        if netstat -tulpn | grep ":1080 " > /dev/null; then
            echo "端口 1080 已被占用，正在清理..."
            pkill -f ss-local 2>/dev/null || true
            sleep 2
        fi
        
        # 启动shadowsocks
        ss-local -c $CONFIG_FILE -f $PID_FILE > $LOG_FILE 2>&1 &
        
        # 等待启动
        sleep 3
        
        # 检查是否启动成功
        if [ -f "$PID_FILE" ] && ps -p $(cat $PID_FILE) > /dev/null 2>&1; then
            echo "✅ Shadowsocks 启动成功 (PID: $(cat $PID_FILE))"
            echo "📝 日志文件: $LOG_FILE"
        else
            echo "❌ Shadowsocks 启动失败"
            echo "📝 查看日志: cat $LOG_FILE"
            exit 1
        fi
        ;;
    stop)
        echo "停止 Shadowsocks..."
        if [ -f "$PID_FILE" ]; then
            PID=$(cat $PID_FILE)
            if ps -p $PID > /dev/null 2>&1; then
                kill $PID
                rm -f $PID_FILE
                echo "✅ Shadowsocks 已停止"
            else
                echo "⚠️  Shadowsocks 进程不存在"
                rm -f $PID_FILE
            fi
        else
            echo "⚠️  PID文件不存在，尝试杀死所有ss-local进程"
            pkill -f ss-local 2>/dev/null || true
        fi
        ;;
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
    status)
        if [ -f "$PID_FILE" ]; then
            PID=$(cat $PID_FILE)
            if ps -p $PID > /dev/null 2>&1; then
                echo "✅ Shadowsocks 正在运行 (PID: $PID)"
                echo "📊 端口状态:"
                netstat -tulpn | grep ":1080" || echo "   端口1080未监听"
            else
                echo "❌ Shadowsocks 进程不存在"
                rm -f $PID_FILE
            fi
        else
            echo "❌ Shadowsocks 未运行"
        fi
        ;;
    test)
        echo "🧪 测试代理连接..."
        
        # 检查本地端口
        if netstat -tulpn | grep ":1080" > /dev/null; then
            echo "✅ 代理端口 1080 正在监听"
        else
            echo "❌ 代理端口 1080 未监听"
            exit 1
        fi
        
        # 测试连接
        echo "🌐 测试 Google 连接..."
        if curl --proxy socks5://127.0.0.1:1080 --connect-timeout 10 -I https://www.google.com > /dev/null 2>&1; then
            echo "✅ Google 访问成功"
        else
            echo "❌ Google 访问失败"
        fi
        
        echo "🎯 测试 club.claudecode.site 连接..."
        if curl --proxy socks5://127.0.0.1:1080 --connect-timeout 10 -I https://club.claudecode.site > /dev/null 2>&1; then
            echo "✅ club.claudecode.site 访问成功"
        else
            echo "❌ club.claudecode.site 访问失败"
        fi
        ;;
    log)
        echo "📝 Shadowsocks 日志:"
        tail -f $LOG_FILE
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|test|log}"
        exit 1
        ;;
esac
EOF
    
    chmod +x /usr/local/bin/ss-start
    
    # 创建系统服务（使用启动脚本）
    cat > /etc/systemd/system/shadowsocks-proxy.service << 'EOF'
[Unit]
Description=Shadowsocks Proxy Client
After=network.target

[Service]
Type=forking
PIDFile=/var/run/shadowsocks.pid
ExecStart=/usr/local/bin/ss-start start
ExecStop=/usr/local/bin/ss-start stop
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
    
    systemctl daemon-reload
    systemctl enable shadowsocks-proxy
    
    log_success "手动启动方式配置完成"
}

# 配置 proxychains
configure_proxychains() {
    log_info "配置 proxychains..."
    
    # 备份原配置
    cp /etc/proxychains4.conf /etc/proxychains4.conf.backup 2>/dev/null || true
    
    # 创建新配置
    cat > /etc/proxychains4.conf << 'EOF'
# proxychains.conf  VER 4.x
strict_chain
proxy_dns 
remote_dns_subnet 224
tcp_read_time_out 15000
tcp_connect_time_out 8000
localnet 127.0.0.0/255.0.0.0
quiet_mode

[ProxyList]
socks5 127.0.0.1 1080
EOF
    
    log_success "proxychains 配置完成"
}

# 启动服务
start_shadowsocks() {
    log_info "启动 Shadowsocks 服务..."
    
    # 使用我们的启动脚本
    /usr/local/bin/ss-start start
    
    if [ $? -eq 0 ]; then
        log_success "Shadowsocks 启动成功"
    else
        log_error "Shadowsocks 启动失败"
        exit 1
    fi
}

# 配置环境变量
setup_environment() {
    log_info "配置代理环境变量..."
    
    # 创建代理环境脚本
    cat > /etc/profile.d/proxy.sh << 'EOF'
# Shadowsocks 代理环境变量
export http_proxy=socks5://127.0.0.1:1080
export https_proxy=socks5://127.0.0.1:1080
export HTTP_PROXY=socks5://127.0.0.1:1080
export HTTPS_PROXY=socks5://127.0.0.1:1080
export no_proxy=localhost,127.0.0.1,::1
export NO_PROXY=localhost,127.0.0.1,::1
EOF
    
    # 创建代理开关脚本
    cat > /usr/local/bin/proxy-switch << 'EOF'
#!/bin/bash

case "$1" in
    on)
        echo "🔛 启用全局代理..."
        source /etc/profile.d/proxy.sh
        export http_proxy=socks5://127.0.0.1:1080
        export https_proxy=socks5://127.0.0.1:1080
        echo "✅ 全局代理已启用"
        echo "   HTTP_PROXY: $http_proxy"
        echo "   HTTPS_PROXY: $https_proxy"
        ;;
    off)
        echo "🔛 禁用全局代理..."
        unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY
        echo "✅ 全局代理已禁用"
        ;;
    status)
        echo "📊 代理状态:"
        if [ -n "$http_proxy" ]; then
            echo "   HTTP_PROXY: $http_proxy"
        else
            echo "   HTTP_PROXY: 未设置"
        fi
        if [ -n "$https_proxy" ]; then
            echo "   HTTPS_PROXY: $https_proxy"
        else
            echo "   HTTPS_PROXY: 未设置"
        fi
        ;;
    *)
        echo "用法: $0 {on|off|status}"
        echo "  on     - 启用全局代理"
        echo "  off    - 禁用全局代理"
        echo "  status - 查看代理状态"
        ;;
esac
EOF
    
    chmod +x /usr/local/bin/proxy-switch
    
    log_success "环境变量配置完成"
}

# 创建便捷脚本
create_helper_scripts() {
    log_info "创建便捷脚本..."
    
    # 创建 Claude 专用启动脚本
    mkdir -p /opt/hxci-campus-portal
    cat > /opt/hxci-campus-portal/start_claude_with_proxy.sh << 'EOF'
#!/bin/bash

echo "==============================================="
echo "      启动 Claude Code (带代理) v2.0"
echo "==============================================="

# 检查Shadowsocks状态
echo "🔍 检查代理服务状态..."
if /usr/local/bin/ss-start status | grep "正在运行" > /dev/null; then
    echo "✅ Shadowsocks 代理服务正常"
else
    echo "⚠️  Shadowsocks 未运行，正在启动..."
    /usr/local/bin/ss-start start
    if [ $? -ne 0 ]; then
        echo "❌ 代理启动失败，请检查配置"
        exit 1
    fi
fi

# 启用代理环境变量
source /etc/profile.d/proxy.sh

echo ""
echo "🧪 测试网络连接..."

# 测试直连
echo "📡 测试直连 club.claudecode.site..."
if curl --connect-timeout 5 -I https://club.claudecode.site > /dev/null 2>&1; then
    echo "✅ 直连成功"
    PROXY_NEEDED=false
else
    echo "❌ 直连失败，使用代理"
    PROXY_NEEDED=true
fi

# 测试代理连接
if [ "$PROXY_NEEDED" = true ]; then
    echo "🔄 测试代理连接 club.claudecode.site..."
    if curl --proxy socks5://127.0.0.1:1080 --connect-timeout 10 -I https://club.claudecode.site > /dev/null 2>&1; then
        echo "✅ 代理连接成功"
    else
        echo "❌ 代理连接失败"
        echo "🔧 请检查:"
        echo "   1. Shadowsocks 服务: ss-start status"
        echo "   2. 网络配置: ss-start test"
        echo "   3. 服务器连通性"
        exit 1
    fi
fi

echo ""
echo "🎯 代理配置完成！"
echo "📊 当前配置:"
echo "   - Shadowsocks: $(ss-start status | head -1)"
echo "   - 本地代理: socks5://127.0.0.1:1080"
echo "   - HTTP_PROXY: $HTTP_PROXY"
echo "   - HTTPS_PROXY: $HTTPS_PROXY"

echo ""
echo "🚀 准备启动 Claude Code..."
echo "💡 如需手动使用代理:"
echo "   - 环境变量: source /etc/profile.d/proxy.sh"
echo "   - Proxychains: proxychains4 <command>"
echo "   - 直接指定: curl --proxy socks5://127.0.0.1:1080 <url>"
echo ""
echo "==============================================="

# 在这里添加启动 Claude Code 的实际命令
# 例如: claude-code 或其他启动命令
EOF
    
    chmod +x /opt/hxci-campus-portal/start_claude_with_proxy.sh
    
    log_success "便捷脚本创建完成"
}

# 创建开机自启脚本
setup_autostart() {
    log_info "配置开机自启动..."
    
    # 启用系统服务
    systemctl enable shadowsocks-proxy
    
    # 创建rc.local备用方案
    if [ ! -f /etc/rc.local ]; then
        cat > /etc/rc.local << 'EOF'
#!/bin/sh -e
# rc.local - 开机自启动脚本

# 启动 Shadowsocks
/usr/local/bin/ss-start start

exit 0
EOF
        chmod +x /etc/rc.local
    else
        # 添加到现有rc.local
        if ! grep -q "ss-start start" /etc/rc.local; then
            sed -i '/^exit 0/i\/usr/local/bin/ss-start start' /etc/rc.local
        fi
    fi
    
    log_success "开机自启动配置完成"
}

# 测试连接
test_connection() {
    log_info "测试代理连接..."
    
    # 使用我们的测试脚本
    /usr/local/bin/ss-start test
    
    log_success "连接测试完成"
}

# 显示使用说明
show_usage() {
    echo ""
    echo -e "${GREEN}"
    echo "=================================================="
    echo "           代理搭建完成！v2.0"
    echo "=================================================="
    echo -e "${NC}"
    echo ""
    echo "🎯 服务管理命令:"
    echo "  ss-start start     # 启动代理"
    echo "  ss-start stop      # 停止代理"
    echo "  ss-start restart   # 重启代理"
    echo "  ss-start status    # 查看状态"
    echo "  ss-start test      # 测试连接"
    echo "  ss-start log       # 查看日志"
    echo ""
    echo "🔄 系统服务:"
    echo "  systemctl start shadowsocks-proxy    # 启动服务"
    echo "  systemctl status shadowsocks-proxy   # 查看服务状态"
    echo ""
    echo "🌐 代理开关:"
    echo "  proxy-switch on    # 启用全局代理"
    echo "  proxy-switch off   # 禁用全局代理"
    echo "  proxy-switch status # 查看代理状态"
    echo ""
    echo "🚀 Claude Code 启动:"
    echo "  cd /opt/hxci-campus-portal"
    echo "  ./start_claude_with_proxy.sh"
    echo ""
    echo "🔧 手动使用代理:"
    echo "  source /etc/profile.d/proxy.sh"
    echo "  curl https://club.claudecode.site"
    echo ""
    echo "🌐 使用 proxychains:"
    echo "  proxychains4 curl https://club.claudecode.site"
    echo ""
    echo "📍 重要文件位置:"
    echo "  配置文件: /etc/shadowsocks/config.json"
    echo "  日志文件: /var/log/shadowsocks.log"
    echo "  PID文件:  /var/run/shadowsocks.pid"
    echo ""
    echo -e "${YELLOW}💡 故障排除:${NC}"
    echo "  如果连接失败: ss-start log"
    echo "  如果端口占用: ss-start restart"
    echo "  如果配置错误: cat /etc/shadowsocks/config.json"
    echo ""
    echo -e "${GREEN}✅ 代理服务已配置为开机自启动${NC}"
}

# 主函数
main() {
    show_banner
    
    check_system
    cleanup_existing
    install_dependencies
    configure_shadowsocks
    setup_manual_start
    configure_proxychains
    start_shadowsocks
    setup_environment
    create_helper_scripts
    setup_autostart
    test_connection
    
    show_usage
    
    log_success "Shadowsocks 代理搭建完成！v2.0"
    echo ""
    log_info "立即测试: ss-start test"
    log_info "启动Claude: cd /opt/hxci-campus-portal && ./start_claude_with_proxy.sh"
}

# 错误处理
trap 'log_error "脚本执行失败，请检查错误信息"; exit 1' ERR

# 运行主函数
main "$@"