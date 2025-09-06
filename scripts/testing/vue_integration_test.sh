#!/bin/bash
echo "🎯 Vue前端集成测试脚本"
echo "============================="

# 检查Node.js环境
echo "📦 Node.js环境检查:"
node --version
npm --version

echo ""
echo "📂 切换到Vue项目目录:"
cd /opt/hxci-campus-portal/hxci-campus-portal-system/hxci-campus-portal

echo ""
echo "🔧 检查依赖安装状态:"
if [ -d "node_modules" ]; then
    echo "✅ node_modules已存在"
else
    echo "❌ node_modules不存在，需要运行: npm install"
fi

echo ""
echo "🔗 Vue前端API配置检查:"
echo "待办API配置:"
grep -n "admin-api/test/todo-new" src/api/todo.ts | head -3
echo ""
echo "通知API配置:"  
grep -n "admin-api/test/notification" src/api/notification.ts | head -3

echo ""
echo "🚀 Vue开发服务器启动指引:"
echo "请执行以下命令启动Vue前端服务："
echo "  cd /opt/hxci-campus-portal/hxci-campus-portal-system/hxci-campus-portal"
echo "  npm run dev"
echo ""
echo "启动后访问：http://localhost:3001 或 http://localhost:3000"

echo ""
echo "🧪 前端功能测试清单:"
echo "□ 1. 登录认证测试 - 使用PRINCIPAL_001/Principal-Zhang/admin123"
echo "□ 2. 待办列表加载 - 验证API数据正确显示"  
echo "□ 3. 权限过滤测试 - 切换不同角色验证数据差异"
echo "□ 4. 功能交互测试 - 完成待办、查看统计等操作"
echo "□ 5. UI响应测试 - 验证界面交互流畅性"

echo ""
echo "✅ 脚本执行完成，请按照指引启动Vue服务进行集成测试"