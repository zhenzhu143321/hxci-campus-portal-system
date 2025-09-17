#!/bin/bash

# 🎯 系统公告发布脚本 - 使用系统管理员角色
# 用于测试系统公告组件功能

echo "========================================"
echo "🎯 系统公告发布测试"
echo "========================================"

# 1. 获取JWT Token (系统管理员账号)
echo "📤 [Step 1] 系统管理员身份认证..."
AUTH_RESPONSE=$(curl -s -X POST http://localhost:48082/mock-school-api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "SYSTEM_ADMIN_001",
    "name": "系统管理员",
    "password": "admin123"
  }')

# 提取JWT Token (新格式：accessToken字段)
JWT_TOKEN=$(echo $AUTH_RESPONSE | grep -o '"accessToken":"[^"]*' | sed 's/"accessToken":"//')

if [ -z "$JWT_TOKEN" ]; then
  echo "❌ 获取JWT Token失败"
  echo "响应: $AUTH_RESPONSE"
  exit 1
fi

echo "✅ JWT Token获取成功"
echo "🔑 Token前20字符: ${JWT_TOKEN:0:20}..."

# 2. 获取CSRF Token
echo ""
echo "📤 [Step 2] 获取CSRF Token..."
CSRF_RESPONSE=$(curl -s -c cookies.txt http://localhost:48081/csrf-token)
CSRF_TOKEN=$(cat cookies.txt | grep XSRF-TOKEN | awk '{print $7}')

if [ -z "$CSRF_TOKEN" ]; then
  echo "❌ 获取CSRF Token失败"
  exit 1
fi

echo "✅ CSRF Token获取成功: $CSRF_TOKEN"

# 3. 发布系统公告
echo ""
echo "📤 [Step 3] 发布系统公告..."

# 当前时间戳
TIMESTAMP=$(date +%s%3N)

PUBLISH_RESPONSE=$(curl -s -X POST http://localhost:48081/admin-api/test/notification/api/publish-database \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "tenant-id: 1" \
  -H "Cookie: XSRF-TOKEN=$CSRF_TOKEN" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d "{
    \"title\": \"📢【系统维护】组件重构测试公告\",
    \"content\": \"**系统组件升级通知**\\n\\n尊敬的用户：\\n\\n系统已完成通知组件重构升级，现进行功能验证测试。\\n\\n**升级内容：**\\n- ✅ 系统公告组件独立化\\n- ✅ TypeScript类型安全增强\\n- ✅ 无障碍访问支持\\n- ✅ 懒加载性能优化\\n\\n如有任何问题，请联系技术支持。\\n\\n系统管理员\\n$(date +%Y-%m-%d' '%H:%M:%S)\",
    \"summary\": \"系统组件重构升级测试公告\",
    \"level\": 3,
    \"categoryId\": 5,
    \"targetScope\": \"SCHOOL_WIDE\",
    \"pushChannels\": [1, 5],
    \"requireConfirm\": false,
    \"pinned\": false
  }")

# 检查发布结果
if echo "$PUBLISH_RESPONSE" | grep -q '"code":0'; then
  echo "✅ 系统公告发布成功！"
  echo ""
  echo "📊 发布响应:"
  echo "$PUBLISH_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$PUBLISH_RESPONSE"

  # 提取通知ID
  NOTIFICATION_ID=$(echo $PUBLISH_RESPONSE | grep -o '"notificationId":[0-9]*' | sed 's/"notificationId"://')
  echo ""
  echo "🎯 通知ID: $NOTIFICATION_ID"
  echo "📋 发布角色: SYSTEM_ADMIN"
  echo "🌐 目标范围: SCHOOL_WIDE"
  echo "📝 通知级别: Level 3 (常规)"

  echo ""
  echo "========================================"
  echo "✅ 系统公告发布测试完成！"
  echo "请查看前端系统公告栏验证显示效果"
  echo "========================================"
else
  echo "❌ 系统公告发布失败"
  echo "响应: $PUBLISH_RESPONSE"

  # 尝试解析错误信息
  ERROR_MSG=$(echo $PUBLISH_RESPONSE | grep -o '"msg":"[^"]*' | sed 's/"msg":"//')
  if [ ! -z "$ERROR_MSG" ]; then
    echo "错误信息: $ERROR_MSG"
  fi

  exit 1
fi

# 清理cookies文件
rm -f cookies.txt