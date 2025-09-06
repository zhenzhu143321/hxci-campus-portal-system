#!/bin/bash
echo "性能测试开始 - 20个连续请求"
start_time=$(date +%s.%3N)
success_count=0
error_count=0

for i in {1..20}; do
  response=$(curl -X GET http://localhost:48081/admin-api/test/todo-new/api/my-list \
  -H "Content-Type: application/json" \
  -H "tenant-id: 1" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJNT0NLIn0=.eyJlbXBsb3llZUlkIjoiUFJJTkNJUEFMXzAwMSIsInVzZXJJZCI6IlBSSU5DSVBBTF8wMDEiLCJyZWFsTmFtZSI6IlByaW5jaXBhbC1aaGFuZyIsInJvbGVDb2RlIjoiUFJJTkNJUEFMIiwicm9sZU5hbWUiOiJQcmluY2lwYWwiLCJ1c2VyVHlwZSI6IkFETUlOIiwiZXhwIjoxNzU2MDUwMzcxLCJpYXQiOjE3NTU5NjM5NzEsInVzZXJuYW1lIjoiUHJpbmNpcGFsLVpoYW5nIn0=.TU9DS19TSUdOQVRVUkVfUFJJTkNJUEFMXzAwMQ==" \
  -s -w "%{http_code}" -o /tmp/resp_$i.json)
  
  if [ "$response" = "200" ]; then
    success_count=$((success_count + 1))
    echo -n "✓"
  else  
    error_count=$((error_count + 1))
    echo -n "✗"
  fi
done

end_time=$(date +%s.%3N)
total_time=$(echo "$end_time - $start_time" | bc -l)
qps=$(echo "scale=2; 20 / $total_time" | bc -l)
avg_time=$(echo "scale=3; $total_time / 20" | bc -l)

echo ""
echo "=== 性能测试结果 ==="
echo "总请求数: 20"
echo "成功请求: $success_count" 
echo "失败请求: $error_count"
echo "总耗时: ${total_time}s"
echo "QPS: $qps" 
echo "平均响应时间: ${avg_time}s"

# 清理临时文件
rm -f /tmp/resp_*.json