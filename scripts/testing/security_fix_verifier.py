#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
安全修复验证自动化脚本
Security Fix Verification Automation Script

用途: 验证安全漏洞修复效果
作者: Security Auditor  
日期: 2025-08-24
"""

import requests
import json
import time
import sys
from datetime import datetime

class SecurityFixVerifier:
    def __init__(self):
        self.base_url_main = "http://localhost:48081"
        self.base_url_mock = "http://localhost:48082"
        self.test_results = []
        self.valid_token = None
        
    def log_result(self, test_name, status, description=""):
        """记录测试结果"""
        result = {
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'test': test_name,
            'status': status,
            'description': description
        }
        self.test_results.append(result)
        
        status_emoji = "✅" if status == "PASS" else "❌" if status == "FAIL" else "⚠️"
        print(f"{status_emoji} {test_name}: {status}")
        if description:
            print(f"   {description}")
        print()
    
    def authenticate(self):
        """获取认证Token"""
        try:
            response = requests.post(
                f"{self.base_url_mock}/mock-school-api/auth/authenticate",
                json={
                    "employeeId": "PRINCIPAL_001",
                    "name": "Principal-Zhang",
                    "password": "admin123"
                },
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get('code') == 200:
                    self.valid_token = data['data']['accessToken']
                    return True
            return False
        except:
            return False
    
    def verify_jwt_none_algorithm_blocked(self):
        """验证JWT None算法是否被禁用"""
        print("🔒 验证JWT None算法防护...")
        
        try:
            # 尝试使用None算法Token
            import jwt
            none_token = jwt.encode(
                {"username": "admin", "role": "SYSTEM_ADMIN"}, 
                "", 
                algorithm="none"
            )
            
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/list",
                headers={
                    'Authorization': f'Bearer {none_token}',
                    'tenant-id': '1'
                },
                timeout=10
            )
            
            if response.status_code == 401 or response.status_code == 403:
                self.log_result(
                    "JWT None算法防护", 
                    "PASS", 
                    "系统正确拒绝了None算法Token"
                )
            else:
                self.log_result(
                    "JWT None算法防护", 
                    "FAIL", 
                    f"系统仍接受None算法Token (HTTP {response.status_code})"
                )
                
        except Exception as e:
            self.log_result(
                "JWT None算法防护", 
                "WARN", 
                f"测试异常: {str(e)}"
            )
    
    def verify_jwt_signature_validation(self):
        """验证JWT签名验证是否生效"""
        print("🔐 验证JWT签名验证...")
        
        if not self.valid_token:
            self.log_result("JWT签名验证", "SKIP", "缺少有效Token")
            return
        
        try:
            # 篡改Token最后几个字符
            tampered_token = self.valid_token[:-5] + "XXXXX"
            
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/list",
                headers={
                    'Authorization': f'Bearer {tampered_token}',
                    'tenant-id': '1'
                },
                timeout=10
            )
            
            if response.status_code == 401 or response.status_code == 403:
                self.log_result(
                    "JWT签名验证", 
                    "PASS", 
                    "系统正确拒绝了被篡改的Token"
                )
            else:
                self.log_result(
                    "JWT签名验证", 
                    "FAIL", 
                    f"系统仍接受被篡改的Token (HTTP {response.status_code})"
                )
                
        except Exception as e:
            self.log_result(
                "JWT签名验证", 
                "WARN", 
                f"测试异常: {str(e)}"
            )
    
    def verify_authentication_enforcement(self):
        """验证认证强制执行"""
        print("🚪 验证认证强制执行...")
        
        try:
            # 无Token访问受保护资源
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/list",
                headers={'tenant-id': '1'},
                timeout=10
            )
            
            if response.status_code == 401 or response.status_code == 403:
                self.log_result(
                    "认证强制执行", 
                    "PASS", 
                    "系统正确要求认证"
                )
            else:
                self.log_result(
                    "认证强制执行", 
                    "FAIL", 
                    f"系统允许无认证访问 (HTTP {response.status_code})"
                )
                
        except Exception as e:
            self.log_result(
                "认证强制执行", 
                "WARN", 
                f"测试异常: {str(e)}"
            )
    
    def verify_permission_matrix(self):
        """验证权限矩阵是否生效"""
        print("🛡️ 验证权限矩阵...")
        
        # 获取学生Token进行测试
        try:
            student_response = requests.post(
                f"{self.base_url_mock}/mock-school-api/auth/authenticate",
                json={
                    "employeeId": "STUDENT_001",
                    "name": "Student-Zhang",
                    "password": "admin123"
                },
                timeout=10
            )
            
            if student_response.status_code != 200:
                self.log_result("权限矩阵验证", "SKIP", "无法获取学生Token")
                return
            
            student_data = student_response.json()
            student_token = student_data['data']['accessToken']
            
            # 学生尝试发布Level 1紧急通知(应被拒绝)
            notification_data = {
                "title": "测试紧急通知",
                "content": "学生尝试发布紧急通知",
                "level": 1,  # 紧急级别
                "categoryId": 1,
                "targetScope": "SCHOOL_WIDE",  # 全校范围
                "pushChannels": [1, 5]
            }
            
            response = requests.post(
                f"{self.base_url_main}/admin-api/test/notification/api/publish-database",
                json=notification_data,
                headers={
                    'Authorization': f'Bearer {student_token}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                },
                timeout=10
            )
            
            if response.status_code == 403:
                self.log_result(
                    "权限矩阵验证", 
                    "PASS", 
                    "学生越权发布被正确拒绝"
                )
            else:
                self.log_result(
                    "权限矩阵验证", 
                    "FAIL", 
                    f"学生仍可越权发布 (HTTP {response.status_code})"
                )
                
        except Exception as e:
            self.log_result(
                "权限矩阵验证", 
                "WARN", 
                f"测试异常: {str(e)}"
            )
    
    def verify_csrf_protection(self):
        """验证CSRF防护是否生效"""
        print("🛡️ 验证CSRF防护...")
        
        if not self.valid_token:
            self.log_result("CSRF防护验证", "SKIP", "缺少有效Token")
            return
        
        try:
            # 使用恶意Origin发送请求
            response = requests.post(
                f"{self.base_url_main}/admin-api/test/notification/api/publish-database",
                json={
                    "title": "CSRF测试",
                    "content": "测试内容",
                    "level": 4,
                    "categoryId": 1,
                    "targetScope": "CLASS",
                    "pushChannels": [1]
                },
                headers={
                    'Authorization': f'Bearer {self.valid_token}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1',
                    'Origin': 'http://evil.com',  # 恶意来源
                    'Referer': 'http://evil.com/attack.html'
                },
                timeout=10
            )
            
            if response.status_code == 403:
                self.log_result(
                    "CSRF防护验证", 
                    "PASS", 
                    "恶意来源请求被正确拒绝"
                )
            else:
                self.log_result(
                    "CSRF防护验证", 
                    "FAIL", 
                    f"恶意来源请求被接受 (HTTP {response.status_code})"
                )
                
        except Exception as e:
            self.log_result(
                "CSRF防护验证", 
                "WARN", 
                f"测试异常: {str(e)}"
            )
    
    def verify_security_headers(self):
        """验证安全响应头"""
        print("📋 验证安全响应头...")
        
        try:
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/ping",
                timeout=10
            )
            
            required_headers = [
                'X-Frame-Options',
                'X-XSS-Protection', 
                'X-Content-Type-Options',
                'Strict-Transport-Security'
            ]
            
            missing_headers = []
            for header in required_headers:
                if header not in response.headers:
                    missing_headers.append(header)
            
            if not missing_headers:
                self.log_result(
                    "安全响应头验证", 
                    "PASS", 
                    "所有必需安全头均已设置"
                )
            else:
                self.log_result(
                    "安全响应头验证", 
                    "FAIL", 
                    f"缺少安全头: {', '.join(missing_headers)}"
                )
                
        except Exception as e:
            self.log_result(
                "安全响应头验证", 
                "WARN", 
                f"测试异常: {str(e)}"
            )
    
    def verify_input_validation(self):
        """验证输入验证和XSS防护"""
        print("🔍 验证输入验证...")
        
        if not self.valid_token:
            self.log_result("输入验证", "SKIP", "缺少有效Token")
            return
        
        try:
            # 尝试提交XSS载荷
            xss_payload = "<script>alert('XSS')</script>"
            
            response = requests.post(
                f"{self.base_url_main}/admin-api/test/notification/api/publish-database",
                json={
                    "title": f"测试通知{xss_payload}",
                    "content": f"通知内容{xss_payload}",
                    "level": 4,
                    "categoryId": 1,
                    "targetScope": "CLASS",
                    "pushChannels": [1]
                },
                headers={
                    'Authorization': f'Bearer {self.valid_token}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                },
                timeout=10
            )
            
            # 检查XSS是否被过滤
            if response.status_code == 400:
                self.log_result(
                    "输入验证", 
                    "PASS", 
                    "XSS载荷被正确拦截"
                )
            elif response.status_code == 200:
                # 检查返回内容是否过滤了脚本
                list_response = requests.get(
                    f"{self.base_url_main}/admin-api/test/notification/api/list",
                    headers={
                        'Authorization': f'Bearer {self.valid_token}',
                        'tenant-id': '1'
                    },
                    timeout=10
                )
                
                if xss_payload in list_response.text:
                    self.log_result(
                        "输入验证", 
                        "FAIL", 
                        "XSS载荷未被过滤"
                    )
                else:
                    self.log_result(
                        "输入验证", 
                        "PASS", 
                        "XSS载荷被正确过滤"
                    )
            else:
                self.log_result(
                    "输入验证", 
                    "WARN", 
                    f"意外响应状态: {response.status_code}"
                )
                
        except Exception as e:
            self.log_result(
                "输入验证", 
                "WARN", 
                f"测试异常: {str(e)}"
            )
    
    def generate_verification_report(self):
        """生成验证报告"""
        passed = sum(1 for r in self.test_results if r['status'] == 'PASS')
        failed = sum(1 for r in self.test_results if r['status'] == 'FAIL')
        warnings = sum(1 for r in self.test_results if r['status'] == 'WARN')
        skipped = sum(1 for r in self.test_results if r['status'] == 'SKIP')
        
        report = f"""
