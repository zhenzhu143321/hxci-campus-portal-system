#!/bin/bash

# 无头浏览器启动脚本 - 解决屏幕乱码问题
# 使用方法: ./start-headless-browser.sh [URL]

set -e

URL=${1:-"https://example.com"}

echo "🚀 启动无头浏览器自动化..."
echo "📍 目标网址: $URL"

# 设置环境变量
export DISPLAY=:99
export PLAYWRIGHT_BROWSERS_PATH=~/.cache/ms-playwright
export PWTEST_SKIP_TEST_OUTPUT=1

# 确保 xvfb 运行
if ! pgrep -x "Xvfb" > /dev/null; then
    echo "🔧 启动虚拟显示服务器..."
    xvfb-run -a -s "-screen 0 1280x720x24" &
    sleep 2
fi

echo "✅ 无头浏览器环境已准备就绪"
echo "📋 配置说明:"
echo "   - 虚拟显示: Xvfb (避免GUI冲突)"
echo "   - 浏览器: Playwright Chromium"
echo "   - 模式: 完全无头 (headless)"
echo "   - 分辨率: 1280x720"

# 运行Playwright测试
if [ -f "tests/basic.spec.js" ]; then
    echo "🧪 运行基础浏览器测试..."
    xvfb-run -a npx playwright test tests/basic.spec.js
else
    echo "⚠️  没有找到测试文件，创建简单测试..."
    node test-headless-browser.js
fi

echo "🎉 无头浏览器自动化完成！"
echo "📸 截图文件: test-screenshot.png"
echo "💡 提示: 使用此方式可以避免远程服务器上的屏幕乱码问题"