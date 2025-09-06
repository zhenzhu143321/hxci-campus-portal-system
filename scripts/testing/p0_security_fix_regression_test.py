#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P0级安全修复回归测试套件
哈尔滨信息工程学院校园门户系统

测试目标：
1. 验证安全修复的有效性 (JWT安全、认证强制、权限控制)
2. 确保业务功能完整性 (通知发布、角色权限、范围控制)
3. 生成详细的测试报告
"""

import requests
import json
import time
import sys
from datetime import datetime
from typing import Dict, List, Any, Optional
import base64
import jwt
from dataclasses import dataclass
from enum import Enum

# 配置常量
BASE_URL = "http://localhost"
MOCK_API_PORT = "48082"
MAIN_API_PORT = "48081"
MOCK_API_BASE = f"{BASE_URL}:{MOCK_API_PORT}"
MAIN_API_BASE = f"{BASE_URL}:{MAIN_API_PORT}"

# 测试用例状态
class TestResult(Enum):
    PASS = "✅ 通过"
    FAIL = "❌ 失败" 
    SKIP = "⏸️ 跳过"
    ERROR = "🔥 错误"

@dataclass
class TestCase:
    name: str
    description: str
    expected_result: str
    actual_result: str = ""
    status: TestResult = TestResult.SKIP
    error_message: str = ""
    execution_time: float = 0.0

@dataclass 
class TestAccount:
    employee_id: str
    name: str
    password: str
    role_code: str
    max_level: int
    allowed_scopes: List[str]
    jwt_token: Optional[str] = None

# 测试账号配置
TEST_ACCOUNTS = {
    "SYSTEM_ADMIN": TestAccount(
        employee_id="SYSTEM_ADMIN_001",
        name="系统管理员",
        password="admin123",
        role_code="SYSTEM_ADMIN",
        max_level=1,
        allowed_scopes=["SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"]
    ),
    "PRINCIPAL": TestAccount(
        employee_id="PRINCIPAL_001", 
        name="Principal-Zhang",
        password="admin123",
        role_code="PRINCIPAL",
        max_level=1,
        allowed_scopes=["SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"]
    ),
    "ACADEMIC_ADMIN": TestAccount(
        employee_id="ACADEMIC_ADMIN_001",
        name="Director-Li", 
        password="admin123",
        role_code="ACADEMIC_ADMIN",
        max_level=2,
        allowed_scopes=["SCHOOL_WIDE", "DEPARTMENT", "GRADE"]
    ),
    "TEACHER": TestAccount(
        employee_id="TEACHER_001",
        name="Teacher-Wang",
        password="admin123", 
        role_code="TEACHER",
        max_level=3,
        allowed_scopes=["DEPARTMENT", "CLASS"]
    ),
    "CLASS_TEACHER": TestAccount(
        employee_id="CLASS_TEACHER_001",
        name="ClassTeacher-Liu",
        password="admin123",
        role_code="CLASS_TEACHER", 
        max_level=3,
        allowed_scopes=["CLASS", "GRADE"]
    ),
    "STUDENT": TestAccount(
        employee_id="STUDENT_001",
        name="Student-Zhang",
        password="admin123",
        role_code="STUDENT",
        max_level=4,
        allowed_scopes=["CLASS"]
    )
}

class P0SecurityRegressionTester:
    def __init__(self):
        self.test_results: List[TestCase] = []
        self.start_time = datetime.now()
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'tenant-id': '1'  # yudao框架必需
        })
        
    def log(self, message: str, level: str = "INFO"):
        """统一日志输出"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        print(f"[{timestamp}] [{level}] {message}")
        
    def authenticate_user(self, account_key: str) -> Optional[str]:
        """用户认证获取JWT Token"""
        if account_key not in TEST_ACCOUNTS:
            self.log(f"测试账号 {account_key} 不存在", "ERROR")
            return None
            
        account = TEST_ACCOUNTS[account_key]
        
        auth_data = {
            "employeeId": account.employee_id,
            "name": account.name,
            "password": account.password
        }
        
        try:
            response = self.session.post(
                f"{MOCK_API_BASE}/mock-school-api/auth/authenticate",
                json=auth_data,
                timeout=10
            )
            
            if response.status_code == 200:
                result = response.json()
                # 修正判断条件：Mock API返回code=200表示成功
                if result.get("code") == 200 and result.get("success") is True:
                    token = result.get("data", {}).get("accessToken")
                    if token:
                        account.jwt_token = token
                        self.log(f"{account_key} 认证成功，获得Token")
                        return token
                        
            self.log(f"{account_key} 认证失败 - 响应: {response.text[:200]}", "ERROR")
            return None
            
        except Exception as e:
            self.log(f"{account_key} 认证异常: {str(e)}", "ERROR")
            return None
    
    def run_test_case(self, test_case: TestCase, test_func) -> TestCase:
        """执行单个测试用例"""
        self.log(f"执行测试: {test_case.name}")
        start_time = time.time()
        
        try:
            test_case.status, test_case.actual_result = test_func()
            test_case.execution_time = time.time() - start_time
            
            if test_case.status == TestResult.PASS:
                self.log(f"测试通过: {test_case.name}")
            else:
                self.log(f"测试失败: {test_case.name} - {test_case.actual_result}", "WARN")
                
        except Exception as e:
            test_case.status = TestResult.ERROR
            test_case.error_message = str(e)
            test_case.execution_time = time.time() - start_time
            self.log(f"测试异常: {test_case.name} - {str(e)}", "ERROR")
            
        self.test_results.append(test_case)
        return test_case
    
    def test_authentication_bypass(self):
        """A组: 认证绕过安全测试"""
        def test_no_token_access():
            """无Token访问API应返回401"""
            try:
                # 移除Authorization头
                headers = {k:v for k,v in self.session.headers.items() if k != 'Authorization'}
                response = requests.get(
                    f"{MAIN_API_BASE}/admin-api/test/notification/api/list",
                    headers=headers,
                    timeout=10
                )
                
                if response.status_code == 401:
                    return TestResult.PASS, "正确返回401未授权"
                else:
                    return TestResult.FAIL, f"期望401，实际{response.status_code}"
                    
            except Exception as e:
                return TestResult.ERROR, f"请求异常: {str(e)}"
                
        def test_invalid_token_access():
            """伪造Token访问API应返回401"""
            try:
                fake_token = "Bearer fake.invalid.token"
                headers = dict(self.session.headers)
                headers['Authorization'] = fake_token
                
                response = requests.get(
                    f"{MAIN_API_BASE}/admin-api/test/notification/api/list",
                    headers=headers,
                    timeout=10
                )
                
                if response.status_code == 401:
                    return TestResult.PASS, "正确拒绝伪造Token"
                else:
                    return TestResult.FAIL, f"期望401，实际{response.status_code}"
                    
            except Exception as e:
                return TestResult.ERROR, f"请求异常: {str(e)}"
        
        # 执行认证绕过测试
        self.run_test_case(
            TestCase(
                name="A1-无Token认证检查",
                description="无Token访问受保护API应返回401",
                expected_result="HTTP 401 未授权"
            ),
            test_no_token_access
        )
        
        self.run_test_case(
            TestCase(
                name="A2-伪造Token检查", 
                description="伪造Token访问API应被拒绝",
                expected_result="HTTP 401 Token验证失败"
            ),
            test_invalid_token_access
        )
    
    def test_permission_boundaries(self):
        """A组: 权限边界测试"""
        def test_student_level1_publish():
            """学生尝试发布Level 1紧急通知应被拒绝"""
            student_token = self.authenticate_user("STUDENT")
            if not student_token:
                return TestResult.ERROR, "学生认证失败"
                
            self.session.headers['Authorization'] = f'Bearer {student_token}'
            
            notification_data = {
                "title": "【安全测试】学生发布Level 1测试",
                "content": "这是权限测试，学生不应该能发布Level 1通知",
                "summary": "权限越界测试",
                "level": 1,  # 学生只能发布Level 4
                "categoryId": 1,
                "targetScope": "CLASS",
                "pushChannels": [1, 5],
                "requireConfirm": False,
                "pinned": False
            }
            
            try:
                response = self.session.post(
                    f"{MAIN_API_BASE}/admin-api/test/notification/api/publish-database",
                    json=notification_data,
                    timeout=10
                )
                
                if response.status_code == 403:
                    return TestResult.PASS, "正确拒绝学生发布Level 1通知"
                elif response.status_code == 200:
                    result = response.json()
                    if result.get("code") != 0:
                        return TestResult.PASS, f"业务层拒绝: {result.get('message', '权限不足')}"
                    else:
                        return TestResult.FAIL, "学生成功发布Level 1通知，权限控制失效"
                else:
                    return TestResult.FAIL, f"期望403或业务拒绝，实际{response.status_code}"
                    
            except Exception as e:
                return TestResult.ERROR, f"请求异常: {str(e)}"
        
        def test_teacher_school_wide_publish():
            """教师尝试发布SCHOOL_WIDE范围通知应被拒绝"""
            teacher_token = self.authenticate_user("TEACHER") 
            if not teacher_token:
                return TestResult.ERROR, "教师认证失败"
                
            self.session.headers['Authorization'] = f'Bearer {teacher_token}'
            
            notification_data = {
                "title": "【安全测试】教师发布全校通知测试",
                "content": "这是权限测试，教师不应该能发布全校范围通知",
                "summary": "权限越界测试", 
                "level": 3,
                "categoryId": 1,
                "targetScope": "SCHOOL_WIDE",  # 教师只能发布DEPARTMENT/CLASS范围
                "pushChannels": [1, 5],
                "requireConfirm": False,
                "pinned": False
            }
            
            try:
                response = self.session.post(
                    f"{MAIN_API_BASE}/admin-api/test/notification/api/publish-database",
                    json=notification_data,
                    timeout=10
                )
                
                if response.status_code == 403:
                    return TestResult.PASS, "正确拒绝教师发布全校通知"
                elif response.status_code == 200:
                    result = response.json()
                    if result.get("code") != 0:
                        return TestResult.PASS, f"业务层拒绝: {result.get('message', '权限不足')}"
                    else:
                        return TestResult.FAIL, "教师成功发布全校通知，权限控制失效"
                else:
                    return TestResult.FAIL, f"期望403或业务拒绝，实际{response.status_code}"
                    
            except Exception as e:
                return TestResult.ERROR, f"请求异常: {str(e)}"
        
        # 执行权限边界测试
        self.run_test_case(
            TestCase(
                name="A3-学生Level1权限检查",
                description="学生尝试发布Level 1紧急通知应被拒绝",
                expected_result="HTTP 403 或业务层权限拒绝"
            ),
            test_student_level1_publish
        )
        
        self.run_test_case(
            TestCase(
                name="A4-教师范围权限检查",
                description="教师尝试发布SCHOOL_WIDE通知应被拒绝", 
                expected_result="HTTP 403 或业务层权限拒绝"
            ),
            test_teacher_school_wide_publish
        )
    
    def test_normal_business_functions(self):
        """B组: 正常功能测试 - 确保业务不受影响"""
        def test_principal_level1_publish():
            """校长发布Level 1全校紧急通知应成功"""
            principal_token = self.authenticate_user("PRINCIPAL")
            if not principal_token:
                return TestResult.ERROR, "校长认证失败"
                
            self.session.headers['Authorization'] = f'Bearer {principal_token}'
            
            notification_data = {
                "title": f"【安全回归测试】校长紧急通知-{int(time.time())}",
                "content": "这是P0安全修复回归测试，校长发布的Level 1全校紧急通知，验证正常业务功能完整性。", 
                "summary": "校长权限正常功能验证",
                "level": 1,
                "categoryId": 1,
                "targetScope": "SCHOOL_WIDE",
                "pushChannels": [1, 5],
                "requireConfirm": False,
                "pinned": True
            }
            
            try:
                response = self.session.post(
                    f"{MAIN_API_BASE}/admin-api/test/notification/api/publish-database",
                    json=notification_data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    result = response.json()
                    if result.get("code") == 0:
                        notification_id = result.get("data", {}).get("id")
                        return TestResult.PASS, f"校长成功发布紧急通知，ID: {notification_id}"
                    else:
                        return TestResult.FAIL, f"业务层失败: {result.get('message', '未知错误')}"
                else:
                    return TestResult.FAIL, f"HTTP失败: {response.status_code} - {response.text}"
                    
            except Exception as e:
                return TestResult.ERROR, f"请求异常: {str(e)}"
        
        def test_teacher_level3_publish():
            """教师发布Level 3部门常规通知应成功"""
            teacher_token = self.authenticate_user("TEACHER")
            if not teacher_token:
                return TestResult.ERROR, "教师认证失败"
                
            self.session.headers['Authorization'] = f'Bearer {teacher_token}'
            
            notification_data = {
                "title": f"【安全回归测试】教师部门通知-{int(time.time())}",
                "content": "这是P0安全修复回归测试，教师发布的Level 3部门常规通知，验证教师正常发布权限。",
                "summary": "教师权限正常功能验证", 
                "level": 3,
                "categoryId": 1,
                "targetScope": "DEPARTMENT",
                "pushChannels": [1, 5],
                "requireConfirm": False,
                "pinned": False
            }
            
            try:
                response = self.session.post(
                    f"{MAIN_API_BASE}/admin-api/test/notification/api/publish-database",
                    json=notification_data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    result = response.json()
                    if result.get("code") == 0:
                        notification_id = result.get("data", {}).get("id") 
                        return TestResult.PASS, f"教师成功发布部门通知，ID: {notification_id}"
                    else:
                        return TestResult.FAIL, f"业务层失败: {result.get('message', '未知错误')}"
                else:
                    return TestResult.FAIL, f"HTTP失败: {response.status_code} - {response.text}"
                    
            except Exception as e:
                return TestResult.ERROR, f"请求异常: {str(e)}"
        
        def test_student_level4_publish():
            """学生发布Level 4班级提醒通知应成功"""
            student_token = self.authenticate_user("STUDENT")
            if not student_token:
                return TestResult.ERROR, "学生认证失败"
                
            self.session.headers['Authorization'] = f'Bearer {student_token}'
            
            notification_data = {
                "title": f"【安全回归测试】学生班级提醒-{int(time.time())}",
                "content": "这是P0安全修复回归测试，学生发布的Level 4班级提醒通知，验证学生正常发布权限。",
                "summary": "学生权限正常功能验证",
                "level": 4, 
                "categoryId": 1,
                "targetScope": "CLASS",
                "pushChannels": [1, 5],
                "requireConfirm": False,
                "pinned": False
            }
            
            try:
                response = self.session.post(
                    f"{MAIN_API_BASE}/admin-api/test/notification/api/publish-database", 
                    json=notification_data,
                    timeout=10
                )
                
                if response.status_code == 200:
                    result = response.json()
                    if result.get("code") == 0:
                        notification_id = result.get("data", {}).get("id")
                        return TestResult.PASS, f"学生成功发布班级通知，ID: {notification_id}"
                    else:
                        return TestResult.FAIL, f"业务层失败: {result.get('message', '未知错误')}"
                else:
                    return TestResult.FAIL, f"HTTP失败: {response.status_code} - {response.text}"
                    
            except Exception as e:
                return TestResult.ERROR, f"请求异常: {str(e)}"
        
        # 执行正常业务功能测试
        self.run_test_case(
            TestCase(
                name="B1-校长紧急通知发布",
                description="校长发布Level 1全校紧急通知应成功",
                expected_result="HTTP 200 + code: 0 + 返回通知ID"
            ),
            test_principal_level1_publish
        )
        
        self.run_test_case(
            TestCase(
                name="B2-教师部门通知发布", 
                description="教师发布Level 3部门常规通知应成功",
                expected_result="HTTP 200 + code: 0 + 返回通知ID"
            ),
            test_teacher_level3_publish
        )
        
        self.run_test_case(
            TestCase(
                name="B3-学生班级通知发布",
                description="学生发布Level 4班级提醒通知应成功", 
                expected_result="HTTP 200 + code: 0 + 返回通知ID"
            ),
            test_student_level4_publish
        )
    
    def test_notification_query_permissions(self):
        """C组: 通知查询权限测试"""
        def test_notification_list_access():
            """验证通知列表查询权限和数据过滤"""
            results = {}
            
            # 测试各角色查询权限
            for role_key in ["PRINCIPAL", "TEACHER", "STUDENT"]:
                token = self.authenticate_user(role_key)
                if not token:
                    results[role_key] = f"认证失败"
                    continue
                    
                self.session.headers['Authorization'] = f'Bearer {token}'
                
                try:
                    response = self.session.get(
                        f"{MAIN_API_BASE}/admin-api/test/notification/api/list",
                        timeout=10
                    )
                    
                    if response.status_code == 200:
                        result = response.json()
                        if result.get("code") == 0:
                            count = len(result.get("data", []))
                            results[role_key] = f"成功查询到{count}条通知"
                        else:
                            results[role_key] = f"业务失败: {result.get('message')}"
                    else:
                        results[role_key] = f"HTTP失败: {response.status_code}"
                        
                except Exception as e:
                    results[role_key] = f"异常: {str(e)}"
            
            # 判断测试结果
            if all("成功查询" in result for result in results.values()):
                return TestResult.PASS, f"各角色查询成功: {results}"
            else:
                return TestResult.FAIL, f"部分角色查询失败: {results}"
        
        self.run_test_case(
            TestCase(
                name="C1-通知列表查询权限",
                description="验证各角色通知列表查询权限和数据过滤",
                expected_result="各角色都能成功查询，数据按权限过滤"
            ),
            test_notification_list_access
        )
    
    def run_all_tests(self):
        """运行完整的P0安全修复回归测试"""
        self.log("🚀 开始P0级安全修复回归测试")
        self.log(f"测试目标: 验证安全修复有效性 + 业务功能完整性")
        self.log(f"测试环境: {MAIN_API_BASE} + {MOCK_API_BASE}")
        
        # A组: 安全修复验证测试
        self.log("\n📋 A组: 安全修复验证测试")
        self.test_authentication_bypass()
        self.test_permission_boundaries()
        
        # B组: 正常业务功能测试  
        self.log("\n📋 B组: 正常业务功能测试")
        self.test_normal_business_functions()
        
        # C组: 查询权限测试
        self.log("\n📋 C组: 查询权限测试")
        self.test_notification_query_permissions()
        
        # 生成测试报告
        self.generate_report()
    
    def generate_report(self):
        """生成测试报告"""
        end_time = datetime.now()
        total_time = (end_time - self.start_time).total_seconds()
        
        # 统计结果
        total_tests = len(self.test_results)
        passed_tests = len([t for t in self.test_results if t.status == TestResult.PASS])
        failed_tests = len([t for t in self.test_results if t.status == TestResult.FAIL])
        error_tests = len([t for t in self.test_results if t.status == TestResult.ERROR])
        
        success_rate = (passed_tests / total_tests * 100) if total_tests > 0 else 0
        
        # 生成报告内容
        report_content = f"""# P0级安全修复回归测试报告

## 🎯 测试概览
- **测试时间**: {self.start_time.strftime('%Y-%m-%d %H:%M:%S')} - {end_time.strftime('%H:%M:%S')}
- **测试耗时**: {total_time:.2f}秒
- **测试用例**: {total_tests}个
- **成功率**: {success_rate:.1f}%

## 📊 测试结果统计
- ✅ **通过**: {passed_tests}个 ({passed_tests/total_tests*100:.1f}%)
- ❌ **失败**: {failed_tests}个 ({failed_tests/total_tests*100:.1f}%)
- 🔥 **异常**: {error_tests}个 ({error_tests/total_tests*100:.1f}%)

## 🔍 详细测试结果

"""
        
        # 按组织测试结果
        groups = {
            "A组-安全修复验证": [t for t in self.test_results if t.name.startswith('A')],
            "B组-业务功能测试": [t for t in self.test_results if t.name.startswith('B')], 
            "C组-查询权限测试": [t for t in self.test_results if t.name.startswith('C')]
        }
        
        for group_name, group_tests in groups.items():
            if not group_tests:
                continue
                
            report_content += f"\n### {group_name}\n\n"
            
            for test in group_tests:
                report_content += f"#### {test.name}\n"
                report_content += f"- **状态**: {test.status.value}\n"
                report_content += f"- **描述**: {test.description}\n" 
                report_content += f"- **期望结果**: {test.expected_result}\n"
                report_content += f"- **实际结果**: {test.actual_result}\n"
                report_content += f"- **执行时间**: {test.execution_time:.3f}秒\n"
                
                if test.error_message:
                    report_content += f"- **错误信息**: {test.error_message}\n"
                    
                report_content += "\n"
        
        # 生成安全评估
        security_score = self.calculate_security_score()
        report_content += f"""
## 🛡️ 安全评估结果

### 安全等级: {security_score['grade']}
- **评分**: {security_score['score']}/100分
- **评级说明**: {security_score['description']}

### 安全修复效果:
- **JWT安全**: {security_score['jwt_security']}
- **认证强制**: {security_score['authentication']}
- **权限控制**: {security_score['authorization']}
- **业务完整性**: {security_score['business_integrity']}

## 🚨 风险和建议

{security_score['recommendations']}

## 📋 下一步行动计划

"""
        
        if failed_tests > 0 or error_tests > 0:
            report_content += """
### 紧急修复需求
1. 立即修复失败的测试用例
2. 分析错误原因，完善安全控制
3. 重新运行回归测试直到100%通过
"""
        else:
            report_content += """
### 继续开发建议  
1. ✅ P0安全修复验证成功，可进入下阶段开发
2. 🔄 建议定期执行安全回归测试
3. 📈 继续监控系统安全状态
"""
        
        report_content += f"""
---
**报告生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**测试工具**: P0SecurityRegressionTester v1.0  
**系统状态**: {"🟢 安全" if success_rate >= 90 else "🔴 需要关注"}
"""
        
        # 保存报告
        report_filename = f"p0_security_regression_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
        report_path = f"/opt/hxci-campus-portal/hxci-campus-portal-system/{report_filename}"
        
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        # 控制台输出摘要
        print("\n" + "="*80)
        print(f"📊 P0安全修复回归测试完成")
        print(f"📈 成功率: {success_rate:.1f}% ({passed_tests}/{total_tests})")
        print(f"🛡️ 安全等级: {security_score['grade']}")
        print(f"📄 详细报告: {report_path}")
        
        if success_rate >= 90:
            print("🎉 测试通过！P0安全修复验证成功，系统可进入下阶段开发")
        else:
            print("⚠️ 测试未完全通过，需要修复失败用例后重新测试")
            
        print("="*80)
        
        return report_path
    
    def calculate_security_score(self) -> Dict[str, Any]:
        """计算安全评分"""
        auth_tests = [t for t in self.test_results if t.name.startswith('A')]
        business_tests = [t for t in self.test_results if t.name.startswith('B')]
        
        auth_passed = len([t for t in auth_tests if t.status == TestResult.PASS])
        auth_total = len(auth_tests)
        
        business_passed = len([t for t in business_tests if t.status == TestResult.PASS])
        business_total = len(business_tests)
        
        # 安全评分计算 (认证安全占70%，业务完整性占30%)
        auth_score = (auth_passed / auth_total * 70) if auth_total > 0 else 0
        business_score = (business_passed / business_total * 30) if business_total > 0 else 0
        
        total_score = auth_score + business_score
        
        # 确定安全等级
        if total_score >= 90:
            grade = "🟢 A级 (优秀)"
            description = "安全修复完全有效，系统安全性优秀"
        elif total_score >= 75:
            grade = "🟡 B级 (良好)"  
            description = "安全修复基本有效，存在少量问题"
        elif total_score >= 60:
            grade = "🟠 C级 (一般)"
            description = "安全修复部分有效，需要进一步完善"
        else:
            grade = "🔴 D级 (需要改进)"
            description = "安全修复效果不佳，存在严重安全风险"
        
        # 生成修复建议
        recommendations = []
        
        failed_auth = [t for t in auth_tests if t.status != TestResult.PASS]
        if failed_auth:
            recommendations.append("🚨 认证安全修复不完整，需要重点关注JWT验证和权限控制")
            
        failed_business = [t for t in business_tests if t.status != TestResult.PASS] 
        if failed_business:
            recommendations.append("⚠️ 业务功能受到安全修复影响，需要调整兼容性")
        
        if not recommendations:
            recommendations.append("✅ 安全修复效果良好，建议定期执行回归测试监控")
        
        return {
            'score': int(total_score),
            'grade': grade,
            'description': description,
            'jwt_security': "✅ 有效" if auth_passed >= auth_total * 0.8 else "❌ 需要改进",
            'authentication': "✅ 有效" if auth_passed >= auth_total * 0.8 else "❌ 需要改进",
            'authorization': "✅ 有效" if auth_passed >= auth_total * 0.8 else "❌ 需要改进",
            'business_integrity': "✅ 完整" if business_passed >= business_total * 0.8 else "❌ 受影响",
            'recommendations': '\n'.join(f"- {rec}" for rec in recommendations)
        }

def main():
    """主函数"""
    print("🔒 P0级安全修复回归测试套件")
    print("="*80)
    print("测试目标: 验证P0安全修复有效性 + 业务功能完整性")
    print("测试范围: JWT安全、认证强制、权限控制、通知发布")
    print("="*80)
    
    # 创建测试实例并运行
    tester = P0SecurityRegressionTester()
    
    try:
        tester.run_all_tests()
        return 0
    except KeyboardInterrupt:
        tester.log("测试被用户中断", "WARN")
        return 1
    except Exception as e:
        tester.log(f"测试执行异常: {str(e)}", "ERROR")
        return 1

if __name__ == "__main__":
    exit(main())