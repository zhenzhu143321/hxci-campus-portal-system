#!/bin/bash

# =============================================================================
# Shadowsocks 一键代理搭建脚本
# 用途: 解决阿里云服务器访问 club.claudecode.site 的问题
# 作者: Claude Code AI
# 创建时间: 2025-08-16
# =============================================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# 显示横幅
show_banner() {
    echo -e "${BLUE}"
    echo "=================================================="
    echo "     Shadowsocks 一键代理搭建脚本"
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

# 更新系统
update_system() {
    log_info "更新系统包管理器..."
    apt update -y
    log_success "系统更新完成"
}

# 安装依赖
install_dependencies() {
    log_info "安装 Shadowsocks 和相关工具..."
    
    # 安装基础工具
    apt install -y curl wget vim net-tools
    
    # 安装 shadowsocks-libev
    apt install -y shadowsocks-libev
    
    # 安装 proxychains4
    apt install -y proxychains4
    
    log_success "依赖安装完成"
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
    "fast_open": false
}
EOF
    
    log_success "Shadowsocks 配置文件创建完成"
}

# 配置 proxychains
configure_proxychains() {
    log_info "配置 proxychains..."
    
    # 备份原配置
    cp /etc/proxychains4.conf /etc/proxychains4.conf.backup
    
    # 创建新配置
    cat > /etc/proxychains4.conf << 'EOF'
# proxychains.conf  VER 4.x
#
#        HTTP, SOCKS4a, SOCKS5 tunneling proxifier with DNS.

strict_chain
proxy_dns 
remote_dns_subnet 224
tcp_read_time_out 15000
tcp_connect_time_out 8000
localnet 127.0.0.0/255.0.0.0
quiet_mode

[ProxyList]
# Shadowsocks proxy
socks5 127.0.0.1 1080
EOF
    
    log_success "proxychains 配置完成"
}

# 创建系统服务
create_systemd_service() {
    log_info "创建 Shadowsocks 系统服务..."
    
    cat > /etc/systemd/system/shadowsocks.service << 'EOF'
[Unit]
Description=Shadowsocks Client
After=network.target

[Service]
Type=forking
PIDFile=/var/run/shadowsocks.pid
ExecStart=/usr/bin/ss-local -c /etc/shadowsocks/config.json -d start
ExecStop=/usr/bin/ss-local -c /etc/shadowsocks/config.json -d stop
Restart=always
User=root

[Install]
WantedBy=multi-user.target
EOF
    
    # 重新加载 systemd
    systemctl daemon-reload
    
    # 启用服务
    systemctl enable shadowsocks
    
    log_success "系统服务创建完成"
}

# 启动服务
start_services() {
    log_info "启动 Shadowsocks 服务..."
    
    # 启动服务
    systemctl start shadowsocks
    
    # 等待服务启动
    sleep 3
    
    # 检查服务状态
    if systemctl is-active --quiet shadowsocks; then
        log_success "Shadowsocks 服务启动成功"
    else
        log_error "Shadowsocks 服务启动失败"
        systemctl status shadowsocks
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
    
    # 使环境变量立即生效
    source /etc/profile.d/proxy.sh
    
    log_success "代理环境变量配置完成"
}

# 创建便捷脚本
create_helper_scripts() {
    log_info "创建便捷管理脚本..."
    
    # 创建代理控制脚本
    cat > /usr/local/bin/proxy-control << 'EOF'
#!/bin/bash

case "$1" in
    start)
        echo "启动 Shadowsocks 代理..."
        systemctl start shadowsocks
        source /etc/profile.d/proxy.sh
        echo "代理已启动"
        ;;
    stop)
        echo "停止 Shadowsocks 代理..."
        systemctl stop shadowsocks
        unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY
        echo "代理已停止"
        ;;
    restart)
        echo "重启 Shadowsocks 代理..."
        systemctl restart shadowsocks
        source /etc/profile.d/proxy.sh
        echo "代理已重启"
        ;;
    status)
        echo "Shadowsocks 服务状态:"
        systemctl status shadowsocks --no-pager
        echo ""
        echo "代理端口状态:"
        netstat -tulpn | grep 1080 || echo "代理端口未监听"
        ;;
    test)
        echo "测试代理连接..."
        echo "测试 Google..."
        curl -I --connect-timeout 10 --proxy socks5://127.0.0.1:1080 https://www.google.com 2>/dev/null && echo "Google 访问成功" || echo "Google 访问失败"
        echo "测试 club.claudecode.site..."
        curl -I --connect-timeout 10 --proxy socks5://127.0.0.1:1080 https://club.claudecode.site 2>/dev/null && echo "Claude Code 网站访问成功" || echo "Claude Code 网站访问失败"
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|test}"
        echo "  start   - 启动代理"
        echo "  stop    - 停止代理" 
        echo "  restart - 重启代理"
        echo "  status  - 查看状态"
        echo "  test    - 测试连接"
        exit 1
        ;;
