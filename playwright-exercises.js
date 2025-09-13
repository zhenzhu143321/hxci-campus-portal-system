// 🎓 Playwright MCP工具学习练习
// 新Claude实例可以逐步练习这些示例

const exercises = {
  
  // 练习1：基础导航 ⭐ 难度：入门
  exercise1_basic_navigation: `
    目标：访问example.com并截图
    
    步骤：
    1. mcp__playwright__browser_navigate({ url: "https://example.com" })
    2. mcp__playwright__browser_take_screenshot({ filename: "ex1-example.png" })
    3. mcp__playwright__browser_snapshot()
    
    预期结果：
    - 成功访问页面
    - 生成截图文件
    - 获得页面结构快照
  `,

  // 练习2：页面分析 ⭐⭐ 难度：初级
  exercise2_page_analysis: `
    目标：分析GitHub首页结构
    
    步骤：
    1. mcp__playwright__browser_navigate({ url: "https://github.com" })
    2. mcp__playwright__browser_snapshot()
    3. 分析返回的页面结构，找到：
       - 导航菜单
       - 搜索框
       - 登录按钮
    4. mcp__playwright__browser_take_screenshot({ filename: "ex2-github.png" })
    
    学习要点：
    - 理解页面结构的yaml格式
    - 学会识别可交互元素 (cursor=pointer)
    - 理解元素引用 (ref=eXX) 的概念
  `,

  // 练习3：简单交互 ⭐⭐ 难度：初级
  exercise3_simple_interaction: `
    目标：点击example.com上的链接
    
    步骤：
    1. mcp__playwright__browser_navigate({ url: "https://example.com" })
    2. mcp__playwright__browser_snapshot() // 找到"More information..."链接的ref
    3. mcp__playwright__browser_click({
         element: "More information link",
         ref: "e6"  // 根据实际快照调整
       })
    4. mcp__playwright__browser_take_screenshot({ filename: "ex3-result.png" })
    
    学习要点：
    - 先快照后操作的工作流程
    - 正确使用元素引用
    - 验证操作结果
  `,

  // 练习4：搜索操作 ⭐⭐⭐ 难度：中级
  exercise4_search_operation: `
    目标：在DuckDuckGo搜索引擎进行搜索
    
    步骤：
    1. mcp__playwright__browser_navigate({ url: "https://duckduckgo.com" })
    2. mcp__playwright__browser_snapshot() // 找到搜索框
    3. mcp__playwright__browser_type({
         element: "search input",
         ref: "eXX", // 根据快照确定
         text: "Playwright automation"
       })
    4. mcp__playwright__browser_click({
         element: "search button",
         ref: "eXX"  // 根据快照确定
       })
    5. 等待结果加载
    6. mcp__playwright__browser_take_screenshot({ filename: "ex4-search-results.png" })
    
    学习要点：
    - 文本输入操作
    - 搜索表单提交
    - 结果页面处理
  `,

  // 练习5：表单填写 ⭐⭐⭐⭐ 难度：中高级
  exercise5_form_filling: `
    目标：使用批量表单填写功能
    
    准备：找一个有表单的测试网站，如 httpbin.org/forms/post
    
    步骤：
    1. mcp__playwright__browser_navigate({ url: "http://httpbin.org/forms/post" })
    2. mcp__playwright__browser_snapshot() // 分析表单结构
    3. mcp__playwright__browser_fill_form({
         fields: [
           { name: "custname", type: "textbox", ref: "eXX", value: "张三" },
           { name: "custtel", type: "textbox", ref: "eXX", value: "13800138000" },
           { name: "custemail", type: "textbox", ref: "eXX", value: "test@example.com" },
           { name: "size", type: "radio", ref: "eXX", value: "medium" }
         ]
       })
    4. mcp__playwright__browser_take_screenshot({ filename: "ex5-filled-form.png" })
    5. 可选：提交表单测试
    
    学习要点：
    - 批量表单操作
    - 不同输入类型处理
    - 单选框/复选框操作
  `,

  // 练习6：等待和时序 ⭐⭐⭐ 难度：中级
  exercise6_timing_and_waits: `
    目标：处理动态加载内容
    
    步骤：
    1. mcp__playwright__browser_navigate({ url: "https://httpbin.org/delay/3" })
    2. mcp__playwright__browser_wait_for({ time: 4 }) // 等待4秒
    3. mcp__playwright__browser_take_screenshot({ filename: "ex6-after-delay.png" })
    
    或者等待特定文本：
    1. mcp__playwright__browser_navigate({ url: "某个有加载状态的网站" })
    2. mcp__playwright__browser_wait_for({ text: "加载完成" })
    3. 进行后续操作
    
    学习要点：
    - 时间等待策略
    - 文本出现等待
    - 处理异步加载
  `,

  // 练习7：多页面操作 ⭐⭐⭐⭐⭐ 难度：高级
  exercise7_multi_page: `
    目标：在多个标签页间切换
    
    步骤：
    1. mcp__playwright__browser_navigate({ url: "https://example.com" })
    2. mcp__playwright__browser_tabs({ action: "new" }) // 新建标签页
    3. mcp__playwright__browser_navigate({ url: "https://github.com" })
    4. mcp__playwright__browser_tabs({ action: "list" }) // 查看所有标签页
    5. mcp__playwright__browser_tabs({ action: "select", index: 0 }) // 切换回第一个
    6. mcp__playwright__browser_take_screenshot({ filename: "ex7-tab-switch.png" })
    
    学习要点：
    - 标签页管理
    - 页面切换策略
    - 多任务处理
  `,

  // 练习8：综合应用 ⭐⭐⭐⭐⭐ 难度：高级
  exercise8_comprehensive: `
    目标：完成一个完整的网页自动化任务
    
    场景：访问新闻网站，搜索特定内容，截图保存
    
    步骤：
    1. 访问新闻网站首页
    2. 找到搜索功能
    3. 输入搜索关键词
    4. 查看搜索结果
    5. 点击第一个结果
    6. 阅读文章内容
    7. 截图保存关键信息
    8. 返回搜索结果页
    9. 截图保存搜索结果
    
    学习要点：
    - 完整的自动化工作流
    - 错误处理
    - 状态管理
    - 结果验证
  `
};

