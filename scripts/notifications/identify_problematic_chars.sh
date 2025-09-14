#!/bin/bash

# 渐进式markdown字符测试
# 逐步添加特殊字符，识别SQL注入检测触发点

echo "🔍 Markdown特殊字符检测脚本"
echo "======================================="

# API端点配置
AUTH_API="http://localhost:48082/mock-school-api/auth/authenticate"
CSRF_API="http://localhost:48081/csrf-token"
PUBLISH_API="http://localhost:48081/admin-api/test/notification/api/publish-database"

# 使用PRINCIPAL校长账号
EMPLOYEE_ID="PRINCIPAL_001"
NAME="Principal-Zhang"
PASSWORD="admin123"

# 公共认证函数
authenticate_and_get_tokens() {
    # 清理Cookie文件
    rm -f "/tmp/char_test_cookies.txt"

    # 认证
    AUTH_RESPONSE=$(curl -s -X POST "$AUTH_API" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\": \"$EMPLOYEE_ID\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")

    JWT_TOKEN=$(echo "$AUTH_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

    # 获取CSRF Token
    CSRF_RESPONSE=$(curl -s -c "/tmp/char_test_cookies.txt" -X GET "$CSRF_API")
    CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

    # 设置Cookie
    echo -e "\tlocalhost\tFALSE\t/\tFALSE\t0\tXSRF-TOKEN\t$CSRF_TOKEN" >> "/tmp/char_test_cookies.txt"

    if [ -n "$JWT_TOKEN" ] && [ -n "$CSRF_TOKEN" ]; then
        return 0
    else
        return 1
    fi
}

# 测试单个内容的函数
test_content() {
    local test_name="$1"
    local content="$2"

    echo ""
    echo "🧪 测试: $test_name"
    echo "内容: $content"

    # 构建JSON
    local json="{\"title\":\"测试$test_name\",\"content\":\"$content\",\"summary\":\"测试\",\"level\":4,\"categoryId\":5,\"targetScope\":\"SCHOOL_WIDE\",\"pushChannels\":[1,5],\"requireConfirm\":false,\"pinned\":false}"

    # 发送请求
    local response=$(curl -s \
        -b "/tmp/char_test_cookies.txt" \
        -X POST "$PUBLISH_API" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        -H "tenant-id: 1" \
        -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
        -d "$json")

    # 检查结果
    if [[ $response == *'"code":0'* ]]; then
        echo "✅ 成功: $test_name"
        return 0
    elif [[ $response == *"SQL injection"* ]]; then
        echo "❌ SQL注入检测: $test_name"
        echo "   响应: $response"
        return 1
    else
        echo "❓ 其他错误: $test_name"
        echo "   响应: $response"
        return 2
    fi
}

# 开始测试
echo ""
echo "🔐 获取认证Token..."

if authenticate_and_get_tokens; then
    echo "✅ 认证成功，开始字符测试"
else
    echo "❌ 认证失败，退出测试"
    exit 1
fi

echo ""
echo "开始渐进式字符测试..."

# 测试计划：从简单到复杂
declare -a test_cases=(
    "基础文本|这是基础文本测试"
    "换行符|第一行\\n第二行"
    "星号|这是*重要*文本"
    "双星号|这是**重要**文本"
    "井号|# 标题测试"
    "减号|- 列表项目"
    "数字列表|1. 第一项"
    "下划线|这是_斜体_文本"
    "反引号|这是`代码`文本"
    "组合1|# 标题\\n\\n这是**重要**内容"
    "组合2|## 小标题\\n\\n- 列表项\\n- 第二项"
    "完整markdown|# 标题\\n\\n## 副标题\\n\\n**重要**: 这是*重要*信息\\n\\n- 项目1\\n- 项目2\\n\\n`代码示例`"
)

successful_tests=0
failed_tests=0

for test_case in "${test_cases[@]}"; do
    IFS='|' read -r test_name content <<< "$test_case"

    if test_content "$test_name" "$content"; then
        ((successful_tests++))
    else
        ((failed_tests++))
        # 如果失败，记录失败的字符
        echo "⚠️  '$test_name' 包含问题字符，停止后续复杂测试"
        break
    fi

    sleep 1  # 避免请求过快
done

# 清理
rm -f "/tmp/char_test_cookies.txt"

# 总结报告
echo ""
echo "======================================="
echo "📊 字符测试总结："
echo "成功: $successful_tests 个测试"
echo "失败: $failed_tests 个测试"

if [ $failed_tests -eq 0 ]; then
    echo ""
    echo "🎉 所有字符测试通过！"
    echo "✅ Markdown语法完全可用"
    echo "✅ 可以继续开发完整的Level 4脚本"
else
    echo ""
    echo "⚠️  发现问题字符，需要进一步分析"
    echo "建议: 使用成功的字符集合开发通知脚本"
fi
echo "======================================="