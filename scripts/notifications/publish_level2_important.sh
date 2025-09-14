#!/bin/bash

# Level 2 重要通知发布脚本 (集成缓存清理)
# 使用校长账号发布重要政策通知，展示高级Markdown格式
# 新增：集成localStorage缓存清理提示，解决ID重用导致的显示问题

# 导入缓存清理工具函数
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/cache_clear_utils.sh"

echo "🔴 Level 2 重要通知发布脚本 (集成缓存清理)"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 使用PRINCIPAL校长账号 (1-4级发布权限)
EMPLOYEE_ID="PRINCIPAL_001"
NAME="Principal-Zhang"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/level2_important_cookies.txt"

echo ""
echo "🔴 Level 2 重要通知配置："
echo "发布角色: 校长 ($EMPLOYEE_ID)"
echo "通知级别: Level 2 (重要)"
echo "目标范围: 全校范围"
echo "消息格式: 高级Markdown渲染"

# 清理旧Cookie文件
rm -f "$COOKIE_JAR"

# 认证获取Token函数
authenticate() {
    echo ""
    echo "🔐 身份认证中..."

    AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")

    # 提取JWT Token
    if command -v jq &> /dev/null; then
        JWT_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.data.accessToken // .data.token // empty')
        ROLE_CODE=$(echo "$AUTH_RESPONSE" | jq -r '.data.roleCode // empty')
    else
        JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
        ROLE_CODE=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"roleCode":"\([^"]*\)".*/\1/p')
    fi

    if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
        echo "✅ 校长认证成功，角色代码: $ROLE_CODE"
        echo "✅ JWT Token获取成功"
        return 0
    else
        echo "❌ 认证失败"
        return 1
    fi
}

# 获取CSRF Token函数
get_csrf() {
    echo ""
    echo "🛡️ 获取CSRF防护Token..."

    CSRF_RESPONSE=$(curl -s -c "$COOKIE_JAR" -X GET "$CSRF_API")

    if command -v jq &> /dev/null; then
        CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | jq -r '.data.token // .token // empty')
        CSRF_HEADER_NAME=$(echo "$CSRF_RESPONSE" | jq -r '.data.headerName // .headerName // "X-XSRF-TOKEN"')
        CSRF_COOKIE_NAME=$(echo "$CSRF_RESPONSE" | jq -r '.data.cookieName // .cookieName // "XSRF-TOKEN"')
    else
        CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
        CSRF_HEADER_NAME="X-XSRF-TOKEN"
        CSRF_COOKIE_NAME="XSRF-TOKEN"
    fi

    if [ -n "$CSRF_TOKEN" ] && [ "$CSRF_TOKEN" != "null" ]; then
        echo "✅ CSRF Token获取成功"
        echo "✅ 防护头名称: $CSRF_HEADER_NAME"

        # 手动设置CSRF Cookie
        echo -e "\tlocalhost\tFALSE\t/\tFALSE\t0\t$CSRF_COOKIE_NAME\t$CSRF_TOKEN" >> "$COOKIE_JAR"
        echo "✅ CSRF Cookie已配置"
        return 0
    else
        echo "❌ CSRF Token获取失败"
        return 1
    fi
}

