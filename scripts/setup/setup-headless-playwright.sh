#!/bin/bash

# Playwright 无头浏览器一键配置脚本
# 使用方法: ./setup-headless-playwright.sh

set -e

echo "🚀 开始配置 Playwright 无头浏览器自动化..."

# 1. 检查并启动虚拟显示服务器
echo "🖥️  配置虚拟显示服务器..."
if ! pgrep -x "Xvfb" > /dev/null; then
    echo "   启动 Xvfb 虚拟显示..."
    nohup Xvfb :99 -screen 0 1280x720x24 > /dev/null 2>&1 &
    sleep 2
else
    echo "   ✅ Xvfb 已经在运行"
fi

# 2. 设置环境变量
echo "⚙️  设置环境变量..."
export DISPLAY=:99
export PLAYWRIGHT_BROWSERS_PATH=~/.cache/ms-playwright
export PWTEST_SKIP_BROWSER_DOWNLOAD=1

# 将环境变量写入 .bashrc
if ! grep -q "DISPLAY=:99" ~/.bashrc; then
    echo "export DISPLAY=:99" >> ~/.bashrc
    echo "export PLAYWRIGHT_BROWSERS_PATH=~/.cache/ms-playwright" >> ~/.bashrc
    echo "export PWTEST_SKIP_BROWSER_DOWNLOAD=1" >> ~/.bashrc
    echo "   ✅ 环境变量已添加到 ~/.bashrc"
fi

# 3. 检查Playwright浏览器安装
echo "🌐 检查 Playwright 浏览器..."
if [ ! -d ~/.cache/ms-playwright/chromium-* ]; then
    echo "   安装 Playwright Chromium..."
    npx playwright install chromium
else
    echo "   ✅ Playwright Chromium 已安装"
fi

# 4. 创建 Chrome 符号链接 (MCP工具需要)
echo "🔗 配置 MCP 工具兼容性..."
if [ ! -f /opt/google/chrome/chrome ]; then
    sudo mkdir -p /opt/google/chrome
    CHROMIUM_PATH=$(find ~/.cache/ms-playwright -name chrome -path "*/chrome-linux/chrome" | head -1)
    if [ -n "$CHROMIUM_PATH" ]; then
        sudo ln -sf "$CHROMIUM_PATH" /opt/google/chrome/chrome
        echo "   ✅ Chrome 符号链接已创建"
    else
        echo "   ⚠️  未找到 Chromium 可执行文件"
    fi
else
    echo "   ✅ Chrome 符号链接已存在"
fi

# 5. 验证配置
echo "🧪 验证配置..."

# 检查虚拟显示
if pgrep -x "Xvfb" > /dev/null; then
    echo "   ✅ 虚拟显示服务器运行中"
else
    echo "   ❌ 虚拟显示服务器未运行"
fi

# 检查浏览器路径
if [ -f /opt/google/chrome/chrome ]; then
    echo "   ✅ Chrome 路径配置正确"
else
    echo "   ❌ Chrome 路径配置失败"
fi

# 检查环境变量
if [ "$DISPLAY" = ":99" ]; then
    echo "   ✅ DISPLAY 环境变量设置正确"
else
    echo "   ❌ DISPLAY 环境变量设置失败"
fi

# 6. 运行测试
echo "🎯 运行功能测试..."
cd ~/myweb

if [ -f test-headless-browser.js ]; then
    echo "   运行无头浏览器测试..."
    if node test-headless-browser.js > /dev/null 2>&1; then
        echo "   ✅ 无头浏览器测试通过"
    else
        echo "   ⚠️  无头浏览器测试失败，请检查配置"
    fi
else
    echo "   ⚠️  测试文件不存在，跳过功能测试"
fi

# 7. 完成提示
echo ""
echo "🎉 Playwright 无头浏览器配置完成！"
echo ""
echo "📋 可用的命令："
echo "   节点测试:     node test-headless-browser.js"
echo "   Playwright:   npx playwright test"
echo "   启动脚本:     ./start-headless-browser.sh"
echo ""
echo "🔧 MCP工具已准备就绪，可以使用："
echo "   - mcp__playwright__browser_navigate"
echo "   - mcp__playwright__browser_take_screenshot"
echo "   - mcp__playwright__browser_click"
echo "   - mcp__playwright__browser_snapshot"
echo ""
echo "💡 提示: 新终端需要运行 'source ~/.bashrc' 来加载环境变量"
echo "📖 详细文档: PLAYWRIGHT_HEADLESS_SETUP.md"