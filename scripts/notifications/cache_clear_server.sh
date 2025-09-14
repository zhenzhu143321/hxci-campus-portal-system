#!/bin/bash

# 缓存清理页面HTTP服务脚本
# 用于提供便捷的HTTP访问方式，无需用户手动下载文件

echo "🌐 启动localStorage缓存清理页面HTTP服务"
echo "======================================="

# 检查是否有可用的HTTP服务器
if command -v python3 &> /dev/null; then
    HTTP_SERVER="python3"
    HTTP_CMD="python3 -m http.server 8099"
elif command -v python &> /dev/null; then
    HTTP_SERVER="python"
    HTTP_CMD="python -m SimpleHTTPServer 8099"
elif command -v php &> /dev/null; then
    HTTP_SERVER="php"
    HTTP_CMD="php -S localhost:8099"
else
    echo "❌ 未找到可用的HTTP服务器 (python3/python/php)"
    echo "💡 建议用户直接使用浏览器访问文件路径"
    exit 1
fi

echo "🔍 检测到HTTP服务器: $HTTP_SERVER"
echo "🌐 启动服务地址: http://localhost:8099/clear-notification-cache.html"
echo ""

# 确保清理页面存在
CACHE_PAGE="/opt/hxci-campus-portal/hxci-campus-portal-system/demo/clear-notification-cache.html"
if [ ! -f "$CACHE_PAGE" ]; then
    echo "⚠️  清理页面不存在，正在生成..."

    # 导入缓存清理工具函数
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    source "$SCRIPT_DIR/cache_clear_utils.sh"

    # 生成清理页面
    generate_cache_clear_page

    # 复制到demo目录
    cp /tmp/clear-notification-cache.html "$CACHE_PAGE"
    echo "✅ 清理页面已生成并复制到demo目录"
fi

echo "📋 用户访问指南："
echo "1. 🌐 HTTP访问: http://localhost:8099/clear-notification-cache.html"
echo "2. 📂 文件访问: file://$CACHE_PAGE"
echo "3. 🔧 手动清理: localStorage.clear(); location.reload();"
echo ""
echo "⚠️  注意: 服务将在前台运行，按 Ctrl+C 停止服务"
echo ""

# 切换到demo目录并启动HTTP服务
cd /opt/hxci-campus-portal/hxci-campus-portal-system/demo/

echo "🚀 启动HTTP服务中..."
echo "======================================="
$HTTP_CMD