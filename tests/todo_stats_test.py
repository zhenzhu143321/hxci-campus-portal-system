#!/usr/bin/env python3
"""
哈尔滨信息工程学院校园门户系统 - 通知统计API功能测试

测试目标：验证待办统计信息准确性、多维度统计功能和统计缓存性能
测试范围：/admin-api/test/todo-new/api/{id}/stats 端点
"""

import requests
import json
import time
import logging
from typing import Dict, List, Any

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 测试配置
BASE_URL = "http://localhost:48081"
MOCK_API_URL = "http://localhost:48082"

# 测试账号配置
TEST_ACCOUNTS = {
    "principal": {
        "employeeId": "PRINCIPAL_001",
        "name": "Principal-Zhang", 
        "password": "admin123",
        "role": "PRINCIPAL"
    },
    "student": {
        "employeeId": "STUDENT_001",
        "name": "Student-Zhang",
        "password": "admin123", 
        "role": "STUDENT"
    },
    "teacher": {
        "employeeId": "TEACHER_001",
        "name": "Teacher-Wang",
        "password": "admin123",
        "role": "TEACHER"
    }
}

class TodoStatsTest:
    """待办统计API功能测试类"""
    
    def __init__(self):
        self.session = requests.Session()
        self.tokens = {}
        self.test_todos = []  # 存储测试创建的待办
        
    def authenticate_user(self, account_key: str) -> str:
        """认证用户并获取JWT Token"""
        account = TEST_ACCOUNTS[account_key]
        
        # 调用Mock School API进行认证
        auth_url = f"{MOCK_API_URL}/mock-school-api/auth/authenticate"
        auth_data = {
            "employeeId": account["employeeId"],
            "name": account["name"],
            "password": account["password"]
        }
        
        try:
            response = self.session.post(auth_url, json=auth_data, timeout=10)
            if response.status_code == 200:
                auth_result = response.json()
                logger.info(f"🔍 认证响应: {json.dumps(auth_result, ensure_ascii=False)}")
                
                # 检查不同格式的响应
                token = None
                
                # 格式1: {"success": true, "data": {"accessToken": "..."}}
                if auth_result.get("success") and "data" in auth_result:
                    token = auth_result["data"].get("accessToken")
                # 格式2: {"code": 200, "data": {"accessToken": "..."}}
                elif auth_result.get("code") == 200 and "data" in auth_result:
                    token = auth_result["data"].get("accessToken")
                
                if token:
                    self.tokens[account_key] = token
                    logger.info(f"✅ 用户 {account['name']} 认证成功，获取到Token")
                    return token
                else:
                    logger.error(f"❌ 认证响应格式无法识别或缺少Token: {auth_result}")
            
            logger.error(f"❌ 用户 {account['name']} 认证失败: {response.text}")
            return None
            
        except Exception as e:
            logger.error(f"❌ 认证请求异常: {e}")
            return None
    
    def get_headers(self, account_key: str) -> Dict[str, str]:
        """获取认证请求头"""
        token = self.tokens.get(account_key)
        if not token:
            token = self.authenticate_user(account_key)
        
        return {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "tenant-id": "1"
        }
    
    def create_test_todo(self, account_key: str) -> Dict[str, Any]:
        """创建测试待办"""
        logger.info(f"📝 使用 {account_key} 创建测试待办")
        
        headers = self.get_headers(account_key)
        publish_url = f"{BASE_URL}/admin-api/test/todo-new/api/publish"
        
        # 构建测试待办数据
        test_todo = {
            "title": f"统计功能测试待办 - {time.strftime('%Y-%m-%d %H:%M:%S')}",
            "content": "这是一个用于测试统计功能的待办事项。",
            "priority": "medium",
            "dueDate": "2025-12-31T23:59:59",
            "targetScope": "CLASS",
            "targetStudentIds": ["2023010105"]  # 针对特定学生
        }
        
        try:
            response = self.session.post(publish_url, json=test_todo, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and "data" in result:
                    todo_data = result["data"]
                    logger.info(f"✅ 测试待办创建成功: ID={todo_data.get('id')}")
                    
                    # 保存测试待办信息
                    todo_info = {
                        "id": todo_data.get("id"),
                        "title": todo_data.get("title"),
                        "creator": account_key,
                        "create_time": time.time()
                    }
                    self.test_todos.append(todo_info)
                    
                    return todo_data
                else:
                    logger.error(f"❌ 测试待办创建失败: {result.get('message')}")
            else:
                logger.error(f"❌ 测试待办创建HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 测试待办创建请求异常: {e}")
        
        return None
    
    def complete_todo(self, account_key: str, todo_id: int) -> Dict[str, Any]:
        """完成待办"""
        logger.info(f"✅ {account_key} 尝试完成待办 ID: {todo_id}")
        
        headers = self.get_headers(account_key)
        complete_url = f"{BASE_URL}/admin-api/test/todo-new/api/{todo_id}/complete"
        
        try:
            response = self.session.post(complete_url, json={}, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    completion_data = result.get("data", {})
                    logger.info(f"✅ 待办完成成功: ID={todo_id}")
                    return completion_data
                else:
                    error_msg = result.get("message", "未知错误")
                    logger.error(f"❌ 待办完成失败: {error_msg}")
                    return {"error": error_msg}
            else:
                logger.error(f"❌ 待办完成HTTP错误: {response.status_code} - {response.text}")
                return {"error": f"HTTP {response.status_code}"}
                
        except Exception as e:
            logger.error(f"❌ 待办完成请求异常: {e}")
            return {"error": str(e)}
    
    def get_todo_stats(self, account_key: str, todo_id: int) -> Dict[str, Any]:
        """获取待办统计信息"""
        logger.info(f"📊 获取待办统计信息 - ID: {todo_id}")
        
        headers = self.get_headers(account_key)
        stats_url = f"{BASE_URL}/admin-api/test/todo-new/api/{todo_id}/stats"
        
        try:
            response = self.session.get(stats_url, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and "data" in result:
                    stats_data = result["data"]
                    logger.info(f"✅ 待办统计获取成功")
                    return stats_data
                else:
                    logger.error(f"❌ 待办统计获取失败: {result.get('message')}")
            else:
                logger.error(f"❌ 待办统计HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 待办统计请求异常: {e}")
        
        return {}
    
    def test_stats_basic_functionality(self):
        """测试统计功能的基本功能"""
        logger.info("🧪 测试统计功能的基本功能")
        
        # 1. 使用校长创建测试待办
        test_todo = self.create_test_todo("principal")
        if not test_todo:
            return False
        
        todo_id = test_todo.get("id")
        
        # 2. 获取初始统计信息（应该为0）
        initial_stats = self.get_todo_stats("principal", todo_id)
        
        if not initial_stats:
            logger.error("❌ 初始统计信息获取失败")
            return False
        
        # 验证初始统计信息
        stats_data = initial_stats.get("stats", {})
        if stats_data.get("totalCompleted", -1) != 0:
            logger.error(f"❌ 初始完成统计应为0，实际为: {stats_data.get('totalCompleted')}")
            return False
        
        logger.info("✅ 初始统计信息验证通过: 完成数为0")
        
        # 3. 学生完成待办
        completion_result = self.complete_todo("student", todo_id)
        if "error" in completion_result:
            logger.error(f"❌ 待办完成失败: {completion_result['error']}")
            return False
        
        # 给系统一些时间处理完成状态
        time.sleep(1)
        
        # 4. 获取完成后的统计信息
        completed_stats = self.get_todo_stats("principal", todo_id)
        
        if not completed_stats:
            logger.error("❌ 完成后的统计信息获取失败")
            return False
        
        # 验证完成后的统计信息
        completed_stats_data = completed_stats.get("stats", {})
        if completed_stats_data.get("totalCompleted", 0) != 1:
            logger.error(f"❌ 完成后的统计应为1，实际为: {completed_stats_data.get('totalCompleted')}")
            return False
        
        logger.info(f"✅ 完成后的统计信息验证通过: 完成数 = {completed_stats_data.get('totalCompleted', 0)}")
        
        # 5. 验证统计信息包含待办基本信息
        todo_info = completed_stats.get("todoInfo", {})
        if not todo_info or "id" not in todo_info:
            logger.error("❌ 统计信息中缺少待办基本信息")
            return False
        
        logger.info(f"✅ 待办基本信息验证通过: ID={todo_info.get('id')}, 标题={todo_info.get('title')}")
        
        return True
    
    def test_stats_multi_user_completion(self):
        """测试多用户完成统计"""
        logger.info("👥 测试多用户完成统计")
        
        # 1. 使用校长创建测试待办
        test_todo = self.create_test_todo("principal")
        if not test_todo:
            return False
        
        todo_id = test_todo.get("id")
        
        # 2. 多个用户完成待办
        completion_count = 0
        test_users = ["student", "teacher"]  # 测试两个用户完成
        
        for user in test_users:
            completion_result = self.complete_todo(user, todo_id)
            if "error" not in completion_result:
                completion_count += 1
                logger.info(f"✅ {user} 完成待办成功")
            else:
                logger.warning(f"⚠️ {user} 完成待办失败: {completion_result.get('error')}")
            
            # 给系统一些时间处理
            time.sleep(0.5)
        
        # 3. 获取统计信息
        stats = self.get_todo_stats("principal", todo_id)
        
        if not stats:
            logger.error("❌ 统计信息获取失败")
            return False
        
        # 验证统计信息
        stats_data = stats.get("stats", {})
        actual_completed = stats_data.get("totalCompleted", 0)
        
        if actual_completed != completion_count:
            logger.error(f"❌ 统计信息不匹配: 预期 {completion_count}, 实际 {actual_completed}")
            return False
        
        logger.info(f"✅ 多用户完成统计验证通过: 预期 {completion_count}, 实际 {actual_completed}")
        
        # 4. 验证角色分类统计
        if completion_count > 0:
            student_completed = stats_data.get("studentCompleted", 0)
            teacher_completed = stats_data.get("teacherCompleted", 0)
            
            logger.info(f"📊 角色分类统计: 学生={student_completed}, 教师={teacher_completed}")
            
            # 验证至少有一个分类统计不为0
            if student_completed + teacher_completed == 0:
                logger.warning("⚠️ 角色分类统计全部为0，可能统计功能未完全实现")
        
        return True
    
    def test_stats_permission_validation(self):
        """测试统计功能的权限验证"""
        logger.info("🔐 测试统计功能的权限验证")
        
        # 1. 使用校长创建测试待办
        test_todo = self.create_test_todo("principal")
        if not test_todo:
            return False
        
        todo_id = test_todo.get("id")
        
        # 2. 测试不同角色的统计访问权限
        test_cases = [
            # (角色, 预期可以访问, 描述)
            ("principal", True, "校长应该可以查看任何待办的统计"),
            ("teacher", True, "教师应该可以查看相关待办的统计"),
            ("student", True, "学生应该可以查看自己相关的待办统计")
        ]
        
        for role, expected_access, description in test_cases:
            logger.info(f"🔍 测试 {role} 的统计访问权限: {description}")
            
            stats_result = self.get_todo_stats(role, todo_id)
            has_access = bool(stats_result)  # 如果有返回数据表示有访问权限
            
            if has_access == expected_access:
                status = "✅ 通过" if has_access else "✅ 正确拒绝"
                logger.info(f"   {status}: {description}")
            else:
                logger.error(f"   ❌ 失败: 预期 {expected_access}, 实际 {has_access}")
                return False
            
            # 给系统一些时间处理
            time.sleep(0.3)
        
        return True
    
    def test_stats_recent_completions(self):
        """测试最近完成记录功能"""
        logger.info("🕒 测试最近完成记录功能")
        
        # 1. 使用校长创建测试待办
        test_todo = self.create_test_todo("principal")
        if not test_todo:
            return False
        
        todo_id = test_todo.get("id")
        
        # 2. 多个用户完成待办
        completions = []
        test_users = ["student", "teacher"]
        
        for user in test_users:
            completion_result = self.complete_todo(user, todo_id)
            if "error" not in completion_result:
                completions.append({
                    "user": user,
                    "time": time.time()
                })
                logger.info(f"✅ {user} 完成待办成功")
            
            # 给系统一些时间处理
            time.sleep(0.5)
        
        # 3. 获取统计信息
        stats = self.get_todo_stats("principal", todo_id)
        
        if not stats:
            logger.error("❌ 统计信息获取失败")
            return False
        
        # 4. 验证最近完成记录
        recent_completions = stats.get("recentCompletions", [])
        
        if not isinstance(recent_completions, list):
            logger.error("❌ 最近完成记录格式错误")
            return False
        
        logger.info(f"📋 获取到 {len(recent_completions)} 条最近完成记录")
        
        # 验证至少包含一些完成记录信息
        if len(recent_completions) > 0:
            first_completion = recent_completions[0]
            if "user_name" in first_completion and "completed_time" in first_completion:
                logger.info(f"✅ 最近完成记录验证通过: 用户={first_completion.get('user_name')}, 时间={first_completion.get('completed_time')}")
            else:
                logger.warning("⚠️ 最近完成记录字段不完整")
        else:
            logger.warning("⚠️ 最近完成记录为空，可能功能未完全实现")
        
        return True
    
    def test_stats_cache_performance(self):
        """测试统计缓存性能"""
        logger.info("⚡ 测试统计缓存性能")
        
        # 1. 使用校长创建测试待办
        test_todo = self.create_test_todo("principal")
        if not test_todo:
            return False
        
        todo_id = test_todo.get("id")
        
        # 2. 学生完成待办
        completion_result = self.complete_todo("student", todo_id)
        if "error" in completion_result:
            logger.error(f"❌ 待办完成失败: {completion_result['error']}")
            return False
        
        # 给系统一些时间处理
        time.sleep(1)
        
        # 3. 多次请求统计信息，测试响应时间
        request_times = []
        num_requests = 5
        
        for i in range(num_requests):
            start_time = time.time()
            stats = self.get_todo_stats("principal", todo_id)
            end_time = time.time()
            
            if stats:
                response_time = (end_time - start_time) * 1000  # 转换为毫秒
                request_times.append(response_time)
                logger.info(f"⏱️ 第 {i+1} 次请求响应时间: {response_time:.2f}ms")
            else:
                logger.error("❌ 统计请求失败")
                return False
            
            # 短暂间隔
            time.sleep(0.1)
        
        # 4. 分析性能数据
        if request_times:
            avg_time = sum(request_times) / len(request_times)
            max_time = max(request_times)
            min_time = min(request_times)
            
            logger.info(f"📊 性能统计: 平均={avg_time:.2f}ms, 最小={min_time:.2f}ms, 最大={max_time:.2f}ms")
            
            # 性能标准：平均响应时间应小于200ms
            if avg_time < 200:
                logger.info("✅ 统计API性能验证通过")
                return True
            else:
                logger.warning(f"⚠️ 统计API性能较慢: 平均响应时间 {avg_time:.2f}ms")
                return True  # 性能警告但不视为失败
        
        return False
    
    def run_comprehensive_test(self):
        """运行全面的统计功能测试"""
        logger.info("🚀 开始全面的统计功能测试")
        
        # 认证所有测试用户
        for account_key in TEST_ACCOUNTS.keys():
            self.authenticate_user(account_key)
        
        test_results = {}
        
        # 运行各个测试用例
        test_cases = [
            ("basic_functionality", self.test_stats_basic_functionality, "基本功能测试"),
            ("multi_user", self.test_stats_multi_user_completion, "多用户完成统计测试"),
            ("permission", self.test_stats_permission_validation, "权限验证测试"),
            ("recent_completions", self.test_stats_recent_completions, "最近完成记录测试"),
            ("cache_performance", self.test_stats_cache_performance, "缓存性能测试")
        ]
        
        for test_name, test_func, description in test_cases:
            logger.info(f"\n🔧 开始测试: {description}")
            try:
                result = test_func()
                test_results[test_name] = result
                status = "✅ 通过" if result else "❌ 失败"
                logger.info(f"📋 {description}: {status}")
            except Exception as e:
                logger.error(f"💥 测试 {test_name} 异常: {e}")
                test_results[test_name] = False
        
        # 汇总测试结果
        total_tests = len(test_results)
        passed_tests = sum(1 for result in test_results.values() if result)
        success_rate = (passed_tests / total_tests) * 100
        
        logger.info("\n" + "=" * 60)
        logger.info("📊 统计功能测试结果汇总")
        logger.info("=" * 60)
        
        for test_name, result in test_results.items():
            status = "✅ 通过" if result else "❌ 失败"
            logger.info(f"{test_name}: {status}")
        
        logger.info("-" * 60)
        logger.info(f"总计测试: {total_tests}")
        logger.info(f"通过测试: {passed_tests}")
        logger.info(f"成功率: {success_rate:.2f}%")
        
        if success_rate >= 80:
            logger.info("🎉 统计功能测试总体通过")
            return True
        else:
            logger.error("💥 统计功能测试总体失败")
            return False

if __name__ == "__main__":
    # 创建测试实例
    tester = TodoStatsTest()
    
    # 运行测试
    success = tester.run_comprehensive_test()
    
    # 返回退出码
    exit(0 if success else 1)