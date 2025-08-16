# 🌤️ 天气数据缓存系统相关脚本

## 📁 文件说明

### 🔑 和风天气API认证文件
- `generate-weather-jwt.py` - 和风天气JWT Token生成器
- `ed25519-private.pem` - Ed25519私钥文件 (保密)
- `ed25519-public.pem` - Ed25519公钥文件

### 🌐 API配置信息
- **专属域名**: https://kc62b63hjr.re.qweatherapi.com
- **凭据ID (kid)**: C7B7YU7RJA  
- **项目ID (sub)**: 3AE3TBK36X
- **哈尔滨城市代码**: 101050101
- **算法**: EdDSA (Ed25519)

### 🚀 使用方法
```bash
# 生成最新JWT Token (15分钟有效期)
cd D:\ClaudeCode\AI_Web\scripts\weather
python generate-weather-jwt.py

# 测试API调用
curl -H "Authorization: Bearer {token}" --compressed \
"https://kc62b63hjr.re.qweatherapi.com/v7/weather/now?location=101050101"
```

### 📊 返回数据示例
```json
{
  "code": "200",
  "updateTime": "2025-08-14T20:28+08:00", 
  "now": {
    "temp": "21",        // 温度21°C
    "feelsLike": "21",   // 体感温度
    "text": "晴",        // 天气状况
    "windDir": "西南风", // 风向
    "windScale": "2",    // 风力等级
    "humidity": "75",    // 湿度75%
    "pressure": "997",   // 气压997hPa
    "vis": "30"          // 能见度30km
  }
}
```

---
📅 **创建时间**: 2025-08-14  
🎯 **用途**: T12天气缓存系统后端定时任务API调用