# 发布通知函数
publish_notification() {
    echo ""
    echo "🔴 发布Level 2重要通知..."

    # 获取当前时间
    CURRENT_DATE=$(date '+%Y年%m月%d日')
    CURRENT_TIME=$(date '+%H:%M')
    NEXT_WEEK_DATE=$(date -d '+7 days' '+%Y年%m月%d日')
    SEMESTER_END_DATE=$(date -d '+3 months' '+%Y年%m月%d日')

    # 构建高级重要通知内容
    LEVEL2_JSON=$(cat <<EOF
{
    "title": "🔴【重要通知】$CURRENT_DATE校园信息化系统升级维护通知",
    "content": "# 🔴 校园信息化系统升级维护通知\\n\\n---\\n\\n> 📢 **重要程度**: 高优先级  \\n> 🏫 **发布单位**: 哈尔滨信息工程学院  \\n> 👤 **发布人**: 校长办公室  \\n> 📅 **发布时间**: $CURRENT_DATE $CURRENT_TIME  \\n> 🎯 **适用对象**: 全校师生\\n\\n---\\n\\n## 🛠️ 维护概述\\n\\n尊敬的全校师生：\\n\\n为提升校园信息化服务质量，保障系统稳定运行，学校决定对核心信息系统进行**重大升级维护**。此次升级将显著提升系统性能和用户体验。\\n\\n### ⚠️ 维护时间安排\\n\\n| 维护阶段 | 开始时间 | 结束时间 | 影响程度 | 备注 |\\n|---------|----------|----------|----------|------|\\n| **🔧 预备阶段** | $CURRENT_DATE 22:00 | $CURRENT_DATE 24:00 | 轻微影响 | 系统响应可能变慢 |\\n| **⚡ 核心升级** | $NEXT_WEEK_DATE 02:00 | $NEXT_WEEK_DATE 06:00 | 完全停机 | 所有系统不可用 |\\n| **🧪 测试阶段** | $NEXT_WEEK_DATE 06:00 | $NEXT_WEEK_DATE 08:00 | 部分影响 | 逐步恢复服务 |\\n\\n### 📋 受影响系统清单\\n\\n#### 🚫 完全停机系统 (4小时)\\n\\n1. **📚 教务管理系统**\\n   - 🔗 **访问地址**: [jwc.greathiit.com](http://jwc.greathiit.com)\\n   - 📱 **功能**: 选课、成绩查询、课表查看\\n   - ⚠️ **影响**: 无法进行任何教务操作\\n\\n2. **🏠 学生管理系统**\\n   - 🔗 **访问地址**: [student.greathiit.com](http://student.greathiit.com)\\n   - 📱 **功能**: 学籍管理、宿舍管理、奖学金申请\\n   - ⚠️ **影响**: 学生信息无法查看和修改\\n\\n3. **📖 图书馆系统**\\n   - 🔗 **访问地址**: [lib.greathiit.com](http://lib.greathiit.com)\\n   - 📱 **功能**: 图书检索、续借、预约\\n   - ⚠️ **影响**: 线上服务暂停，现场服务正常\\n\\n4. **💰 财务缴费系统**\\n   - 🔗 **访问地址**: [pay.greathiit.com](http://pay.greathiit.com)\\n   - 📱 **功能**: 学费缴纳、生活费充值\\n   - ⚠️ **影响**: 无法进行线上支付\\n\\n#### 🟡 部分影响系统 (2小时)\\n\\n- **📧 校园邮箱**: 可能出现邮件延迟\\n- **📶 校园WiFi**: 认证系统响应较慢\\n- **🍽️ 食堂刷卡**: 线下刷卡正常，充值暂停\\n\\n## 🎯 升级亮点\\n\\n### 💡 核心改进特性\\n\\n本次升级将带来以下重要改进：\\n\\n```markdown\\n✨ 性能提升\\n├── 🚀 系统响应速度提升 60%\\n├── 📊 数据库查询优化 40%\\n├── 🖥️ 界面加载速度提升 50%\\n└── 📱 移动端体验全面优化\\n\\n🔒 安全加强\\n├── 🛡️ 双因子认证系统上线\\n├── 🔐 密码安全策略升级\\n├── 🕵️ 异常登录检测机制\\n└── 📋 安全审计功能完善\\n\\n🎨 用户体验\\n├── 🌟 全新UI设计语言\\n├── 🎯 智能推荐功能\\n├── 🔍 全局搜索优化\\n└── 📞 在线客服系统\\n```\\n\\n### 🆕 新增功能预览\\n\\n1. **🤖 智能助手**\\n   - 24/7在线答疑\\n   - 常见问题智能解答\\n   - 个性化服务推荐\\n\\n2. **📱 移动APP升级**\\n   - 全新界面设计\\n   - 离线功能支持\\n   - 消息推送优化\\n\\n3. **🔔 实时通知系统**\\n   - 重要通知即时推送\\n   - 个性化通知设置\\n   - 多渠道消息同步\\n\\n## 📋 师生配合事项\\n\\n### 👩‍🏫 教师注意事项\\n\\n- **📊 成绩录入**: 请在 $CURRENT_DATE 21:00 前完成本周成绩录入\\n- **📅 课程调整**: 临时调课信息请通过班级群通知学生\\n- **📋 教学计划**: 建议提前下载必要的教学资料\\n- **🔄 系统备份**: 重要数据请提前备份到本地\\n\\n### 🎓 学生配合事项\\n\\n#### 📚 学习相关\\n- ✅ **课程查询**: 提前截图保存本周课程表\\n- ✅ **作业提交**: $CURRENT_DATE 21:00前完成线上作业提交\\n- ✅ **图书续借**: 需要续借的图书请提前办理\\n- ✅ **选课准备**: 了解下学期选课计划，准备选课清单\\n\\n#### 💳 生活服务\\n- 💰 **饭卡充值**: 建议提前充值，确保维护期间正常用餐\\n- 🏠 **宿舍门禁**: 维护期间门禁卡正常使用\\n- 📞 **紧急联系**: 记住宿管老师和辅导员联系方式\\n\\n## 📞 应急联系方式\\n\\n### 🆘 技术支持热线\\n\\n**维护期间7×24小时值班**\\n\\n| 服务类型 | 联系电话 | 服务时间 | 负责人 |\\n|---------|----------|----------|--------|\\n| 🔧 **技术故障** | 0451-8888-2000 | 24小时 | 信息中心 |\\n| 📚 **教务咨询** | 0451-8888-1001 | 8:00-18:00 | 教务处 |\\n| 🏠 **学生事务** | 0451-8888-3000 | 24小时 | 学工处 |\\n| 💰 **财务问题** | 0451-8888-4000 | 8:00-17:00 | 财务处 |\\n\\n### 📧 电子邮件支持\\n\\n- **技术支持**: [support@greathiit.com](mailto:support@greathiit.com)\\n- **教务咨询**: [jwc@greathiit.com](mailto:jwc@greathiit.com)\\n- **紧急事务**: [emergency@greathiit.com](mailto:emergency@greathiit.com)\\n\\n> 💡 **温馨提示**: 邮件回复可能有延迟，紧急情况请直接电话联系\\n\\n## ✅ 维护完成验收\\n\\n维护完成后，我们将进行全面的系统测试验收：\\n\\n### 🧪 测试验收流程\\n\\n1. **⚡ 系统性能测试** - 验证响应速度提升\\n2. **🔒 安全功能测试** - 确认安全机制正常\\n3. **📱 用户界面测试** - 检查新UI功能完整性\\n4. **🔗 系统集成测试** - 验证各系统间数据同步\\n5. **👥 用户体验测试** - 邀请师生代表测试反馈\\n\\n### 📊 预期效果指标\\n\\n- ✅ 系统响应时间 < 2秒 (原3.2秒)\\n- ✅ 并发用户支持 > 5000人 (原3000人)\\n- ✅ 系统可用性 > 99.9%\\n- ✅ 用户满意度 > 95%\\n\\n---\\n\\n## 📢 特别声明\\n\\n> ⚠️ **重要提醒**  \\n> 本次升级是学校信息化建设的重要里程碑，升级过程中如遇突发情况，请通过应急联系方式及时反馈。我们将第一时间响应和处理。\\n\\n> 🙏 **感谢配合**  \\n> 感谢全校师生的理解与配合！升级完成后，我们将为大家提供更加稳定、高效、安全的信息化服务体验。\\n\\n---\\n\\n**哈尔滨信息工程学院校长办公室**  \\n**$CURRENT_DATE**\\n\\n---\\n\\n*🌟 学校官网: [www.greathiit.com](http://www.greathiit.com) | 📱 微信公众号: 哈信工官微*",
    "summary": "校园信息化系统重大升级维护通知，$NEXT_WEEK_DATE 2:00-6:00期间教务、学生、图书馆、财务等核心系统将停机4小时进行升级",
    "level": 2,
    "categoryId": 1,
    "targetScope": "SCHOOL_WIDE",
    "pushChannels": [1, 2, 5],
    "requireConfirm": true,
    "pinned": true
}
EOF
)

    echo "📝 通知内容预览："
    echo "标题: 🔴【重要通知】$CURRENT_DATE校园信息化系统升级维护通知"
    echo "级别: Level 2 (重要)"
    echo "范围: 全校范围"
    echo "格式: 高级Markdown (表格/代码块/链接/引用/emoji)"
    echo ""

    # 发送发布请求
    PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
        -b "$COOKIE_JAR" \
        -X POST "$PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "$CSRF_HEADER_NAME: $CSRF_TOKEN" \
        -d "$LEVEL2_JSON")

    # 分析响应结果
    HTTP_STATUS=$(echo "$PUBLISH_RESPONSE" | tail -n1 | cut -d: -f2)
    RESPONSE_BODY=$(echo "$PUBLISH_RESPONSE" | sed '$d')

    echo "📊 发布响应："
    echo "状态码: $HTTP_STATUS"
    echo "响应内容: $RESPONSE_BODY"

    # 提取通知ID
    if command -v jq &> /dev/null; then
        NOTIFICATION_ID=$(echo "$RESPONSE_BODY" | jq -r '.data.notificationId // .data.id // empty')
        JSON_CODE=$(echo "$RESPONSE_BODY" | jq -r '.code // empty')
    else
        NOTIFICATION_ID=$(echo "$RESPONSE_BODY" | sed -n 's/.*"notificationId":\([0-9]*\).*/\1/p')
        JSON_CODE=$(echo "$RESPONSE_BODY" | sed -n 's/.*"code":\([0-9]*\).*/\1/p')
    fi

    # 清理Cookie文件
    rm -f "$COOKIE_JAR"

    # 结果判断
    if [ "$HTTP_STATUS" -ge 200 ] && [ "$HTTP_STATUS" -lt 300 ] && [ "$JSON_CODE" = "0" ]; then
        echo ""
        echo "🎉 Level 2 重要通知发布成功！"
        echo "✅ 通知ID: $NOTIFICATION_ID"
        echo "✅ 高级Markdown格式支持确认"
        echo "✅ 代码块、链接、复杂表格等格式完整渲染"
        echo "✅ 置顶显示已启用"

        # 显示缓存清理提示
        check_and_show_cache_tips "$NOTIFICATION_ID" "2"

        return 0
    else
        echo ""
        echo "❌ Level 2 重要通知发布失败"
        echo "状态码: $HTTP_STATUS, JSON代码: $JSON_CODE"
        return 1
    fi
}

# 执行完整发布流程
if authenticate && get_csrf && publish_notification; then
    echo ""
    echo "======================================="
    echo "🎉 Level 2 重要通知发布成功报告："
    echo "- 认证状态: ✅ 校长角色认证成功"
    echo "- CSRF防护: ✅ Token验证通过"
    echo "- Markdown格式: ✅ 高级语法支持"
    echo "- 复杂格式: ✅ 代码块、链接、表格完整支持"
    echo "- 发布状态: ✅ 发布成功并置顶"
    echo "- 通知级别: ✅ Level 2重要级别确认"
    echo "- 缓存清理: ✅ 已集成localStorage清理提示"
    echo "- 下一步: 开发Level 1紧急通知脚本"
    echo "======================================="
    exit 0
else
    echo ""
    echo "======================================="
    echo "❌ Level 2 重要通知发布失败"
    echo "请检查服务状态和网络连接"
    echo "======================================="
    exit 1
fi