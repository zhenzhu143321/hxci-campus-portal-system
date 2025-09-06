#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
JWT信息泄露修复验证测试脚本
Phase 1.2: JWT安全修复验证

功能：
1. 验证JWT Payload不包含敏感信息
2. 验证JWT签名算法安全性
3. 验证Token生命周期控制
4. 验证重放攻击防护
5. 生成安全测试报告

@author: Security Team
@version: 1.0
@date: 2025-08-25
"""

import requests
import json
import base64
import time
import sys
from datetime import datetime
from typing import Dict, List, Optional, Tuple

class JWTSecurityValidator:
    """JWT安全验证器"""
    
    def __init__(self, mock_api_base: str = "http://localhost:48082", 
                 main_api_base: str = "http://localhost:48081"):
        self.mock_api_base = mock_api_base
        self.main_api_base = main_api_base
        self.test_results = []
        self.vulnerabilities_found = []
        
    def log_test_result(self, test_name: str, passed: bool, message: str, details: str = ""):
        """记录测试结果"""
        self.test_results.append({
            'test_name': test_name,
            'passed': passed,
            'message': message,
            'details': details,
            'timestamp': datetime.now().isoformat()
        })
        
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{status} [{test_name}] {message}")
        if details:
            print(f"    详情: {details}")
            
    def log_vulnerability(self, severity: str, description: str, recommendation: str):
        """记录发现的漏洞"""
        self.vulnerabilities_found.append({
            'severity': severity,
            'description': description,
            'recommendation': recommendation,
            'timestamp': datetime.now().isoformat()
        })
        
    def authenticate_user(self, employee_id: str = "STUDENT_001", name: str = "Student-Zhang", password: str = "admin123") -> Optional[str]:
        """用户认证获取JWT Token"""
        try:
            url = f"{self.mock_api_base}/mock-school-api/auth/authenticate"
            payload = {
                "employeeId": employee_id,
                "name": name,
                "password": password
            }
            
            response = requests.post(url, json=payload, headers={'Content-Type': 'application/json'})
            
            if response.status_code == 200:
                data = response.json()
                if data.get('code') == 200 and data.get('data'):
                    token = data['data'].get('accessToken')
                    if token:
                        self.log_test_result("用户认证", True, f"成功获取JWT Token: {employee_id}")
                        return token
                        
            self.log_test_result("用户认证", False, f"认证失败: HTTP {response.status_code}")
            return None
            
        except Exception as e:
            self.log_test_result("用户认证", False, f"认证异常: {str(e)}")
            return None
    
    def decode_jwt_payload(self, token: str) -> Optional[Dict]:
        """解码JWT Payload（不验证签名）"""
        try:
            # 分离JWT的各部分
            parts = token.split('.')
            if len(parts) != 3:
                return None
                
            # 解码Payload（添加padding）
            payload_part = parts[1]
            payload_part += '=' * (4 - len(payload_part) % 4)
            payload_bytes = base64.urlsafe_b64decode(payload_part)
            payload = json.loads(payload_bytes.decode('utf-8'))
            
            return payload
            
        except Exception as e:
            print(f"JWT解码失败: {str(e)}")
            return None
    
    def test_payload_security(self, token: str):
        """测试JWT Payload安全性"""
        print("\n🔍 [测试1] JWT Payload安全性验证")
        print("=" * 60)
        
        payload = self.decode_jwt_payload(token)
        if not payload:
            self.log_test_result("Payload解码", False, "无法解码JWT Payload")
            return
        
        self.log_test_result("Payload解码", True, "JWT Payload解码成功")
        
        # 检查标准JWT声明
        standard_claims = ['iss', 'aud', 'sub', 'exp', 'iat', 'jti']
        missing_standard = []
        for claim in standard_claims:
            if claim not in payload:
                missing_standard.append(claim)
        
        if missing_standard:
            self.log_test_result("标准声明检查", False, f"缺少标准JWT声明: {missing_standard}")
        else:
            self.log_test_result("标准声明检查", True, "所有标准JWT声明完整")
        
        # 检查敏感信息泄露
        sensitive_fields = [
            'username', 'realName', 'password', 'email', 'mobile', 
            'phone', 'idCard', 'address', 'departmentName', 'className',
            'grade', 'class', 'birth', 'gender'
        ]
        
        found_sensitive = []
        for field in sensitive_fields:
            if field in payload:
                found_sensitive.append(field)
        
        if found_sensitive:
            self.log_test_result("敏感信息检查", False, f"发现敏感信息: {found_sensitive}")
            self.log_vulnerability("HIGH", f"JWT Payload包含敏感信息: {found_sensitive}", 
                                 "移除所有不必要的个人身份信息")
        else:
            self.log_test_result("敏感信息检查", True, "JWT Payload不包含敏感信息")
        
        # 检查业务必要字段
        required_fields = ['userId', 'role']
        missing_required = []
        for field in required_fields:
            if field not in payload:
                missing_required.append(field)
        
        if missing_required:
            self.log_test_result("必要字段检查", False, f"缺少必要字段: {missing_required}")
        else:
            self.log_test_result("必要字段检查", True, "包含所有必要业务字段")
        
        # Payload大小检查
        payload_size = len(json.dumps(payload))
        if payload_size > 500:
            self.log_test_result("Payload大小检查", False, f"Payload过大: {payload_size}字节")
            self.log_vulnerability("MEDIUM", f"JWT Payload过大({payload_size}字节)", 
                                 "进一步精简JWT载荷，只保留绝对必要信息")
        else:
            self.log_test_result("Payload大小检查", True, f"Payload大小合理: {payload_size}字节")
        
        print(f"\n📋 JWT Payload内容:")
        for key, value in payload.items():
            print(f"    {key}: {value}")
    
    def test_algorithm_security(self, token: str):
        """测试JWT算法安全性"""
        print("\n🔐 [测试2] JWT算法安全性验证")
        print("=" * 60)
        
        try:
            # 解码Header
            parts = token.split('.')
            header_part = parts[0]
            header_part += '=' * (4 - len(header_part) % 4)
            header_bytes = base64.urlsafe_b64decode(header_part)
            header = json.loads(header_bytes.decode('utf-8'))
            
            algorithm = header.get('alg', '').upper()
            
            # 检查None算法
            if algorithm.lower() in ['none', 'null', '']:
                self.log_test_result("None算法检查", False, f"检测到不安全的算法: {algorithm}")
                self.log_vulnerability("CRITICAL", "JWT使用None算法，可被绕过", 
                                     "禁用None算法，使用HS256或RS256")
            else:
                self.log_test_result("None算法检查", True, f"算法安全: {algorithm}")
            
            # 检查算法白名单
            secure_algorithms = ['HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512', 'ES256']
            if algorithm not in secure_algorithms:
                self.log_test_result("算法白名单检查", False, f"算法不在安全白名单: {algorithm}")
                self.log_vulnerability("HIGH", f"JWT使用非推荐算法: {algorithm}", 
                                     "使用推荐的安全算法: HS256, RS256")
            else:
                self.log_test_result("算法白名单检查", True, f"使用安全算法: {algorithm}")
            
            print(f"📋 JWT Header信息:")
            for key, value in header.items():
                print(f"    {key}: {value}")
                
        except Exception as e:
            self.log_test_result("算法检查", False, f"Header解析失败: {str(e)}")
    
    def test_token_lifecycle(self, token: str):
        """测试Token生命周期"""
        print("\n⏰ [测试3] Token生命周期验证")
        print("=" * 60)
        
        payload = self.decode_jwt_payload(token)
        if not payload:
            self.log_test_result("生命周期检查", False, "无法解析Token")
            return
        
        # 检查过期时间
        exp = payload.get('exp')
        iat = payload.get('iat')
        
        if not exp:
            self.log_test_result("过期时间检查", False, "Token缺少过期时间")
            self.log_vulnerability("HIGH", "JWT Token缺少过期时间声明", "添加exp声明设置合理过期时间")
            return
        
        if not iat:
            self.log_test_result("签发时间检查", False, "Token缺少签发时间")
        else:
            self.log_test_result("签发时间检查", True, "Token包含签发时间")
        
        # 计算Token生命周期
        current_time = time.time()
        exp_time = datetime.fromtimestamp(exp)
        iat_time = datetime.fromtimestamp(iat) if iat else None
        
        # 检查是否过期
        if exp < current_time:
            self.log_test_result("过期检查", False, f"Token已过期: {exp_time}")
        else:
            remaining_seconds = exp - current_time
            remaining_minutes = remaining_seconds / 60
            self.log_test_result("过期检查", True, f"Token有效，剩余{remaining_minutes:.1f}分钟")
        
        # 检查生命周期长度
        if iat:
            lifecycle_seconds = exp - iat
            lifecycle_minutes = lifecycle_seconds / 60
            
            if lifecycle_minutes > 30:  # 超过30分钟
                self.log_test_result("生命周期长度检查", False, f"Token生命周期过长: {lifecycle_minutes:.1f}分钟")
                self.log_vulnerability("MEDIUM", f"JWT生命周期过长({lifecycle_minutes:.1f}分钟)", 
                                     "缩短Token有效期至15-30分钟")
            else:
                self.log_test_result("生命周期长度检查", True, f"Token生命周期合理: {lifecycle_minutes:.1f}分钟")
        
        print(f"📋 Token时间信息:")
        if iat_time:
            print(f"    签发时间: {iat_time}")
        print(f"    过期时间: {exp_time}")
        print(f"    当前时间: {datetime.fromtimestamp(current_time)}")
    
    def test_replay_protection(self, token: str):
        """测试重放攻击防护"""
        print("\n🔄 [测试4] 重放攻击防护验证")
        print("=" * 60)
        
        payload = self.decode_jwt_payload(token)
        if not payload:
            self.log_test_result("重放防护检查", False, "无法解析Token")
            return
        
        # 检查JWT ID
        jti = payload.get('jti')
        if not jti:
            self.log_test_result("JWT ID检查", False, "Token缺少JWT ID (jti)")
            self.log_vulnerability("MEDIUM", "JWT Token缺少唯一标识符(jti)", 
                                 "添加jti声明用于重放攻击防护")
        else:
            self.log_test_result("JWT ID检查", True, f"Token包含JWT ID")
            
            # 检查JTI格式
            if jti.startswith('jwt_v2_'):
                self.log_test_result("JTI格式检查", True, "JWT ID格式符合安全规范")
            else:
                self.log_test_result("JTI格式检查", False, f"JWT ID格式可能不够安全: {jti[:20]}...")
        
        # 模拟重放攻击测试（多次使用相同Token）
        try:
            url = f"{self.main_api_base}/admin-api/test/notification/api/ping"
            headers = {
                'Authorization': f'Bearer {token}',
                'Content-Type': 'application/json',
                'tenant-id': '1'
            }
            
            success_count = 0
            for i in range(3):  # 尝试3次请求
                response = requests.get(url, headers=headers)
                if response.status_code == 200:
                    success_count += 1
                time.sleep(0.1)
            
            if success_count == 3:
                self.log_test_result("重放测试", True, "Token重复使用正常（基础功能）")
            else:
                self.log_test_result("重放测试", False, f"Token重复使用异常: {success_count}/3次成功")
                
        except Exception as e:
            self.log_test_result("重放测试", False, f"重放测试异常: {str(e)}")
    
    def test_signature_validation(self, token: str):
        """测试签名验证"""
        print("\n🔏 [测试5] 签名验证测试")
        print("=" * 60)
        
        # 尝试篡改Token
        parts = token.split('.')
        if len(parts) != 3:
            self.log_test_result("签名测试", False, "Token格式错误")
            return
        
        # 篡改Payload
        try:
            payload_part = parts[1]
            payload_part += '=' * (4 - len(payload_part) % 4)
            payload_bytes = base64.urlsafe_b64decode(payload_part)
            payload = json.loads(payload_bytes.decode('utf-8'))
            
            # 篡改角色信息
            payload['role'] = 'PRINCIPAL'  # 尝试提权
            
            # 重新编码
            new_payload_bytes = json.dumps(payload).encode('utf-8')
            new_payload_part = base64.urlsafe_b64encode(new_payload_bytes).decode('utf-8').rstrip('=')
            
            # 构造篡改后的Token
            tampered_token = f"{parts[0]}.{new_payload_part}.{parts[2]}"
            
            # 尝试使用篡改的Token
            url = f"{self.main_api_base}/admin-api/test/notification/api/ping"
            headers = {
                'Authorization': f'Bearer {tampered_token}',
                'Content-Type': 'application/json',
                'tenant-id': '1'
            }
            
            response = requests.get(url, headers=headers)
            
            if response.status_code == 401 or response.status_code == 403:
                self.log_test_result("篡改Token测试", True, "篡改Token被正确拒绝")
            else:
                self.log_test_result("篡改Token测试", False, f"篡改Token通过验证: HTTP {response.status_code}")
                self.log_vulnerability("CRITICAL", "JWT签名验证失效，篡改Token未被拒绝", 
                                     "检查JWT签名验证逻辑，确保篡改Token被拒绝")
                
        except Exception as e:
            self.log_test_result("篡改Token测试", False, f"测试异常: {str(e)}")
    
    def generate_security_report(self) -> str:
        """生成安全测试报告"""
        report = []
        report.append("JWT安全修复验证报告")
        report.append("=" * 80)
        report.append(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append(f"测试目标: Phase 1.2 JWT信息泄露修复验证")
        report.append("")
        
        # 测试结果统计
        total_tests = len(self.test_results)
        passed_tests = sum(1 for result in self.test_results if result['passed'])
        failed_tests = total_tests - passed_tests
        
        report.append("📊 测试结果统计:")
        report.append(f"  总测试数: {total_tests}")
        report.append(f"  通过测试: {passed_tests} ✅")
        report.append(f"  失败测试: {failed_tests} ❌")
        report.append(f"  通过率: {(passed_tests/total_tests*100):.1f}%")
        report.append("")
        
        # 详细测试结果
        report.append("📋 详细测试结果:")
        for result in self.test_results:
            status = "✅" if result['passed'] else "❌"
            report.append(f"  {status} {result['test_name']}: {result['message']}")
            if result['details']:
                report.append(f"     详情: {result['details']}")
        report.append("")
        
        # 发现的漏洞
        if self.vulnerabilities_found:
            report.append("🚨 发现的安全问题:")
            for vuln in self.vulnerabilities_found:
                report.append(f"  [{vuln['severity']}] {vuln['description']}")
                report.append(f"     建议: {vuln['recommendation']}")
                report.append("")
        else:
            report.append("✅ 未发现安全问题")
            report.append("")
        
        # 总体安全评估
        if failed_tests == 0 and len(self.vulnerabilities_found) == 0:
            security_level = "优秀"
            report.append("🎯 总体安全评估: 优秀 ✅")
            report.append("   JWT信息泄露修复完全成功，安全防护措施完善")
        elif len([v for v in self.vulnerabilities_found if v['severity'] in ['CRITICAL', 'HIGH']]) > 0:
            security_level = "需要改进"
            report.append("⚠️ 总体安全评估: 需要改进")
            report.append("   发现高风险安全问题，需要立即修复")
        else:
            security_level = "良好"
            report.append("👍 总体安全评估: 良好")
            report.append("   JWT安全修复基本成功，有少量需要优化的地方")
        
        report.append("")
        report.append("=" * 80)
        
        return "\n".join(report)
    
    def run_all_tests(self) -> bool:
        """运行所有JWT安全测试"""
        print("🚀 开始JWT信息泄露修复验证测试")
        print("=" * 80)
        
        # 获取测试Token
        token = self.authenticate_user()
        if not token:
            print("❌ 无法获取测试Token，测试终止")
            return False
        
        # 执行所有测试
        self.test_payload_security(token)
        self.test_algorithm_security(token)
        self.test_token_lifecycle(token)
        self.test_replay_protection(token)
        self.test_signature_validation(token)
        
        # 生成报告
        report = self.generate_security_report()
        print("\n" + report)
        
        # 保存报告
        report_file = f"jwt_security_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)
        print(f"\n📄 测试报告已保存: {report_file}")
        
        # 返回是否全部通过
        return all(result['passed'] for result in self.test_results) and len(self.vulnerabilities_found) == 0

def main():
    """主函数"""
    print("JWT信息泄露修复验证测试工具 v1.0")
    print("Phase 1.2: JWT Security Vulnerability Fix Verification")
    print()
    
    # 检查服务可用性
    validator = JWTSecurityValidator()
    
    try:
        # 检查Mock API
        response = requests.get(f"{validator.mock_api_base}/mock-school-api/auth/ping", timeout=5)
        if response.status_code != 200:
            print(f"❌ Mock API服务不可用 (端口48082)")
            return False
            
        # 检查主服务
        response = requests.get(f"{validator.main_api_base}/admin-api/test/notification/api/ping", timeout=5)
        if response.status_code != 200:
            print(f"❌ 主服务不可用 (端口48081)")
            return False
            
    except requests.exceptions.RequestException as e:
        print(f"❌ 服务连接失败: {str(e)}")
        print("请确保后端服务已启动：")
        print("  - Mock School API (端口48082)")
        print("  - 主通知服务 (端口48081)")
        return False
    
    print("✅ 服务连接正常，开始安全测试...")
    print()
    
    # 运行测试
    success = validator.run_all_tests()
    
    if success:
        print("\n🎉 所有安全测试通过！JWT信息泄露修复验证成功")
        return True
    else:
        print("\n⚠️ 部分测试失败，请检查上述安全问题")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)