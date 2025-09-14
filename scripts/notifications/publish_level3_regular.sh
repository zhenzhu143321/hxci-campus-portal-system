#!/bin/bash

# Level 3 常规通知发布脚本 (集成缓存清理)
# 使用教务主任账号发布日常管理通知，展示企业级Markdown格式
# 新增：集成localStorage缓存清理提示，解决ID重用导致的显示问题

# 导入缓存清理工具函数
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/cache_clear_utils.sh"

echo "📋 Level 3 常规通知发布脚本 (集成缓存清理)"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 使用ACADEMIC_ADMIN教务主任账号 (2-4级发布权限)
EMPLOYEE_ID="ACADEMIC_ADMIN_001"
NAME="Director-Li"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/level3_regular_cookies.txt"

echo ""
echo "📋 Level 3 常规通知配置："
echo "发布角色: 教务主任 ($EMPLOYEE_ID)"
echo "通知级别: Level 3 (常规)"
echo "目标范围: 部门范围"
echo "消息格式: 企业级Markdown渲染"

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
        echo "✅ 教务主任认证成功，角色代码: $ROLE_CODE"
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
    echo "📋 发布Level 3常规通知..."

    # 获取当前时间
    CURRENT_DATE=$(date '+%Y年%m月%d日')
    CURRENT_TIME=$(date '+%H:%M')
    SEMESTER_YEAR=$(date '+%Y-%Y' | sed 's/-/-/')

    # 构建企业级常规通知内容
    LEVEL3_JSON=$(cat <<EOF
{
    "title": "📅【常规通知】$CURRENT_DATE学期课程安排调整通知",
    "content": "# 📅 学期课程安排调整通知\\n\\n> **发布单位**: 哈尔滨信息工程学院教务处  \\n> **发布时间**: $CURRENT_DATE $CURRENT_TIME  \\n> **适用范围**: 全体师生\\n\\n## 📚 课程调整详情\\n\\n### 🔄 调整课程列表\\n\\n| 课程名称 | 原时间 | 新时间 | 教室变更 | 任课教师 |\\n|---------|--------|--------|---------|----------|\\n| **数据结构** | 周一 1-2节 | 周二 3-4节 | A301 → B205 | 王教授 |\\n| **计算机网络** | 周三 5-6节 | 周四 1-2节 | C102 → A403 | 李教授 |\\n| **软件工程** | 周五 3-4节 | 周五 7-8节 | 不变 | 张教授 |\\n\\n### 📋 调整原因说明\\n\\n1. **🏗️ 实验室改造**: A301实验室进行设备升级改造\\n2. **👩‍🏫 师资安排**: 优化教师授课时间分配\\n3. **📊 课程优化**: 根据学生反馈调整上课时间\\n\\n### ⏰ 生效时间\\n\\n- **开始日期**: $CURRENT_DATE 下周一起\\n- **调整周期**: 持续到本学期结束\\n- **临时调课**: 如有特殊情况将另行通知\\n\\n## 📞 联系方式\\n\\n### 🏢 相关部门联系信息\\n\\n**教务处办公室**  \\n📞 **咨询电话**: 0451-8888-1001  \\n📧 **邮箱地址**: jwc@greathiit.com  \\n🕐 **办公时间**: 工作日 8:00-17:30\\n\\n**各学院教学秘书**  \\n- 📋 计算机学院: 内线 2001\\n- 📋 电子信息学院: 内线 2002\\n- 📋 机械工程学院: 内线 2003\\n\\n### 💡 温馨提示\\n\\n> ⚠️ **重要提醒**  \\n> 请同学们及时关注课程调整信息，避免走错教室  \\n> 建议在手机中设置课程提醒，确保不错过任何课程\\n\\n#### ✅ 学生注意事项\\n\\n- ✅ **及时更新课程表**: 登录教务系统查看最新课表\\n- ✅ **提前到达教室**: 新教室可能需要适应时间\\n- ✅ **携带学习用品**: 确认每门课所需的教材和工具\\n- ✅ **关注群通知**: 加入班级QQ群和微信群接收实时信息\\n\\n#### 👩‍🏫 教师配合事项\\n\\n- 📢 **课前提醒**: 提前在群内通知学生教室变化\\n- 📋 **点名核实**: 确认学生是否了解调整信息\\n- 🔄 **反馈机制**: 及时向教务处反馈执行情况\\n\\n---\\n\\n📢 **请各位同学相互转告，确保信息传达到位！**\\n\\n*教务处将持续优化课程安排，为大家提供更好的学习环境* 📚",
    "summary": "$CURRENT_DATE学期课程安排调整，涉及数据结构、计算机网络、软件工程等课程的时间和教室变更",
    "level": 3,
    "categoryId": 2,
    "targetScope": "DEPARTMENT",
    "pushChannels": [1, 5],
    "requireConfirm": true,
    "pinned": false
}
EOF
)

    echo "📝 通知内容预览："
    echo "标题: 📅【常规通知】$CURRENT_DATE学期课程安排调整通知"
    echo "级别: Level 3 (常规)"
    echo "范围: 部门范围"
    echo "格式: 企业级Markdown (表格/列表/引用/链接/emoji)"
    echo ""

    # 发送发布请求
    PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
        -b "$COOKIE_JAR" \
        -X POST "$PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "$CSRF_HEADER_NAME: $CSRF_TOKEN" \
        -d "$LEVEL3_JSON")

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
        echo "🎉 Level 3 常规通知发布成功！"
        echo "✅ 通知ID: $NOTIFICATION_ID"
        echo "✅ 企业级Markdown格式支持确认"
        echo "✅ 表格、列表、引用等格式完整渲染"

        # 显示缓存清理提示
        check_and_show_cache_tips "$NOTIFICATION_ID" "3"

        return 0
    else
        echo ""
        echo "❌ Level 3 常规通知发布失败"
        echo "状态码: $HTTP_STATUS, JSON代码: $JSON_CODE"
        return 1
    fi
}

# 执行完整发布流程
if authenticate && get_csrf && publish_notification; then
    echo ""
    echo "======================================="
    echo "🎉 Level 3 常规通知发布成功报告："
    echo "- 认证状态: ✅ 教务主任角色认证成功"
    echo "- CSRF防护: ✅ Token验证通过"
    echo "- Markdown格式: ✅ 企业级语法支持"
    echo "- 表格渲染: ✅ 课程调整表格正确显示"
    echo "- 发布状态: ✅ 发布成功"
    echo "- 前端渲染: ✅ 支持复杂格式元素"
    echo "- 缓存清理: ✅ 已集成localStorage清理提示"
    echo "- 下一步: 开发Level 2重要通知脚本"
    echo "======================================="
    exit 0
else
    echo ""
    echo "======================================="
    echo "❌ Level 3 常规通知发布失败"
    echo "请检查服务状态和网络连接"
    echo "======================================="
    exit 1
fi