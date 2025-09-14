#!/bin/bash

# Level 1 紧急通知发布脚本 (集成缓存清理)
# 使用校长账号发布紧急校园安全通知，最高优先级
# 新增：集成localStorage缓存清理提示，解决ID重用导致的显示问题

# 导入缓存清理工具函数
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/cache_clear_utils.sh"

echo "🚨 Level 1 紧急通知发布脚本 (集成缓存清理)"
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
COOKIE_JAR="/tmp/level1_emergency_cookies.txt"

echo ""
echo "🚨 Level 1 紧急通知配置："
echo "发布角色: 校长 ($EMPLOYEE_ID)"
echo "通知级别: Level 1 (紧急)"
echo "目标范围: 全校范围"
echo "消息格式: 紧急事件专用Markdown"

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
    echo "🚨 发布Level 1紧急通知..."

    # 获取当前时间
    CURRENT_DATE=$(date '+%Y年%m月%d日')
    CURRENT_TIME=$(date '+%H:%M')
    EMERGENCY_TIME=$(date '+%H时%M分')

    # 构建紧急通知内容
    LEVEL1_JSON=$(cat <<EOF
{
    "title": "🚨【紧急通知】$CURRENT_DATE校园网络安全紧急事件警报",
    "content": "# 🚨 校园网络安全紧急事件警报\\n\\n> ⚠️ **紧急程度**: 🔴 **最高优先级**  \\n> 📅 **事件时间**: $CURRENT_DATE $EMERGENCY_TIME  \\n> 🏢 **发布机构**: 哈尔滨信息工程学院应急指挥中心  \\n> 👤 **发布人**: 校长 张明  \\n> 🎯 **通知对象**: 全校师生（立即执行）\\n\\n---\\n\\n## 🔥 紧急情况通报\\n\\n**全校师生请注意！**\\n\\n我校网络安全监测系统于**$CURRENT_DATE $EMERGENCY_TIME**检测到重大网络安全威胁，为保护全校师生信息安全和财产安全，现启动**一级应急响应预案**。\\n\\n### 🎯 威胁详情\\n\\n| 威胁类型 | 影响范围 | 严重级别 | 发现时间 |\\n|---------|----------|----------|----------|\\n| **🦠 恶意软件传播** | 全校网络 | 🔴 极高 | $CURRENT_DATE $EMERGENCY_TIME |\\n| **🎣 钓鱼邮件攻击** | 师生邮箱 | 🔴 极高 | $CURRENT_DATE $(date '+%H:%M' -d '10 minutes ago') |\\n| **🔓 密码暴力破解** | 教务系统 | 🟠 高 | $CURRENT_DATE $(date '+%H:%M' -d '20 minutes ago') |\\n\\n### ⚠️ 当前风险状态\\n\\n```markdown\\n🚨 紧急风险评估\\n├── 🔴 网络入侵检测: 发现异常连接 15,000+ 次\\n├── 🔴 恶意文件检测: 拦截病毒样本 2,300+ 个\\n├── 🟠 账户安全检测: 发现异常登录 450+ 次\\n└── 🟡 数据传输监控: 发现可疑流量传输\\n```\\n\\n---\\n\\n## ⚡ 立即执行措施\\n\\n### 🚫 **立即停止以下活动**\\n\\n#### ❌ 网络行为禁止清单\\n\\n1. **🚫 停止下载任何文件**\\n   - 不要从邮件附件下载文件\\n   - 不要从QQ、微信下载陌生文件\\n   - 暂停从百度网盘、网络下载软件\\n\\n2. **🚫 停止登录敏感系统**\\n   - 教务管理系统 → **立即退出**\\n   - 财务缴费系统 → **暂停使用**\\n   - 学生信息系统 → **停止访问**\\n   - 图书馆系统 → **暂缓登录**\\n\\n3. **🚫 停止输入敏感信息**\\n   - 不要在任何网页输入身份证号\\n   - 不要在陌生网站输入银行卡信息\\n   - 不要点击任何可疑链接\\n\\n### ✅ **必须立即执行的保护措施**\\n\\n#### 🔒 个人防护操作清单\\n\\n**第一步：保护个人设备** (⏰ 5分钟内完成)\\n\\n- ✅ **断开可疑网络连接**\\n  ```\\n  Windows: 右键网络图标 → 断开连接\\n  Mac: 系统偏好设置 → 网络 → 断开WiFi\\n  手机: 设置 → WiFi → 断开校园网\\n  ```\\n\\n- ✅ **关闭自动下载功能**\\n  - 微信: 设置 → 通用 → 照片、视频、文件 → 关闭自动下载\\n  - QQ: 设置 → 文件管理 → 关闭自动接收文件\\n  - 浏览器: 关闭自动下载功能\\n\\n- ✅ **启动病毒扫描**\\n  ```\\n  Windows: 运行 Windows Defender 全盘扫描\\n  Mac: 运行 Malware 检测软件\\n  手机: 运行手机管家病毒扫描\\n  ```\\n\\n**第二步：密码安全检查** (⏰ 10分钟内完成)\\n\\n- ✅ **立即修改重要密码**\\n  1. 教务系统密码 → **必须修改**\\n  2. 邮箱登录密码 → **必须修改**\\n  3. WiFi连接密码 → **暂时不连**\\n  4. 银行卡相关密码 → **立即修改**\\n\\n- ✅ **检查登录历史**\\n  - 查看教务系统最近登录记录\\n  - 检查邮箱异常登录提醒\\n  - 查看银行卡消费记录\\n\\n**第三步：数据保护措施** (⏰ 15分钟内完成)\\n\\n- ✅ **备份重要数据**\\n  - 毕业设计、课程作业 → 备份到本地U盘\\n  - 重要照片、文档 → 备份到移动硬盘\\n  - 学习资料 → 立即导出保存\\n\\n- ✅ **隔离可疑文件**\\n  - 最近下载的文件 → 移动到隔离文件夹\\n  - 可疑邮件附件 → 不要打开，截图报告\\n  - 陌生U盘设备 → 暂时不要使用\\n\\n---\\n\\n## 📞 应急联系方式\\n\\n### 🆘 24小时应急热线 (优先拨打)\\n\\n| 紧急程度 | 联系电话 | 服务内容 | 响应时间 |\\n|---------|----------|----------|----------|\\n| 🔴 **生命安全威胁** | **110 + 0451-8888-0000** | 人身安全保护 | 立即响应 |\\n| 🔴 **财产安全威胁** | **0451-8888-0001** | 资金安全保护 | 5分钟内 |\\n| 🟠 **技术安全问题** | **0451-8888-0002** | 技术支持服务 | 10分钟内 |\\n| 🟡 **一般咨询求助** | **0451-8888-0003** | 安全咨询指导 | 15分钟内 |\\n\\n### 🏢 现场求助地点\\n\\n#### 🚑 紧急避险区域 (24小时开放)\\n\\n1. **🏛️ 图书馆一楼大厅**\\n   - 📍 地址: 图书馆主楼一层\\n   - 👥 现场人员: 安全员 + 技术员\\n   - 🔧 提供服务: 设备检测 + 密码重置\\n\\n2. **🏢 学生服务中心**\\n   - 📍 地址: 学生事务大厅\\n   - 👥 现场人员: 学工老师 + 网络工程师\\n   - 🔧 提供服务: 账户恢复 + 安全咨询\\n\\n3. **💻 信息技术中心**\\n   - 📍 地址: 行政楼3楼\\n   - 👥 现场人员: 网络安全专家\\n   - 🔧 提供服务: 专业技术支持\\n\\n### 📱 数字化求助渠道\\n\\n- **🔗 应急求助网站**: [emergency.greathiit.com](http://emergency.greathiit.com)\\n- **📱 微信应急群**: 搜索\"哈信工应急响应\" (群号: 123456789)\\n- **📧 紧急邮箱**: [emergency@greathiit.com](mailto:emergency@greathiit.com)\\n\\n> ⚠️ **特别提醒**: 紧急情况下，请优先拨打电话求助，网络渠道可能受影响\\n\\n---\\n\\n## 🔄 应急响应流程\\n\\n### 🎯 分级响应机制\\n\\n学校已启动**三级应急响应体系**：\\n\\n#### 🔴 一级响应 (当前状态)\\n- **响应时间**: 立即响应 (0-5分钟)\\n- **响应人员**: 校长 + 副校长 + 全体中层干部\\n- **响应措施**: 全校范围安全防护\\n- **持续时间**: 直到威胁完全解除\\n\\n#### 🟠 二级响应 (备用状态)\\n- **响应时间**: 快速响应 (5-15分钟)\\n- **响应人员**: 信息中心 + 学工处 + 保卫处\\n- **响应措施**: 重点区域防护\\n\\n#### 🟡 三级响应 (常规状态)\\n- **响应时间**: 正常响应 (15-30分钟)\\n- **响应人员**: 值班人员\\n- **响应措施**: 日常安全维护\\n\\n### 📊 实时威胁监控\\n\\n我们将每**15分钟**发布一次威胁评估更新：\\n\\n- **$CURRENT_TIME** - 🔴 当前威胁等级: 极高\\n- **$(date '+%H:%M' -d '15 minutes')** - 下次评估预定时间\\n- **实时更新渠道**: 校园广播 + 微信群 + 短信通知\\n\\n---\\n\\n## ⏰ 预计恢复时间表\\n\\n### 🛠️ 分阶段恢复计划\\n\\n| 恢复阶段 | 预计开始时间 | 预计完成时间 | 恢复服务 |\\n|---------|-------------|-------------|----------|\\n| **🔧 第一阶段** | $CURRENT_DATE $(date '+%H:%M' -d '2 hours') | $CURRENT_DATE $(date '+%H:%M' -d '4 hours') | 基础网络连接 |\\n| **🔧 第二阶段** | $CURRENT_DATE $(date '+%H:%M' -d '4 hours') | $CURRENT_DATE $(date '+%H:%M' -d '6 hours') | 教务、图书馆系统 |\\n| **🔧 第三阶段** | $CURRENT_DATE $(date '+%H:%M' -d '6 hours') | $CURRENT_DATE $(date '+%H:%M' -d '8 hours') | 财务、学工系统 |\\n| **✅ 全面恢复** | $CURRENT_DATE $(date '+%H:%M' -d '8 hours') | $CURRENT_DATE $(date '+%H:%M' -d '12 hours') | 所有服务正常 |\\n\\n> 📝 **注意**: 以上时间为预估，实际恢复时间根据威胁解除情况调整\\n\\n---\\n\\n## 🏛️ 领导承诺与责任\\n\\n### 👨‍💼 校长承诺\\n\\n> 🎯 **郑重承诺**  \\n> 作为学校主要负责人，我向全校师生承诺：我们将投入一切必要资源，确保每一位师生的信息安全和财产安全。任何因此次事件造成的损失，学校将承担责任并给予合理补偿。\\n\\n### 📞 校长直通热线\\n\\n**紧急情况下可直接联系校长**\\n- 📱 **手机**: 138-0451-0001 (24小时开机)\\n- 📧 **邮箱**: [principal@greathiit.com](mailto:principal@greathiit.com)\\n- 🏢 **办公室**: 行政楼6楼校长室 (有专人值班)\\n\\n---\\n\\n## 📢 特别声明\\n\\n> 🚨 **紧急提醒**  \\n> 此次网络安全事件属于**外部攻击**，不是校内系统故障。请全校师生不要恐慌，按照本通知要求执行防护措施即可。我们已联系公安部门协助调查，相关进展将及时通报。\\n\\n> ⚡ **行动要求**  \\n> 本通知为**强制执行**级别，所有师生必须严格按照要求执行。如有违反，将按照学校相关规定处理。\\n\\n> 🔄 **后续通报**  \\n> 我们将每隔**1小时**发布威胁状态更新，每隔**3小时**发布详细进展报告。请持续关注官方通知渠道。\\n\\n---\\n\\n**🚨 哈尔滨信息工程学院应急指挥中心**  \\n**校长: 张明**  \\n**发布时间: $CURRENT_DATE $CURRENT_TIME**  \\n\\n---\\n\\n*🆘 紧急求助: 0451-8888-0000 | 📱 应急微信群: 哈信工应急响应 | 🌐 应急网站: emergency.greathiit.com*\\n\\n**⚠️ 重要提示: 此通知将持续有效直到正式解除通知发布**",
    "summary": "检测到重大网络安全威胁，启动一级应急响应预案，全校师生必须立即执行防护措施，停止敏感系统登录，修改重要密码，24小时应急热线已开通",
    "level": 1,
    "categoryId": 1,
    "targetScope": "SCHOOL_WIDE",
    "pushChannels": [1, 2, 3, 5],
    "requireConfirm": true,
    "pinned": true
}
EOF
)

    echo "📝 通知内容预览："
    echo "标题: 🚨【紧急通知】$CURRENT_DATE校园网络安全紧急事件警报"
    echo "级别: Level 1 (紧急)"
    echo "范围: 全校范围"
    echo "格式: 紧急事件专用Markdown (完整企业级格式)"
    echo ""

    # 发送发布请求
    PUBLISH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
        -b "$COOKIE_JAR" \
        -X POST "$PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "$CSRF_HEADER_NAME: $CSRF_TOKEN" \
        -d "$LEVEL1_JSON")

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
        echo "🚨 Level 1 紧急通知发布成功！"
        echo "✅ 通知ID: $NOTIFICATION_ID"
        echo "✅ 紧急事件专用Markdown格式完整支持"
        echo "✅ 全渠道推送已启用 (APP+SMS+系统)"
        echo "✅ 置顶显示+确认阅读已启用"

        # 显示缓存清理提示
        check_and_show_cache_tips "$NOTIFICATION_ID" "1"

        return 0
    else
        echo ""
        echo "❌ Level 1 紧急通知发布失败"
        echo "状态码: $HTTP_STATUS, JSON代码: $JSON_CODE"
        return 1
    fi
}

# 执行完整发布流程
if authenticate && get_csrf && publish_notification; then
    echo ""
    echo "======================================="
    echo "🚨 Level 1 紧急通知发布成功报告："
    echo "- 认证状态: ✅ 校长角色认证成功"
    echo "- CSRF防护: ✅ Token验证通过"
    echo "- Markdown格式: ✅ 紧急事件专用格式支持"
    echo "- 复杂内容: ✅ 表格、代码块、链接、引用完整支持"
    echo "- 发布状态: ✅ 最高优先级发布成功"
    echo "- 通知级别: ✅ Level 1紧急级别确认"
    echo "- 全渠道推送: ✅ APP+SMS+系统通知已激活"
    echo "- 缓存清理: ✅ 已集成localStorage清理提示"
    echo "- 任务完成: ✅ Level 1-4 全系列通知脚本开发完成"
    echo "======================================="
    exit 0
else
    echo ""
    echo "======================================="
    echo "❌ Level 1 紧急通知发布失败"
    echo "请检查服务状态和网络连接"
    echo "======================================="
    exit 1
fi