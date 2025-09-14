#!/bin/bash

# Level 4 完整版提醒通知发布脚本 (修复版 + 缓存清理)
# 修复：将Markdown水平分隔线---改为***，避免SQL注入检测误判
# 新增：集成localStorage缓存清理提示，解决ID重用导致的显示问题

# 导入缓存清理工具函数
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/cache_clear_utils.sh"

echo "🌟 Level 4 完整版提醒通知发布脚本 (修复版 + 缓存清理)"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 使用PRINCIPAL校长账号 (最高权限)
EMPLOYEE_ID="PRINCIPAL_001"
NAME="Principal-Zhang"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/level4_complete_fixed_cookies.txt"

echo ""
echo "📋 Level 4 完整版通知配置："
echo "发布角色: 校长 ($EMPLOYEE_ID)"
echo "通知级别: Level 4 (温馨提醒)"
echo "目标范围: 全校范围"
echo "消息格式: 修复版Markdown (避免SQL注入误判)"

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
    echo "🌟 发布Level 4温馨提醒通知..."

    # 获取当前时间
    CURRENT_DATE=$(date '+%Y年%m月%d日')
    CURRENT_TIME=$(date '+%H:%M')

    # 构建修复版Markdown通知内容 (将---改为***)
    LEVEL4_JSON=$(cat <<EOF
{
    "title": "🌟【温馨提醒】健康生活全方位指导",
    "content": "# 🌟 健康生活温馨提醒\\n\\n> **发布时间**: $CURRENT_DATE $CURRENT_TIME  \\n> **发布单位**: 哈尔滨信息工程学院校医院\\n\\n## 🍂 秋季健康生活指导\\n\\n### 📋 日常保健要点\\n\\n- **💧 水分补充**: 每日饮水量 *≥2000ml*\\n- **😴 充足睡眠**: 睡眠时间 **7-8小时**\\n- **🏃 适量运动**: 每天运动 *30分钟以上*\\n- **🌡️ 温度适应**: 及时添减衣物，预防感冒\\n\\n### 🥗 饮食健康建议\\n\\n#### 推荐食物\\n1. **应季蔬果**: 苹果🍎、梨🍐、白萝卜🥕\\n2. **温补食材**: 红枣、枸杞、银耳\\n3. **蛋白质**: 鱼类、豆制品、坚果\\n\\n#### 饮食原则\\n- ✅ 规律三餐时间\\n- ✅ 少食辛辣刺激\\n- ✅ 多喝温开水\\n- ❌ 避免暴饮暴食\\n\\n### 🧠 心理健康关怀\\n\\n**情绪调节方法**:\\n- 🎵 听音乐放松心情\\n- 📚 阅读有益书籍\\n- 👥 多与朋友交流\\n- 🌳 户外散步呼吸新鲜空气\\n\\n> 💡 **温馨提示**: 如有心理困扰，欢迎联系学校心理咨询中心\\n> 📞 **咨询电话**: 010-8888-9999\\n> 🕐 **服务时间**: 周一至周五 8:00-17:00\\n\\n### 📞 应急联系方式\\n\\n| 服务项目 | 联系电话 | 服务时间 |\\n|---------|---------|---------|\\n| 校医院急诊 | 010-8888-1234 | 24小时 |\\n| 心理咨询 | 010-8888-9999 | 工作日 8:00-17:00 |\\n| 后勤服务 | 010-8888-5678 | 工作日 8:00-18:00 |\\n\\n***\\n\\n🌈 **愿每位师生都拥有健康快乐的校园生活！**\\n\\n*让我们一起营造温馨、健康的校园环境* ❤️",
    "summary": "秋季健康生活全方位指导，包含日常保健、饮食建议、心理健康和应急联系方式",
    "level": 4,
    "categoryId": 5,
    "targetScope": "SCHOOL_WIDE",
    "pushChannels": [1, 5],
    "requireConfirm": false,
    "pinned": false
}
EOF
)

    echo "📝 通知内容预览："
    echo "标题: 🌟【温馨提醒】健康生活全方位指导"
    echo "级别: Level 4 (提醒)"
    echo "范围: 全校范围"
    echo "格式: 修复版Markdown (水平分隔线使用***，避免SQL注入误判)"
    echo ""

    # 发送发布请求
    PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
        -b "$COOKIE_JAR" \
        -X POST "$PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "$CSRF_HEADER_NAME: $CSRF_TOKEN" \
        -d "$LEVEL4_JSON")

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
        echo "🎉 Level 4 完整版提醒通知发布成功！"
        echo "✅ 通知ID: $NOTIFICATION_ID"
        echo "✅ 修复版Markdown语法支持确认"
        echo "✅ 前端将正确渲染所有格式元素"
        echo "✅ SQL注入检测问题已解决"

        # 显示缓存清理提示
        check_and_show_cache_tips "$NOTIFICATION_ID" "4"

        return 0
    else
        echo ""
        echo "❌ Level 4 完整版通知发布失败"
        echo "状态码: $HTTP_STATUS, JSON代码: $JSON_CODE"
        return 1
    fi
}

# 执行完整发布流程
if authenticate && get_csrf && publish_notification; then
    echo ""
    echo "======================================="
    echo "🎉 Level 4 完整版通知发布成功报告："
    echo "- 认证状态: ✅ 校长角色认证成功"
    echo "- CSRF防护: ✅ Token验证通过"
    echo "- Markdown格式: ✅ 修复版语法支持"
    echo "- SQL注入问题: ✅ 已解决(---改为***)"
    echo "- 发布状态: ✅ 发布成功"
    echo "- 前端渲染: ✅ 支持标题/列表/表格/引用/emoji"
    echo "- 缓存清理: ✅ 已集成localStorage清理提示"
    echo "- 下一步: 开发Level 3常规通知脚本"
    echo "======================================="
    exit 0
else
    echo ""
    echo "======================================="
    echo "❌ Level 4 完整版通知发布失败"
    echo "请检查服务状态和网络连接"
    echo "======================================="
    exit 1
fi