esac
EOF
    
    chmod +x /usr/local/bin/proxy-control
    
    # 创建 Claude 专用启动脚本
    cat > /opt/hxci-campus-portal/start_claude_with_proxy.sh << 'EOF'
#!/bin/bash

echo "==============================================="
echo "      启动 Claude Code (带代理)"
echo "==============================================="

# 加载代理环境变量
source /etc/profile.d/proxy.sh

# 检查代理状态
echo "检查代理状态..."
if netstat -tulpn | grep 1080 > /dev/null; then
    echo "✅ 代理服务正常运行"
else
    echo "❌ 代理服务未运行，正在启动..."
    systemctl start shadowsocks
    sleep 3
fi

# 测试代理连接
echo "测试 club.claudecode.site 连接..."
if curl -I --connect-timeout 10 https://club.claudecode.site > /dev/null 2>&1; then
    echo "✅ club.claudecode.site 连接成功"
else
    echo "⚠️  直连失败，使用代理测试..."
    if curl -I --connect-timeout 10 --proxy socks5://127.0.0.1:1080 https://club.claudecode.site > /dev/null 2>&1; then
        echo "✅ 通过代理访问 club.claudecode.site 成功"
    else
        echo "❌ 代理访问也失败，请检查网络配置"
    fi
fi

echo ""
echo "代理已配置，可以启动 Claude Code"
echo "当前代理设置:"
echo "  HTTP_PROXY: $HTTP_PROXY"
echo "  HTTPS_PROXY: $HTTPS_PROXY"
echo ""
echo "如需使用 proxychains: proxychains4 <your-command>"
echo "==============================================="

# 在这里添加启动 Claude Code 的命令
# claude-code 或者您的实际启动命令
EOF
    
    chmod +x /opt/hxci-campus-portal/start_claude_with_proxy.sh
    
    log_success "便捷脚本创建完成"
}

# 测试连接
test_connection() {
    log_info "测试代理连接..."
    
    # 检查本地代理端口
    if netstat -tulpn | grep 1080 > /dev/null; then
        log_success "代理端口 1080 正常监听"
    else
        log_error "代理端口 1080 未监听"
        return 1
    fi
    
    # 测试代理连接
    log_info "测试 Google 连接..."
    if curl -I --connect-timeout 10 --proxy socks5://127.0.0.1:1080 https://www.google.com > /dev/null 2>&1; then
        log_success "Google 访问成功"
    else
        log_warning "Google 访问失败"
    fi
    
    log_info "测试 club.claudecode.site 连接..."
    if curl -I --connect-timeout 10 --proxy socks5://127.0.0.1:1080 https://club.claudecode.site > /dev/null 2>&1; then
        log_success "club.claudecode.site 访问成功"
    else
        log_warning "club.claudecode.site 访问失败"
    fi
}

# 显示使用说明
show_usage() {
    echo -e "${GREEN}"
    echo "=================================================="
    echo "           代理搭建完成！"
    echo "=================================================="
    echo -e "${NC}"
    echo ""
    echo "🎯 服务管理命令:"
    echo "  proxy-control start    # 启动代理"
    echo "  proxy-control stop     # 停止代理"
    echo "  proxy-control restart  # 重启代理"
    echo "  proxy-control status   # 查看状态"
    echo "  proxy-control test     # 测试连接"
    echo ""
    echo "🚀 Claude Code 启动:"
    echo "  cd /opt/hxci-campus-portal"
    echo "  ./start_claude_with_proxy.sh"
    echo ""
    echo "🔧 手动使用代理:"
    echo "  export https_proxy=socks5://127.0.0.1:1080"
    echo "  curl https://club.claudecode.site"
    echo ""
    echo "🌐 使用 proxychains:"
    echo "  proxychains4 curl https://club.claudecode.site"
    echo "  proxychains4 git clone https://github.com/xxx/xxx.git"
    echo ""
    echo "📍 配置文件位置:"
    echo "  Shadowsocks: /etc/shadowsocks/config.json"
    echo "  Proxychains: /etc/proxychains4.conf"
    echo ""
    echo -e "${YELLOW}注意: 代理服务已设置为开机自启动${NC}"
}

# 主函数
main() {
    show_banner
    
    check_system
    update_system
    install_dependencies
    configure_shadowsocks
    configure_proxychains
    create_systemd_service
    start_services
    setup_environment
    create_helper_scripts
    test_connection
    
    echo ""
    show_usage
    
    log_success "Shadowsocks 代理搭建完成！"
}

# 运行主函数
main "$@"