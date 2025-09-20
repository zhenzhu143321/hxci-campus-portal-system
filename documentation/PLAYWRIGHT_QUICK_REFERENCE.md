# 🔖 Playwright MCP工具快速参考手册

## 🚀 一分钟快速上手

```bash
# 新Claude实例必须先运行这个！
cd ~/myweb && ./setup-headless-playwright.sh
```

## 📋 MCP工具完整列表

### 🌐 导航类工具
```javascript
// 导航到指定URL
mcp__playwright__browser_navigate({ url: "https://example.com" })

// 返回上一页
mcp__playwright__browser_navigate_back()

// 关闭浏览器
mcp__playwright__browser_close()
```

### 📸 截图和快照
```javascript
// 截图整个视窗
mcp__playwright__browser_take_screenshot({ filename: "page.png", type: "png" })

// 截图特定元素
mcp__playwright__browser_take_screenshot({ 
  element: "按钮描述", 
  ref: "e15", 
  filename: "button.png" 
})

// 截图整个页面
mcp__playwright__browser_take_screenshot({ 
  filename: "fullpage.png", 
  fullPage: true 
})

// 获取页面结构快照 (最重要！)
mcp__playwright__browser_snapshot()
```

### 🖱️ 交互操作
```javascript
// 点击元素
mcp__playwright__browser_click({ 
  element: "登录按钮", 
  ref: "e12" 
})

// 双击
mcp__playwright__browser_click({ 
  element: "文件图标", 
  ref: "e8", 
  doubleClick: true 
})

// 右键点击
mcp__playwright__browser_click({ 
  element: "菜单项", 
  ref: "e5", 
  button: "right" 
})

// 悬停
mcp__playwright__browser_hover({ 
  element: "导航菜单", 
  ref: "e10" 
})
```

### ⌨️ 文本输入
```javascript
// 在输入框中输入文本
mcp__playwright__browser_type({ 
  element: "用户名输入框", 
  ref: "e7", 
  text: "myusername" 
})

// 慢速输入 (逐个字符)
mcp__playwright__browser_type({ 
  element: "密码输入框", 
  ref: "e8", 
  text: "password123", 
  slowly: true 
})

// 输入后按回车
mcp__playwright__browser_type({ 
  element: "搜索框", 
  ref: "e9", 
  text: "search query", 
  submit: true 
})
```

### 📝 表单操作
```javascript
// 批量填写表单
mcp__playwright__browser_fill_form({
  fields: [
    { name: "用户名", type: "textbox", ref: "e10", value: "admin" },
    { name: "密码", type: "textbox", ref: "e11", value: "123456" },
    { name: "记住我", type: "checkbox", ref: "e12", value: "true" },
    { name: "性别", type: "radio", ref: "e13", value: "male" },
    { name: "城市", type: "combobox", ref: "e14", value: "北京" }
  ]
})

// 选择下拉框选项
mcp__playwright__browser_select_option({ 
  element: "国家选择", 
  ref: "e15", 
  values: ["China"] 
})

// 多选
mcp__playwright__browser_select_option({ 
  element: "兴趣爱好", 
  ref: "e16", 
  values: ["篮球", "游泳", "阅读"] 
})
```

### ⏳ 等待和时序
```javascript
// 等待指定时间 (秒)
mcp__playwright__browser_wait_for({ time: 3 })

// 等待文本出现
mcp__playwright__browser_wait_for({ text: "加载完成" })

// 等待文本消失
mcp__playwright__browser_wait_for({ textGone: "正在加载..." })
```

### 🖼️ 窗口管理
```javascript
// 调整浏览器窗口大小
mcp__playwright__browser_resize({ width: 1920, height: 1080 })

// 管理标签页
mcp__playwright__browser_tabs({ action: "list" })        // 列出所有标签页
mcp__playwright__browser_tabs({ action: "new" })         // 新建标签页
mcp__playwright__browser_tabs({ action: "close" })       // 关闭当前标签页
mcp__playwright__browser_tabs({ action: "select", index: 0 }) // 切换标签页
```

