#!/bin/bash

# 🚨 API发布功能修复验证脚本
# 用于验证NewTodoNotificationController的目标定向字段修复是否生效

echo "🚨 API发布功能修复验证开始"
echo "====================================="

# 配置API基础信息
API_BASE="http://localhost:48081"
MOCK_API_BASE="http://localhost:48082"

# 1. 获取JWT Token (校长权限)
echo "🔑 Step 1: 获取校长JWT Token..."
LOGIN_RESPONSE=$(curl -s -X POST "${MOCK_API_BASE}/mock-school-api/auth/authenticate" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "PRINCIPAL_001",
    "name": "Principal-Zhang", 
    "password": "admin123"
  }')

echo "登录响应: $LOGIN_RESPONSE"

# 提取Token
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "❌ 获取Token失败，退出测试"
  exit 1
fi

echo "✅ Token获取成功: ${TOKEN:0:20}..."

# 2. 测试1: 发布全校待办 (无目标定向字段)
echo ""
echo "🧪 Test 1: 发布全校待办通知 (无目标定向)"
PUBLISH_RESPONSE_1=$(curl -s -X POST "${API_BASE}/admin-api/test/todo-new/api/publish" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{
    "title": "【修复验证】全校待办通知测试",
    "content": "验证API修复后的全校通知发布功能",
    "priority": "high",
    "dueDate": "2025-08-30",
    "targetScope": "SCHOOL_WIDE"
  }')

echo "全校发布响应: $PUBLISH_RESPONSE_1"
echo ""

# 3. 测试2: 发布班级待办 (带目标定向字段)
echo "🧪 Test 2: 发布班级待办通知 (带目标定向)"
PUBLISH_RESPONSE_2=$(curl -s -X POST "${API_BASE}/admin-api/test/todo-new/api/publish" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{
    "title": "【修复验证】班级待办通知测试",
    "content": "验证API修复后的目标定向功能",
    "priority": "medium", 
    "dueDate": "2025-08-31",
    "targetScope": "CLASS",
    "targetStudentIds": ["2023010105", "2023010106"],
    "targetClassIds": ["2023-CS-01"],
    "targetGradeIds": ["2023"]
  }')

echo "班级发布响应: $PUBLISH_RESPONSE_2"
echo ""

# 4. 测试3: 发布年级待办 (空目标数组测试)
echo "🧪 Test 3: 发布年级待办通知 (空数组处理测试)"
PUBLISH_RESPONSE_3=$(curl -s -X POST "${API_BASE}/admin-api/test/todo-new/api/publish" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -d '{
    "title": "【修复验证】年级待办通知测试",
    "content": "验证API修复后的空数组处理功能",
    "priority": "low",
    "dueDate": "2025-09-01",
    "targetScope": "GRADE",
    "targetStudentIds": [],
    "targetGradeIds": ["2023", "2024"],
    "targetClassIds": [],
    "targetDepartmentIds": []
  }')

echo "年级发布响应: $PUBLISH_RESPONSE_3"
echo ""

# 5. 验证数据库插入结果
echo "🔍 Step 5: 验证数据库插入的目标定向数据..."
DB_CHECK=$(mysql -u root ruoyi-vue-pro --default-character-set=utf8 -e "
SELECT id, title, target_scope, target_student_ids, target_grade_ids, target_class_ids 
FROM todo_notifications 
WHERE title LIKE '%修复验证%' 
ORDER BY id DESC LIMIT 3;
")

echo "数据库查询结果:"
echo "$DB_CHECK"

echo ""
echo "🎉 API发布功能修复验证完成!"
echo "====================================="
echo "请检查上述测试结果："
echo "1. 三个API调用是否都成功 (HTTP 200)"
echo "2. 数据库中是否正确保存了目标定向字段"
echo "3. JSON序列化是否正确处理了空数组和非空数组"