// 练习验证清单
const checkList = {
  basic_skills: [
    "✅ 能够导航到任何网页",
    "✅ 能够截取高质量截图", 
    "✅ 能够获取页面结构快照",
    "✅ 理解元素引用(ref)概念",
    "✅ 能够描述页面元素"
  ],
  
  interaction_skills: [
    "✅ 能够点击按钮和链接",
    "✅ 能够在输入框中输入文本",
    "✅ 能够选择下拉选项",
    "✅ 能够填写完整表单",
    "✅ 能够处理复选框和单选框"
  ],
  
  advanced_skills: [
    "✅ 能够等待页面加载",
    "✅ 能够处理动态内容",
    "✅ 能够管理多个标签页", 
    "✅ 能够调整浏览器窗口",
    "✅ 能够处理页面跳转"
  ],
  
  expert_skills: [
    "✅ 能够执行JavaScript代码",
    "✅ 能够处理文件上传",
    "✅ 能够分析网络请求",
    "✅ 能够处理弹窗和警告",
    "✅ 能够进行完整的端到端测试"
  ]
};

console.log("🎓 Playwright MCP工具练习集已加载");
console.log("📚 使用方法：");
console.log("   1. 从exercise1开始，逐步练习");
console.log("   2. 每完成一个练习，截图保存结果");  
console.log("   3. 遇到问题时，先运行browser_snapshot()分析页面");
console.log("   4. 完成所有练习后，检查技能清单");
console.log("");
console.log("🚀 开始你的Playwright自动化学习之旅！");