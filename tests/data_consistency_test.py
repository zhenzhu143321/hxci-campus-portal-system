#!/usr/bin/env python3
"""
哈尔滨信息工程学院校园门户系统 - 数据一致性验证测试

测试目标：验证CRUD操作和权限过滤准确性
测试范围：通知发布、查询、完成、统计API的数据一致性
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

class DataConsistencyTest:
    """数据一致性验证测试类"""
    
    def __init__(self):
        self.session = requests.Session()
        self.tokens = {}
        self.test_data = []
        
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
                if auth_result.get("success") and "data" in auth_result:
                    token = auth_result["data"].get("token")
                    if token:
                        self.tokens[account_key] = token
                        logger.info(f"✅ 用户 {account['name']} 认证成功")
                        return token
            
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
    
    def test_notification_publish(self, account_key: str) -> Dict[str, Any]:
        """测试通知发布功能"""
        logger.info(f"📝 测试 {account_key} 的通知发布功能")
        
        headers = self.get_headers(account_key)
        publish_url = f"{BASE_URL}/admin-api/test/notification/api/publish-database"
        
        # 构建测试通知数据
        test_notification = {
            "title": f"测试通知 - {time.strftime('%Y-%m-%d %H:%M:%S')}",
            "content": "这是一个测试通知内容，用于验证数据一致性。",
            "level": 3,  # 常规通知
            "targetScope": "SCHOOL_WIDE"
        }
        
        try:
            response = self.session.post(publish_url, json=test_notification, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and "data" in result:
                    notification_data = result["data"]
                    logger.info(f"✅ 通知发布成功: ID={notification_data.get('notificationId')}")
                    
                    # 保存测试数据用于后续验证
                    self.test_data.append({
                        "id": notification_data.get("notificationId"),
                        "title": test_notification["title"],
                        "publisher": TEST_ACCOUNTS[account_key]["name"],
                        "timestamp": time.time()
                    })
                    
                    return notification_data
                else:
                    logger.error(f"❌ 通知发布失败: {result.get('message')}")
            else:
                logger.error(f"❌ 通知发布HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 通知发布请求异常: {e}")
        
        return None
    
    def test_notification_list(self, account_key: str) -> List[Dict[str, Any]]:
        """测试通知列表查询功能"""
        logger.info(f"📋 测试 {account_key} 的通知列表查询")
        
        headers = self.get_headers(account_key)
        list_url = f"{BASE_URL}/admin-api/test/notification/api/list"
        
        try:
            response = self.session.get(list_url, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and "data" in result:
                    notifications = result["data"].get("notifications", [])
                    logger.info(f"✅ 获取到 {len(notifications)} 条通知")
                    return notifications
                else:
                    logger.error(f"❌ 通知列表查询失败: {result.get('message')}")
            else:
                logger.error(f"❌ 通知列表查询HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 通知列表查询请求异常: {e}")
        
        return []
    
    def test_todo_publish(self, account_key: str) -> Dict[str, Any]:
        """测试待办发布功能"""
        logger.info(f"📝 测试 {account_key} 的待办发布功能")
        
        headers = self.get_headers(account_key)
        publish_url = f"{BASE_URL}/admin-api/test/todo-new/api/publish"
        
        # 构建测试待办数据
        test_todo = {
            "title": f"测试待办 - {time.strftime('%Y-%m-%d %H:%M:%S')}",
            "content": "这是一个测试待办内容，用于验证数据一致性。",
            "priority": "medium",
            "dueDate": "2025-12-31T23:59:59",
            "targetScope": "CLASS"
        }
        
        try:
            response = self.session.post(publish_url, json=test_todo, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and "data" in result:
                    todo_data = result["data"]
                    logger.info(f"✅ 待办发布成功: ID={todo_data.get('id')}")
                    return todo_data
                else:
                    logger.error(f"❌ 待办发布失败: {result.get('message')}")
            else:
                logger.error(f"❌ 待办发布HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 待办发布请求异常: {e}")
        
        return None
    
    def test_todo_list(self, account_key: str) -> List[Dict[str, Any]]:
        """测试待办列表查询功能"""
        logger.info(f"📋 测试 {account_key} 的待办列表查询")
        
        headers = self.get_headers(account_key)
        list_url = f"{BASE_URL}/admin-api/test/todo-new/api/my-list"
        
        try:
            response = self.session.get(list_url, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and "data" in result:
                    todos = result["data"].get("todos", [])
                    logger.info(f"✅ 获取到 {len(todos)} 条待办")
                    return todos
                else:
                    logger.error(f"❌ 待办列表查询失败: {result.get('message')}")
            else:
                logger.error(f"❌ 待办列表查询HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 待办列表查询请求异常: {e}")
        
        return []
    
    def test_todo_complete(self, account_key: str, todo_id: int) -> bool:
        """测试待办完成功能"""
        logger.info(f"✅ 测试 {account_key} 的待办完成功能 - ID: {todo_id}")
        
        headers = self.get_headers(account_key)
        complete_url = f"{BASE_URL}/admin-api/test/todo-new/api/{todo_id}/complete"
        
        try:
            response = self.session.post(complete_url, json={}, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success"):
                    logger.info(f"✅ 待办完成成功: ID={todo_id}")
                    return True
                else:
                    logger.error(f"❌ 待办完成失败: {result.get('message')}")
            else:
                logger.error(f"❌ 待办完成HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 待办完成请求异常: {e}")
        
        return False
    
    def test_todo_stats(self, account_key: str, todo_id: int) -> Dict[str, Any]:
        """测试待办统计功能"""
        logger.info(f"📊 测试 {account_key} 的待办统计功能 - ID: {todo_id}")
        
        headers = self.get_headers(account_key)
        stats_url = f"{BASE_URL}/admin-api/test/todo-new/api/{todo_id}/stats"
        
        try:
            response = self.session.get(stats_url, headers=headers, timeout=10)
            if response.status_code == 200:
                result = response.json()
                if result.get("success") and "data" in result:
                    stats_data = result["data"]
                    logger.info(f"✅ 待办统计获取成功: ID={todo_id}")
                    return stats_data
                else:
                    logger.error(f"❌ 待办统计获取失败: {result.get('message')}")
            else:
                logger.error(f"❌ 待办统计HTTP错误: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.error(f"❌ 待办统计请求异常: {e}")
        
        return {}
    
    def verify_data_consistency(self, account_key: str):
        """验证数据一致性"""
        logger.info(f"🔍 开始验证 {account_key} 的数据一致性")
        
        # 1. 发布测试待办
        published_todo = self.test_todo_publish(account_key)
        if not published_todo:
            logger.error("❌ 数据一致性验证失败: 待办发布失败")
            return False
        
        todo_id = published_todo.get("id")
        
        # 2. 验证待办列表包含新发布的待办
        todos = self.test_todo_list(account_key)
        found_todo = any(todo.get("id") == todo_id for todo in todos)
        
        if not found_todo:
            logger.error(f"❌ 数据一致性验证失败: 待办列表未包含新发布的待办 ID={todo_id}")
            return False
        
        logger.info(f"✅ 数据一致性验证通过: 待办发布后列表查询成功")
        
        # 3. 验证待办统计
        stats = self.test_todo_stats(account_key, todo_id)
        if not stats:
            logger.error(f"❌ 数据一致性验证失败: 待办统计获取失败")
            return False
        
        logger.info(f"✅ 待办统计验证通过: {json.dumps(stats, ensure_ascii=False, indent=2)}")
        
        # 4. 验证待办完成
        completion_success = self.test_todo_complete(account_key, todo_id)
        if not completion_success:
            logger.error(f"❌ 数据一致性验证失败: 待办完成失败")
            return False
        
        logger.info(f"✅ 待办完成验证通过")
        
        # 5. 验证完成后的待办列表
        completed_todos = self.test_todo_list(account_key)
        completed_todo = next((todo for todo in completed_todos if todo.get("id") == todo_id), None)
        
        if completed_todo and completed_todo.get("isCompleted"):
            logger.info(f"✅ 完成状态一致性验证通过: 待办标记为已完成")
            return True
        else:
            logger.error(f"❌ 完成状态一致性验证失败: 待办未正确标记为已完成")
            return False
    
    def verify_permission_filtering(self):
        """验证权限过滤准确性"""
        logger.info("🔐 开始验证权限过滤准确性")
        
        # 使用校长账号发布一个全校范围的通知
        principal_headers = self.get_headers("principal")
        publish_url = f"{BASE_URL}/admin-api/test/notification/api/publish-database"
        
        test_notification = {
            "title": f"权限测试通知 - {time.strftime('%Y-%m-%d %H:%M:%S')}",
            "content": "这是一个权限测试通知，用于验证不同角色的查看权限。",
            "level": 3,
            "targetScope": "SCHOOL_WIDE"
        }
        
        try:
            response = self.session.post(publish_url, json=test_notification, headers=principal_headers, timeout=10)
            if response.status_code != 200:
                logger.error("❌ 权限测试通知发布失败")
                return False
            
            # 给系统一些时间处理通知
            time.sleep(2)
            
            # 验证不同角色都能看到全校范围的通知
            roles_to_test = ["principal", "teacher", "student"]
            
            for role in roles_to_test:
                notifications = self.test_notification_list(role)
                if not notifications:
                    logger.error(f"❌ {role} 角色无法获取通知列表")
                    return False
                
                # 检查是否包含全校范围的通知
                school_wide_notifications = [
                    n for n in notifications 
                    if n.get("targetScope") == "SCHOOL_WIDE"
                ]
                
                if not school_wide_notifications:
                    logger.error(f"❌ {role} 角色无法看到全校范围的通知")
                    return False
                
                logger.info(f"✅ {role} 角色权限过滤验证通过: 能看到 {len(school_wide_notifications)} 条全校通知")
            
            return True
            
        except Exception as e:
            logger.error(f"❌ 权限过滤验证异常: {e}")
            return False
    
    def run_comprehensive_test(self):
        """运行全面的数据一致性测试"""
        logger.info("🚀 开始全面的数据一致性测试")
        
        test_results = {}
        
        # 1. 认证所有测试用户
        for account_key in TEST_ACCOUNTS.keys():
            token = self.authenticate_user(account_key)
            test_results[f"auth_{account_key}"] = token is not None
        
        # 2. 测试数据一致性（使用校长账号）
        test_results["data_consistency"] = self.verify_data_consistency("principal")
        
        # 3. 测试权限过滤
        test_results["permission_filtering"] = self.verify_permission_filtering()
        
        # 4. 测试通知功能
        test_results["notification_publish"] = self.test_notification_publish("principal") is not None
        
        # 汇总测试结果
        total_tests = len(test_results)
        passed_tests = sum(1 for result in test_results.values() if result)
        success_rate = (passed_tests / total_tests) * 100
        
        logger.info("=" * 60)
        logger.info("📊 数据一致性测试结果汇总")
        logger.info("=" * 60)
        
        for test_name, result in test_results.items():
            status = "✅ 通过" if result else "❌ 失败"
            logger.info(f"{test_name}: {status}")
        
        logger.info("-" * 60)
        logger.info(f"总计测试: {total_tests}")
        logger.info(f"通过测试: {passed_tests}")
        logger.info(f"成功率: {success_rate:.2f}%")
        
        if success_rate >= 80:
            logger.info("🎉 数据一致性测试总体通过")
            return True
        else:
            logger.error("💥 数据一致性测试总体失败")
            return False

if __name__ == "__main__":
    # 创建测试实例
    tester = DataConsistencyTest()
    
    # 运行测试
    success = tester.run_comprehensive_test()
    
    # 返回退出码
    exit(0 if success else 1)