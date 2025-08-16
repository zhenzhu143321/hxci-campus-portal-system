#!/usr/bin/env python3
"""
测试和风天气JWT Token生成 - 直接验证
"""
import requests
import subprocess
import json

def test_qweather_api():
    print("测试和风天气API功能")
    print("=" * 60)
    
    # 1. 生成JWT Token
    print("1. 生成JWT Token...")
    
    try:
        # 调用JWT生成脚本
        result = subprocess.run([
            'python', 
            'D:/ClaudeCode/AI_Web/scripts/weather/generate-weather-jwt.py'
        ], capture_output=True, text=True)
        
        if result.returncode != 0:
            print(f"JWT生成脚本执行失败: {result.stderr}")
            return False
        
        # 从输出中提取Token
        lines = result.stdout.strip().split('\n')
        token = None
        for line in lines:
            if len(line) > 100 and '.' in line and not line.startswith('='):
                token = line.strip()
                break
        
        if not token:
            print("无法从脚本输出中提取Token")
            print(f"脚本输出: {result.stdout}")
            return False
            
    except Exception as e:
        print(f"Token生成异常: {e}")
        return False
    
    print(f"Token生成成功: {token[:50]}...")
    
    # 2. 测试API调用
    print("\n2. 测试和风天气API...")
    
    api_url = "https://kc62b63hjr.re.qweatherapi.com/v7/weather/now?location=101050101"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/json",
        "Accept-Encoding": "gzip, deflate"
    }
    
    try:
        response = requests.get(api_url, headers=headers)
        print(f"请求状态码: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print("API调用成功!")
            
            if data.get('code') == '200':
                now_data = data.get('now', {})
                print(f"\n哈尔滨当前天气:")
                print(f"   温度: {now_data.get('temp')}°C")
                print(f"   天气: {now_data.get('text')}")
                print(f"   湿度: {now_data.get('humidity')}%")
                print(f"   风向: {now_data.get('windDir')}")
                print(f"   风力: {now_data.get('windScale')}级")
                print(f"   气压: {now_data.get('pressure')}hPa")
                print(f"   能见度: {now_data.get('vis')}km")
                return True
            else:
                print(f"API返回错误: {data}")
                return False
        else:
            print(f"HTTP请求失败: {response.status_code}")
            print(f"响应内容: {response.text}")
            return False
            
    except Exception as e:
        print(f"API调用异常: {e}")
        return False

if __name__ == "__main__":
    success = test_qweather_api()
    print("\n" + "=" * 60)
    if success:
        print("和风天气API测试通过!")
    else:
        print("和风天气API测试失败!")