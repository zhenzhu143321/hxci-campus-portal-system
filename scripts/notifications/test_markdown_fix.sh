#!/bin/bash

# 测试Markdown修复效果脚本
# 验证#标题和其他Markdown语法是否能正常发布

echo "🧪 Markdown修复效果测试"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 使用PRINCIPAL校长账号
EMPLOYEE_ID="PRINCIPAL_001"
NAME="Principal-Zhang"
PASSWORD="admin123"

# Cookie文件
COOKIE_JAR="/tmp/markdown_test_cookies.txt"

# 认证函数
authenticate() {
    rm -f "$COOKIE_JAR"

    # 认证
    AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")

    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

    # 获取CSRF Token
    CSRF_RESPONSE=$(curl -s -c "$COOKIE_JAR" -X GET "$CSRF_API")
    CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

    # 设置Cookie
    echo -e "\tlocalhost\tFALSE\t/\tFALSE\t0\tXSRF-TOKEN\t$CSRF_TOKEN" >> "$COOKIE_JAR"

    if [ -n "$JWT_TOKEN" ] && [ -n "$CSRF_TOKEN" ]; then
        echo "✅ 认证成功"
        return 0
    else
        echo "❌ 认证失败"
        return 1
    fi
}

# 测试单个Markdown内容
test_markdown() {
    local test_name="$1"
    local content="$2"

    echo ""
    echo "🧪 测试: $test_name"

    # 构建JSON
    local json="{\"title\":\"$test_name\",\"content\":\"$content\",\"summary\":\"测试\",\"level\":4,\"categoryId\":5,\"targetScope\":\"SCHOOL_WIDE\",\"pushChannels\":[1,5],\"requireConfirm\":false,\"pinned\":false}"

    # 发送请求
    local response=$(curl -s \
        -b "$COOKIE_JAR" \
        -X POST "$PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
        -d "$json")

    # 检查结果
    if [[ $response == *'"code":0'* ]]; then
        echo "✅ 成功: $test_name"
        # 提取通知ID
        local notification_id=$(echo "$response" | sed -n 's/.*"notificationId":\([0-9]*\).*/\1/p')
        if [ -n "$notification_id" ]; then
            echo "   通知ID: $notification_id"
        fi
        return 0
    else
        echo "❌ 失败: $test_name"
        echo "   响应: $response"
        return 1
    fi
}

# 开始测试
echo ""
echo "🔐 获取认证Token..."

if ! authenticate; then
    echo "❌ 认证失败，退出测试"
    exit 1
fi

echo ""
echo "开始Markdown修复验证测试..."

# 测试计划：专门测试之前失败的字符
declare -a test_cases=(
    "井号标题测试|# 这是一级标题"
    "多级标题测试|# 主标题\\n\\n## 副标题\\n\\n### 三级标题"
    "完整Markdown|# 健康生活指导\\n\\n## 日常保健\\n\\n- 每日饮水 **2000ml**\\n- 睡眠时间 *7-8小时*\\n- 适量运动\\n\\n## 饮食建议\\n\\n1. 多吃蔬果\\n2. 减少辛辣\\n\\n*愿大家身体健康！*"
    "复杂Markdown|# 🌟 温馨提醒\\n\\n**重要通知**：\\n\\n- [ ] 任务1\\n- [x] 任务2\\n\\n> 引用内容\\n\\n\`代码示例\`"
)

successful_tests=0
failed_tests=0

for test_case in "${test_cases[@]}"; do
    IFS='|' read -r test_name content <<< "$test_case"

    if test_markdown "$test_name" "$content"; then
        ((successful_tests++))
    else
        ((failed_tests++))
    fi

    sleep 1  # 避免请求过快
done

# 清理
rm -f "$COOKIE_JAR"

# 总结报告
echo ""
echo "======================================="
echo "📊 Markdown修复测试总结："
echo "成功: $successful_tests 个测试"
echo "失败: $failed_tests 个测试"

if [ $failed_tests -eq 0 ]; then
    echo ""
    echo "🎉 Markdown修复成功！"
    echo "✅ #标题语法完全可用"
    echo "✅ **加粗**和*斜体*正常"
    echo "✅ 列表和引用正常"
    echo "✅ 可以继续开发完整的通知脚本"
else
    echo ""
    echo "⚠️  仍有问题需要解决"
    echo "建议: 检查SafeSQLExecutor修改是否正确"
fi
echo "======================================="