### ⌨️ 键盘操作
```javascript
// 按键
mcp__playwright__browser_press_key({ key: "Enter" })
mcp__playwright__browser_press_key({ key: "Escape" })
mcp__playwright__browser_press_key({ key: "Tab" })
mcp__playwright__browser_press_key({ key: "ArrowDown" })
```

### 🗂️ 文件操作
```javascript
// 文件上传
mcp__playwright__browser_file_upload({ 
  paths: ["/path/to/file1.txt", "/path/to/file2.pdf"] 
})
```

### 🎛️ 高级操作
```javascript
// 执行JavaScript代码
mcp__playwright__browser_evaluate({ 
  function: "() => { return document.title; }" 
})

// 在特定元素上执行代码
mcp__playwright__browser_evaluate({ 
  element: "按钮元素",
  ref: "e10",
  function: "(element) => { element.style.backgroundColor = 'red'; }" 
})

// 拖拽操作
mcp__playwright__browser_drag({ 
  startElement: "源元素", 
  startRef: "e8", 
  endElement: "目标元素", 
  endRef: "e12" 
})
```

### 🔍 调试工具
```javascript
// 查看控制台消息
mcp__playwright__browser_console_messages()

// 查看网络请求
mcp__playwright__browser_network_requests()

// 处理弹窗
mcp__playwright__browser_handle_dialog({ accept: true })
mcp__playwright__browser_handle_dialog({ accept: false })
mcp__playwright__browser_handle_dialog({ accept: true, promptText: "确认内容" })
```

## 🎯 常用操作模式

### 模式1：标准页面分析流程
```javascript
1. mcp__playwright__browser_navigate({ url: "目标网址" })
2. mcp__playwright__browser_snapshot() // 分析页面结构
3. mcp__playwright__browser_take_screenshot({ filename: "analysis.png" })
4. // 根据快照进行后续操作
```

### 模式2：表单填写流程
```javascript
1. mcp__playwright__browser_navigate({ url: "表单页面" })
2. mcp__playwright__browser_snapshot() // 获取表单结构
3. mcp__playwright__browser_fill_form({ fields: [...] })
4. mcp__playwright__browser_take_screenshot({ filename: "filled-form.png" })
5. mcp__playwright__browser_click({ element: "提交按钮", ref: "eXX" })
```

### 模式3：搜索操作流程
```javascript
1. mcp__playwright__browser_navigate({ url: "搜索网站" })
2. mcp__playwright__browser_type({ element: "搜索框", ref: "eXX", text: "查询内容" })
3. mcp__playwright__browser_click({ element: "搜索按钮", ref: "eXX" })
4. mcp__playwright__browser_wait_for({ text: "搜索结果" })
5. mcp__playwright__browser_take_screenshot({ filename: "search-results.png" })
```

## ⚠️ 重要注意事项

### ✅ 最佳实践
- **总是先运行 `browser_snapshot()`** 来了解页面结构
- **使用准确的元素描述** 和正确的ref引用
- **频繁截图** 记录操作过程，便于调试
- **合理使用等待** 给页面加载充足的时间
- **元素定位要精确** 避免点击错误的元素

### ❌ 常见错误
- 不获取快照就盲目操作
- 使用错误的元素引用(ref)
- 不等待页面加载完成
- 忽略页面跳转和状态变化
- 不验证操作结果

### 🐛 调试技巧
```javascript
// 调试模式：详细记录每一步
1. mcp__playwright__browser_take_screenshot({ filename: "step1-before.png" })
2. mcp__playwright__browser_snapshot() // 记录页面状态
3. // 执行操作
4. mcp__playwright__browser_take_screenshot({ filename: "step1-after.png" })
5. mcp__playwright__browser_snapshot() // 验证操作结果
```

## 🎓 学习建议

1. **从基础开始**：先掌握导航、截图、快照三大基础操作
2. **循序渐进**：按照 导航 → 分析 → 交互 → 验证 的顺序学习
3. **实践导向**：每学一个工具就立即实践
4. **记录过程**：保存每次练习的截图和经验
5. **善用调试**：遇到问题时先截图分析状态

---

**💡 记住：`browser_snapshot()` 是你最好的朋友！** 🎯