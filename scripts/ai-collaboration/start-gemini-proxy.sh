#!/bin/bash

# 启动Gemini to OpenRouter代理服务的便捷脚本

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
PROXY_PORT=8888
PROXY_HOST="127.0.0.1"
PROXY_SCRIPT="$(dirname "$0")/gemini-openrouter-proxy.py"

# 功能：检查依赖
check_dependencies() {
    echo -e "${CYAN}🔍 检查依赖...${NC}"
    
    # 检查Python
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}❌ Python3未安装${NC}"
        return 1
    fi
    
    # 检查Flask
    if ! python3 -c "import flask" 2>/dev/null; then
        echo -e "${YELLOW}⚠️  Flask未安装，正在安装...${NC}"
        pip3 install flask requests
    fi
    
    echo -e "${GREEN}✅ 依赖检查通过${NC}"
    return 0
}

# 功能：启动代理
start_proxy() {
    echo -e "${CYAN}🚀 启动Gemini to OpenRouter代理服务...${NC}"
    
    # 检查端口是否被占用
    if lsof -Pi :$PROXY_PORT -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${YELLOW}⚠️  端口 $PROXY_PORT 已被占用${NC}"
        echo -e "${CYAN}是否要停止现有服务并重新启动？(y/n)${NC}"
        read -r answer
        if [[ "$answer" == "y" ]]; then
            stop_proxy
        else
            return 1
        fi
    fi
    
    # 启动代理服务（后台运行）
    nohup python3 "$PROXY_SCRIPT" > /tmp/gemini-proxy.log 2>&1 &
    local pid=$!
    echo $pid > /tmp/gemini-proxy.pid
    
    # 等待服务启动
    sleep 2
    
    # 检查服务是否成功启动
    if kill -0 $pid 2>/dev/null; then
        echo -e "${GREEN}✅ 代理服务已启动 (PID: $pid)${NC}"
        echo -e "${BLUE}📍 代理地址: http://$PROXY_HOST:$PROXY_PORT${NC}"
        echo ""
        echo -e "${PURPLE}🔧 配置Gemini CLI使用代理:${NC}"
        echo -e "${YELLOW}export GOOGLE_GEMINI_BASE_URL='http://$PROXY_HOST:$PROXY_PORT'${NC}"
        echo -e "${YELLOW}export GEMINI_API_KEY='dummy-key-for-proxy'${NC}"
        echo ""
        echo -e "${GREEN}📝 查看日志: tail -f /tmp/gemini-proxy.log${NC}"
        return 0
    else
        echo -e "${RED}❌ 代理服务启动失败${NC}"
        echo -e "${YELLOW}查看错误日志: cat /tmp/gemini-proxy.log${NC}"
        return 1
    fi
}

# 功能：停止代理
stop_proxy() {
    echo -e "${CYAN}🛑 停止代理服务...${NC}"
    
    if [[ -f /tmp/gemini-proxy.pid ]]; then
        local pid=$(cat /tmp/gemini-proxy.pid)
        if kill -0 $pid 2>/dev/null; then
            kill $pid
            rm /tmp/gemini-proxy.pid
            echo -e "${GREEN}✅ 代理服务已停止${NC}"
        else
            echo -e "${YELLOW}⚠️  进程不存在${NC}"
            rm /tmp/gemini-proxy.pid
        fi
    else
        # 尝试通过端口查找进程
        local pid=$(lsof -ti:$PROXY_PORT)
        if [[ -n "$pid" ]]; then
            kill $pid
            echo -e "${GREEN}✅ 代理服务已停止 (PID: $pid)${NC}"
        else
            echo -e "${YELLOW}⚠️  没有找到运行中的代理服务${NC}"
        fi
    fi
}

# 功能：查看状态
check_status() {
    echo -e "${CYAN}📊 代理服务状态${NC}"
    
    local pid=""
    if [[ -f /tmp/gemini-proxy.pid ]]; then
        pid=$(cat /tmp/gemini-proxy.pid)
    fi
    
    if [[ -n "$pid" ]] && kill -0 $pid 2>/dev/null; then
        echo -e "${GREEN}✅ 代理服务运行中 (PID: $pid)${NC}"
        echo -e "${BLUE}📍 代理地址: http://$PROXY_HOST:$PROXY_PORT${NC}"
        
        # 测试代理健康状态
        if curl -s "http://$PROXY_HOST:$PROXY_PORT/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 健康检查通过${NC}"
        else
            echo -e "${YELLOW}⚠️  健康检查失败${NC}"
        fi
    else
        echo -e "${RED}❌ 代理服务未运行${NC}"
    fi
}

# 功能：测试代理
test_proxy() {
    echo -e "${CYAN}🧪 测试代理功能...${NC}"
    
    # 设置环境变量
    export GOOGLE_GEMINI_BASE_URL="http://$PROXY_HOST:$PROXY_PORT"
    export GEMINI_API_KEY="test-key"
    
    # 测试简单查询
    echo -e "${YELLOW}发送测试请求: '2+2等于几？'${NC}"
    echo "2+2等于几？回答一个数字即可。" | gemini --prompt "2+2等于几？回答一个数字即可。"
    
    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ 代理测试成功！${NC}"
    else
        echo -e "${RED}❌ 代理测试失败${NC}"
        echo -e "${YELLOW}查看日志: tail -20 /tmp/gemini-proxy.log${NC}"
    fi
}

# 功能：显示帮助
show_help() {
    echo -e "${BLUE}================ Gemini代理服务管理 ================${NC}"
    echo ""
    echo -e "${YELLOW}使用方法:${NC}"
    echo "  $0 start   - 启动代理服务"
    echo "  $0 stop    - 停止代理服务"
    echo "  $0 restart - 重启代理服务"
    echo "  $0 status  - 查看服务状态"
    echo "  $0 test    - 测试代理功能"
    echo "  $0 logs    - 查看服务日志"
    echo "  $0 help    - 显示帮助"
    echo ""
    echo -e "${GREEN}✨ 功能说明:${NC}"
    echo "  • 将Gemini CLI请求转发到OpenRouter"
    echo "  • 自动格式转换（Gemini ↔ OpenRouter）"
    echo "  • 无速率限制，使用付费API"
    echo "  • 保留Gemini CLI所有功能"
}

# 主程序逻辑
case "${1:-help}" in
    "start")
        check_dependencies && start_proxy
        ;;
    "stop")
        stop_proxy
        ;;
    "restart")
        stop_proxy
        sleep 1
        check_dependencies && start_proxy
        ;;
    "status")
        check_status
        ;;
    "test")
        test_proxy
        ;;
    "logs")
        if [[ -f /tmp/gemini-proxy.log ]]; then
            tail -f /tmp/gemini-proxy.log
        else
            echo -e "${YELLOW}⚠️  日志文件不存在${NC}"
        fi
        ;;
    "help"|*)
        show_help
        ;;
esac