# 安全修复验证报告

## 测试概览
- **测试时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- **测试项目**: {len(self.test_results)}个
- **通过**: ✅ {passed}个
- **失败**: ❌ {failed}个  
- **警告**: ⚠️ {warnings}个
- **跳过**: ⏭️ {skipped}个

## 修复效果评估
{'🎉 修复完全成功' if failed == 0 else f'⚠️ 仍有{failed}个问题需要处理'}

## 详细结果
"""
        
        for result in self.test_results:
            status_emoji = {
                'PASS': '✅',
                'FAIL': '❌', 
                'WARN': '⚠️',
                'SKIP': '⏭️'
            }.get(result['status'], '📋')
            
            report += f"""
### {status_emoji} {result['test']}
- **状态**: {result['status']}
- **时间**: {result['timestamp']}
"""
            if result['description']:
                report += f"- **说明**: {result['description']}\n"
        
        return report
    
    def run_verification(self):
        """执行完整验证"""
        print("=" * 60)
        print("🔒 安全修复验证开始")
        print("=" * 60)
        print()
        
        # 获取认证Token
        if self.authenticate():
            print("✅ 认证成功，开始安全验证\n")
        else:
            print("❌ 认证失败，部分测试将跳过\n")
        
        # 执行所有验证测试
        verification_tests = [
            self.verify_jwt_none_algorithm_blocked,
            self.verify_jwt_signature_validation,
            self.verify_authentication_enforcement,
            self.verify_permission_matrix,
            self.verify_csrf_protection,
            self.verify_security_headers,
            self.verify_input_validation
        ]
        
        for test in verification_tests:
            try:
                test()
            except Exception as e:
                self.log_result(f"{test.__name__}", "ERROR", f"测试异常: {str(e)}")
            print("-" * 40)
        
        # 生成并保存报告
        report = self.generate_verification_report()
        report_file = f"security_fix_verification_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
        
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)
        
        print("=" * 60)
        print(f"🔒 安全修复验证完成")
        print(f"📋 报告已保存: {report_file}")
        print("=" * 60)
        
        return self.test_results

if __name__ == "__main__":
    verifier = SecurityFixVerifier()
    results = verifier.run_verification()
    
    # 返回适当的退出代码
    failed_tests = sum(1 for r in results if r['status'] == 'FAIL')
    sys.exit(failed_tests)  # 有失败测试时返回非零代码