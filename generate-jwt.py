#!/usr/bin/env python3
"""
和风天气JWT生成器
凭据名称: hxcisunli
"""

import time
import jwt
import sys
import os

def generate_jwt_token():
    """生成和风天气API的JWT Token"""
    
    # 配置参数 - 从和风天气控制台获取
    kid = "C7B7YU7RJA"     # 凭据ID
    sub = "3AE3TBK36X"     # 项目ID
    private_key_path = "ed25519-private.pem"
    
    # 检查私钥文件
    if not os.path.exists(private_key_path):
        print(f"Error: Private key file not found: {private_key_path}")
        return None
    
    # 读取私钥
    try:
        with open(private_key_path, 'r') as f:
            private_key = f.read()
    except Exception as e:
        print(f"Error reading private key: {e}")
        return None
    
    # 设置JWT payload
    # iat: 当前时间 -30秒 (避免时钟偏差)
    # exp: iat +15分钟
    iat = int(time.time()) - 30
    exp = iat + 900
    
    payload = {
        'iat': iat,
        'exp': exp,
        'sub': sub
    }
    
    # 设置JWT header
    headers = {
        'kid': kid,
        'alg': 'EdDSA'
    }
    
    print("Generate JWT Token...")
    print(f"Issued at (iat): {iat} ({time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(iat))})")
    print(f"Expires at (exp): {exp} ({time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(exp))})")
    print(f"Key ID (kid): {kid}")
    print(f"Subject (sub): {sub}")
    
    try:
        # 生成JWT Token
        token = jwt.encode(payload, private_key, algorithm='EdDSA', headers=headers)
        
        print("\nJWT Token generated successfully:")
        print("=" * 60)
        print(token)
        print("=" * 60)
        
        print("\nUsage:")
        print(f'curl -H "Authorization: Bearer {token}" --compressed \\')
        print('"https://YOUR_API_HOST/v7/weather/now?location=101050101"')
        
        print("\nToken details:")
        print(f"Header: {headers}")
        print(f"Payload: {payload}")
        
        return token
        
    except Exception as e:
        print(f"JWT generation failed: {e}")
        return None

def test_jwt_with_qweather():
    """测试JWT Token与和风天气API"""
    
    token = generate_jwt_token()
    if not token:
        return
    
    print("\nTesting JWT Token...")
    
    # 这里可以添加实际的API测试代码
    # 由于需要配置实际的API Host，暂时只生成Token
    
    print("Note: Please complete the following configuration in QWeather console:")
    print("1. Create new project (credential name: hxcisunli)")
    print("2. Upload public key to console")
    print("3. Get Key ID (kid) and Project ID (sub)")
    print("4. Configure API Host")
    print("5. Update configuration parameters in this script")

if __name__ == "__main__":
    print("QWeather JWT Generator")
    print("=" * 40)
    
    # 检查是否安装了PyJWT
    try:
        import jwt
    except ImportError:
        print("Please install PyJWT: pip install PyJWT")
        sys.exit(1)
    
    test_jwt_with_qweather()