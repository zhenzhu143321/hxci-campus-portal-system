#!/bin/bash

# 和风天气JWT生成脚本
# 凭据名称: hxcisunli
# 需要在控制台配置时使用下面的公钥

kid="YOUR_KID_FROM_CONSOLE"  # 需要从控制台获取实际的凭据ID
sub="YOUR_PROJECT_ID"        # 需要从控制台获取项目ID
private_key_path="ed25519-private.pem"

# 检查私钥文件是否存在
if [ ! -f "$private_key_path" ]; then
    echo "错误: 找不到私钥文件 $private_key_path"
    exit 1
fi

# 设置时间戳
# iat: 当前时间 -30秒 (避免时钟偏差)
# exp: iat +15分钟 (900秒)
iat=$(( $(date +%s) - 30 ))
exp=$((iat + 900))

echo "生成JWT Token..."
echo "发布时间 (iat): $iat"
echo "过期时间 (exp): $exp"

# base64url 编码 header 和 payload
header_base64=$(printf '{"alg":"EdDSA","kid":"%s"}' "$kid" | openssl base64 -e | tr -d '=' | tr '/+' '_-' | tr -d '\n')
payload_base64=$(printf '{"sub":"%s","iat":%d,"exp":%d}' "$sub" "$iat" "$exp" | openssl base64 -e | tr -d '=' | tr '/+' '_-' | tr -d '\n')
header_payload="${header_base64}.${payload_base64}"

echo "Header (base64url): $header_base64"
echo "Payload (base64url): $payload_base64"

# 创建临时文件用于签名
tmp_file=$(mktemp)
echo -n "$header_payload" > "$tmp_file"

# 使用Ed25519签名
signature=$(openssl pkeyutl -sign -inkey "$private_key_path" -rawin -in "$tmp_file" | openssl base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n')

# 删除临时文件
rm -f "$tmp_file"

# 生成完整的JWT
jwt="${header_payload}.${signature}"

echo ""
echo "✅ JWT Token 生成成功:"
echo "=========================="
echo "$jwt"
echo "=========================="
echo ""
echo "使用方法:"
echo "curl -H \"Authorization: Bearer $jwt\" --compressed \\"
echo "\"https://YOUR_API_HOST/v7/weather/now?location=101050101\""