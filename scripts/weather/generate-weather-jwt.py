#!/usr/bin/env python3
"""
和风天气JWT生成器 - 项目专用版本
凭据名称: hxcisunli
位置: D:\ClaudeCode\AI_Web\scripts\weather\
用途: T12天气缓存系统后端定时任务API调用
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
    
    print("Generate Weather JWT Token...")
    print(f"Issued at: {time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(iat))}")
    print(f"Expires at: {time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(exp))}")
    print(f"Key ID: {kid}")
    print(f"Project ID: {sub}")
    
    try:
        # 生成JWT Token
        token = jwt.encode(payload, private_key, algorithm='EdDSA', headers=headers)
        
        print("\nJWT Token generated successfully:")
        print("=" * 80)
        print(token)
        print("=" * 80)
        
        print("\nTest command:")
        print(f'curl -H "Authorization: Bearer {token}" --compressed \\')
        print('"https://kc62b63hjr.re.qweatherapi.com/v7/weather/now?location=101050101"')
        
        return token
        
    except Exception as e:
        print(f"JWT generation failed: {e}")
        return None

if __name__ == "__main__":
    print("QWeather JWT Generator - Harbin Institute of Information Engineering")
    print("=" * 60)
    
    # 检查是否安装了PyJWT
    try:
        import jwt
    except ImportError:
        print("ERROR: Please install PyJWT: pip install PyJWT")
        sys.exit(1)
    
    token = generate_jwt_token()
    
    if token:
        print(f"\nSUCCESS: Token generated successfully!")
        print(f"Valid for 15 minutes from now")
        print(f"Location: Harbin (101050101)")
        print(f"API Host: https://kc62b63hjr.re.qweatherapi.com")
    else:
        print("ERROR: Token generation failed!")
        sys.exit(1)