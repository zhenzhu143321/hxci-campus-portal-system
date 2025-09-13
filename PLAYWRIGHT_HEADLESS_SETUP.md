# Playwright 无头浏览器自动化配置指南

## 🎯 给新Claude实例的重要说明

**在远程Linux服务器上使用Playwright时，必须使用无头模式避免屏幕乱码问题！**

## ⚠️ 问题背景

在远程服务器环境中，直接使用GUI浏览器会导致：
- 屏幕显示乱码
- 布局完全混乱
- 终端界面被破坏
- 浏览器无法正常启动

## ✅ 解决方案

### 1. 环境配置 (必须步骤)

```bash
# 启动虚拟显示服务器
nohup Xvfb :99 -screen 0 1280x720x24 > /dev/null 2>&1 &

# 设置必要的环境变量
export DISPLAY=:99
export PLAYWRIGHT_BROWSERS_PATH=~/.cache/ms-playwright
export PWTEST_SKIP_BROWSER_DOWNLOAD=1

# 创建Chrome符号链接 (MCP工具需要)
sudo mkdir -p /opt/google/chrome
sudo ln -sf ~/.cache/ms-playwright/chromium-1187/chrome-linux/chrome /opt/google/chrome/chrome
```

### 2. 验证安装

```bash
cd ~/myweb

# 检查Playwright浏览器
ls ~/.cache/ms-playwright/chromium-*/

# 测试基础功能
node test-headless-browser.js

# 运行测试套件
xvfb-run -a npx playwright test
```

### 3. MCP工具使用

配置完成后，以下MCP工具可以正常使用：

```javascript
// ✅ 导航到网页
mcp__playwright__browser_navigate("https://example.com")

// ✅ 截取屏幕截图
mcp__playwright__browser_take_screenshot({
  filename: "screenshot.png",
  type: "png"
})

// ✅ 点击元素
mcp__playwright__browser_click({
  element: "按钮描述",
  ref: "元素引用"
})

// ✅ 获取页面快照
mcp__playwright__browser_snapshot()

// ✅ 调整浏览器大小
mcp__playwright__browser_resize(1280, 720)
```

## 📁 重要文件

### 配置文件
- `~/myweb/playwright.config.js` - Playwright配置
- `~/myweb/test-headless-browser.js` - 测试脚本
- `~/myweb/start-headless-browser.sh` - 启动脚本

### 生成的文件
- `~/myweb/*.png` - 截图文件
- `~/.cache/ms-playwright/` - 浏览器安装目录
- `~/.playwright-mcp/` - MCP工具截图目录

## 🚀 快速开始命令

```bash
# 方法1: 使用启动脚本
cd ~/myweb && ./start-headless-browser.sh

# 方法2: 手动设置环境
export DISPLAY=:99
export PLAYWRIGHT_BROWSERS_PATH=~/.cache/ms-playwright
cd ~/myweb && npx playwright test

# 方法3: 使用测试脚本
cd ~/myweb && node test-headless-browser.js
```

## 🔧 故障排除

### 问题1: "Missing X server or $DISPLAY"
```bash
# 启动虚拟显示
nohup Xvfb :99 -screen 0 1280x720x24 > /dev/null 2>&1 &
export DISPLAY=:99
```

### 问题2: "Chromium distribution 'chrome' is not found"
```bash
# 创建符号链接
sudo mkdir -p /opt/google/chrome
sudo ln -sf ~/.cache/ms-playwright/chromium-1187/chrome-linux/chrome /opt/google/chrome/chrome
```

### 问题3: 屏幕仍然乱码
```bash
# 强制使用xvfb
xvfb-run -a npx playwright test --project=chromium
```

## ✅ 成功验证标志

当配置正确时，你应该看到：
- ✅ 无头浏览器可以访问网页
- ✅ 截图功能正常工作
- ✅ MCP工具响应正常
- ✅ 终端界面保持清洁
- ✅ 没有GUI弹窗或乱码

## 📝 给新Claude实例的检查清单

1. [ ] 确认虚拟显示服务器运行中 (`ps aux | grep Xvfb`)
2. [ ] 设置环境变量 (`echo $DISPLAY` 应显示 `:99`)
3. [ ] 验证浏览器路径 (`ls /opt/google/chrome/chrome`)
4. [ ] 运行测试脚本确认功能正常
5. [ ] 尝试使用MCP工具进行基本操作

配置完成后，你就可以安全地使用Playwright进行浏览器自动化，无需担心屏幕显示问题！

---

**最后更新**: 2025-01-09  
**环境**: Ubuntu 22.04 + Playwright + xvfb  
**状态**: ✅ 测试通过，MCP工具正常工作