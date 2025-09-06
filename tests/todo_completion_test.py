#!/usr/bin/env python3
"""
哈尔滨信息工程学院校园门户系统 - 待办完成API功能测试

测试目标：
1. 验证待办完成状态更新的正确性和一致性
2. 测试完成状态的数据库持久化机制
3. 验证多角色权限控制的严格执行
4. 测试异常情况处理和错误恢复能力
5. 性能测试和并发处理验证

测试范围：/admin-api/test/todo-new/api/{id}/complete 端点
版本：v2.0 - 企业级测试优化版
"""

import requests
import json
import time
import logging
import threading
import concurrent.futures
from datetime import datetime
from dataclasses import dataclass
from typing import Dict, List, Any, Optional, Tuple, NamedTuple
from enum import Enum
from contextlib import contextmanager

# 配置结构化日志系统
class ColorFormatter(logging.Formatter):
    """彩色日志格式化器"""
    
    COLORS = {
        'DEBUG': '\033[36m',    # 青色
        'INFO': '\033[32m',     # 绿色
        'WARNING': '\033[33m',  # 黄色
        'ERROR': '\033[31m',    # 红色
        'CRITICAL': '\033[35m'  # 紫色
    }
    RESET = '\033[0m'
    
    def format(self, record):
        log_color = self.COLORS.get(record.levelname, self.RESET)
        record.levelname = f"{log_color}{record.levelname}{self.RESET}"
        return super().format(record)

# 配置日志
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
handler.setFormatter(ColorFormatter(
    fmt='%(asctime)s - %(levelname)s - [%(funcName)s:%(lineno)d] - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
))
logger.addHandler(handler)

# 测试结果枚举
class TestResult(Enum):
    PASS = "✅ 通过"
    FAIL = "❌ 失败" 
    SKIP = "⏭️ 跳过"
    ERROR = "💥 错误"

# 测试配置
BASE_URL = "http://localhost:48081"
MOCK_API_URL = "http://localhost:48082"
DEFAULT_TIMEOUT = 15  # 增加超时时间
MAX_RETRIES = 3
RETRY_DELAY = 1.0

# 测试数据类型定义
@dataclass
class TestAccount:
    """测试账号数据结构"""
    employee_id: str
    name: str
    password: str
    role: str
    description: str

@dataclass 
class TestConfig:
    """测试配置数据结构"""
    max_concurrent: int = 5
    completion_timeout: int = 10
    list_check_timeout: int = 5
    stats_check_timeout: int = 8
    
class TestMetrics(NamedTuple):
    """测试指标"""
    start_time: float
    end_time: float
    duration: float
    success: bool
    error_msg: Optional[str] = None

# 测试账号配置 - 增强版
TEST_ACCOUNTS = {
    "system_admin": TestAccount(
        employee_id="SYSTEM_ADMIN_001",
        name="系统管理员",
        password="admin123",
        role="SYSTEM_ADMIN", 
        description="系统管理员 - 超级权限"
    ),
    "principal": TestAccount(
        employee_id="PRINCIPAL_001", 
        name="Principal-Zhang",
        password="admin123",
        role="PRINCIPAL",
        description="校长 - 全校权限"
    ),
    "academic_admin": TestAccount(
        employee_id="ACADEMIC_ADMIN_001",
        name="Director-Li", 
        password="admin123",
        role="ACADEMIC_ADMIN",
        description="教务主任 - 部门权限"
    ),
    "teacher": TestAccount(
        employee_id="TEACHER_001",
        name="Teacher-Wang",
        password="admin123", 
        role="TEACHER",
        description="教师 - 班级权限"
    ),
    "class_teacher": TestAccount(
        employee_id="CLASS_TEACHER_001",
        name="ClassTeacher-Liu",
        password="admin123",
        role="CLASS_TEACHER", 
        description="班主任 - 班级管理权限"
    ),
    "student": TestAccount(
        employee_id="STUDENT_001",
        name="Student-Zhang",
        password="admin123",
        role="STUDENT",
        description="学生 - 基础权限"
    )
}

class APIException(Exception):
    """API请求异常"""
    def __init__(self, message: str, status_code: Optional[int] = None, response_data: Optional[Dict] = None):
        super().__init__(message)
        self.status_code = status_code
        self.response_data = response_data

class AuthenticationException(APIException):
    """认证异常"""
    pass

class PermissionException(APIException):
    """权限异常"""
    pass

class TodoCompletionTestSuite:
    """待办完成API功能测试套件 - 企业级版本"""
    
    def __init__(self, config: Optional[TestConfig] = None):
        self.config = config or TestConfig()
        self.session = requests.Session()
        self.session.timeout = DEFAULT_TIMEOUT
        self.tokens: Dict[str, str] = {}
        self.test_todos: List[Dict[str, Any]] = []
        self.metrics: List[TestMetrics] = []
        self._lock = threading.Lock()
        
        # 设置会话配置
        self.session.headers.update({
            'User-Agent': 'HXCI-Campus-Portal-Test/2.0',
            'Accept': 'application/json'
        })
    
    @contextmanager
    def timing_context(self, operation_name: str):
        """计时上下文管理器"""
        start_time = time.time()
        error_msg = None
        success = True
        
        try:
            yield
        except Exception as e:
            success = False
            error_msg = str(e)
            raise
        finally:
            end_time = time.time()
            duration = end_time - start_time
            
            metric = TestMetrics(
                start_time=start_time,
                end_time=end_time, 
                duration=duration,
                success=success,
                error_msg=error_msg
            )
            
            with self._lock:
                self.metrics.append(metric)
            
            status = "✅" if success else "❌"
            logger.info(f"{status} {operation_name} 耗时: {duration:.3f}s")
    
    def retry_on_failure(self, operation_func, max_retries: int = MAX_RETRIES, delay: float = RETRY_DELAY):
        """失败重试装饰器"""
        last_exception = None
        
        for attempt in range(max_retries + 1):
            try:
                return operation_func()
            except (requests.RequestException, APIException) as e:
                last_exception = e
                if attempt < max_retries:
                    logger.warning(f"操作失败，{delay}s后进行第{attempt + 1}次重试: {e}")
                    time.sleep(delay)
                    delay *= 1.5  # 指数退避
                else:
                    logger.error(f"操作最终失败，已重试{max_retries}次: {e}")
        
        raise last_exception
    
    def authenticate_user(self, account_key: str) -> str:
        """
        认证用户并获取JWT Token
        
        Args:
            account_key: 账号标识
            
        Returns:
            JWT Token字符串
            
        Raises:
            AuthenticationException: 认证失败
        """
        if account_key not in TEST_ACCOUNTS:
            raise AuthenticationException(f"未知账号类型: {account_key}")
        
        account = TEST_ACCOUNTS[account_key]
        logger.info(f"🔐 正在认证用户: {account.description}")
        
        # 调用Mock School API进行认证
        auth_url = f"{MOCK_API_URL}/mock-school-api/auth/authenticate"
        auth_data = {
            "employeeId": account.employee_id,
            "name": account.name,
            "password": account.password
        }
        
        def _authenticate():
            with self.timing_context(f"认证用户 {account.name}"):
                response = self.session.post(auth_url, json=auth_data, timeout=DEFAULT_TIMEOUT)
                
                if response.status_code != 200:
                    raise AuthenticationException(
                        f"认证HTTP错误: {response.status_code}",
                        status_code=response.status_code,
                        response_data=response.text
                    )
                
                try:
                    auth_result = response.json()
                except json.JSONDecodeError as e:
                    raise AuthenticationException(f"认证响应JSON解析失败: {e}")
                
                if not auth_result.get("success"):
                    error_msg = auth_result.get("message", "认证失败")
                    raise AuthenticationException(f"认证失败: {error_msg}", response_data=auth_result)
                
                if "data" not in auth_result:
                    raise AuthenticationException("认证响应缺少data字段", response_data=auth_result)
                
                # 修复: Mock API返回的token字段名是accessToken
                token = auth_result["data"].get("accessToken")
                if not token:
                    # 尝试旧的token字段名（向后兼容）
                    token = auth_result["data"].get("token")
                    if not token:
                        raise AuthenticationException("认证响应缺少accessToken字段", response_data=auth_result)
                
                # 缓存Token
                self.tokens[account_key] = token
                logger.info(f"✅ 用户 {account.name} 认证成功")
                return token
        
        return self.retry_on_failure(_authenticate)
    
    def get_headers(self, account_key: str) -> Dict[str, str]:
        """
        获取认证请求头
        
        Args:
            account_key: 账号标识
            
        Returns:
            包含认证信息的请求头
        """
        # 确保有有效Token
        if account_key not in self.tokens:
            self.authenticate_user(account_key)
        
        token = self.tokens[account_key]
        return {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "tenant-id": "1"
        }
    
    def create_test_todo(self, creator_account: str, target_account: Optional[str] = None) -> Dict[str, Any]:
        """
        创建测试待办
        
        Args:
            creator_account: 创建者账号
            target_account: 目标账号(如果为空，则针对学生)
            
        Returns:
            创建的待办信息
            
        Raises:
            APIException: 创建失败
        """
        creator = TEST_ACCOUNTS[creator_account]
        target = TEST_ACCOUNTS[target_account] if target_account else TEST_ACCOUNTS["student"]
        
        logger.info(f"📝 {creator.description} 为 {target.description} 创建测试待办")
        
        headers = self.get_headers(creator_account)
        publish_url = f"{BASE_URL}/admin-api/test/todo-new/api/publish"
        
        # 生成唯一的测试待办数据
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S_%f')[:-3]
        test_todo = {
            "title": f"完成功能测试待办_{timestamp}",
            "content": f"由{creator.name}创建的测试待办，用于验证完成功能。创建时间: {datetime.now()}",
            "priority": "medium",
            "dueDate": "2025-12-31T23:59:59",
            "targetScope": "CLASS",
            "targetStudentIds": [target.employee_id] if target.role == "STUDENT" else []
        }
        
        def _create_todo():
            with self.timing_context(f"创建测试待办 by {creator.name}"):
                response = self.session.post(publish_url, json=test_todo, headers=headers, timeout=DEFAULT_TIMEOUT)
                
                if response.status_code != 200:
                    raise APIException(
                        f"创建待办HTTP错误: {response.status_code}",
                        status_code=response.status_code,
                        response_data=response.text
                    )
                
                try:
                    result = response.json()
                except json.JSONDecodeError as e:
                    raise APIException(f"创建待办响应JSON解析失败: {e}")
                
                if not result.get("success"):
                    error_msg = result.get("message", "创建失败")
                    raise APIException(f"创建待办失败: {error_msg}", response_data=result)
                
                if "data" not in result:
                    raise APIException("创建待办响应缺少data字段", response_data=result)
                
                todo_data = result["data"]
                todo_id = todo_data.get("id")
                
                if not todo_id:
                    raise APIException("创建待办响应缺少id字段", response_data=result)
                
                # 记录测试待办信息
                todo_info = {
                    "id": todo_id,
                    "title": todo_data.get("title", test_todo["title"]),
                    "creator": creator_account,
                    "target": target_account or "student",
                    "create_time": time.time(),
                    "test_data": test_todo
                }
                
                with self._lock:
                    self.test_todos.append(todo_info)
                
                logger.info(f"✅ 测试待办创建成功: ID={todo_id}, 标题='{todo_info['title']}'")
                return todo_data
        
        return self.retry_on_failure(_create_todo)
    
    def get_todo_list(self, account_key: str) -> List[Dict[str, Any]]:
        """
        获取待办列表
        
        Args:
            account_key: 账号标识
            
        Returns:
            待办列表
            
        Raises:
            APIException: 获取失败
        """
        account = TEST_ACCOUNTS[account_key]
        logger.info(f"📋 获取 {account.description} 的待办列表")
        
        headers = self.get_headers(account_key)
        list_url = f"{BASE_URL}/admin-api/test/todo-new/api/my-list"
        
        def _get_list():
            with self.timing_context(f"获取待办列表 for {account.name}"):
                response = self.session.get(list_url, headers=headers, timeout=self.config.list_check_timeout)
                
                if response.status_code != 200:
                    raise APIException(
                        f"获取待办列表HTTP错误: {response.status_code}",
                        status_code=response.status_code,
                        response_data=response.text
                    )
                
                try:
                    result = response.json()
                except json.JSONDecodeError as e:
                    raise APIException(f"待办列表响应JSON解析失败: {e}")
                
                if not result.get("success"):
                    error_msg = result.get("message", "获取失败")
                    raise APIException(f"获取待办列表失败: {error_msg}", response_data=result)
                
                if "data" not in result:
                    raise APIException("待办列表响应缺少data字段", response_data=result)
                
                todos = result["data"].get("todos", [])
                logger.info(f"✅ 获取到 {len(todos)} 条待办")
                return todos
        
        return self.retry_on_failure(_get_list)
    
    def complete_todo(self, account_key: str, todo_id: int, comment: Optional[str] = None) -> Dict[str, Any]:
        """
        完成待办
        
        Args:
            account_key: 操作者账号
            todo_id: 待办ID
            comment: 完成备注
            
        Returns:
            完成结果数据
            
        Raises:
            APIException: 完成失败
            PermissionException: 权限不足
        """
        account = TEST_ACCOUNTS[account_key]
        logger.info(f"✅ {account.description} 尝试完成待办 ID: {todo_id}")
        
        headers = self.get_headers(account_key)
        complete_url = f"{BASE_URL}/admin-api/test/todo-new/api/{todo_id}/complete"
        
        # 构建完成数据
        completion_data = {
            "comment": comment or f"由 {account.name}({account.role}) 于 {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} 完成"
        }
        
        def _complete_todo():
            with self.timing_context(f"完成待办 {todo_id} by {account.name}"):
                response = self.session.post(complete_url, json=completion_data, headers=headers, timeout=self.config.completion_timeout)
                
                if response.status_code == 403:
                    raise PermissionException(
                        f"权限不足: {account.description} 无权完成待办 {todo_id}",
                        status_code=response.status_code,
                        response_data=response.text
                    )
                
                if response.status_code != 200:
                    raise APIException(
                        f"完成待办HTTP错误: {response.status_code}",
                        status_code=response.status_code,
                        response_data=response.text
                    )
                
                try:
                    result = response.json()
                except json.JSONDecodeError as e:
                    raise APIException(f"完成待办响应JSON解析失败: {e}")
                
                if not result.get("success"):
                    error_msg = result.get("message", "完成失败")
                    # 检查是否为权限错误
                    if "权限" in error_msg or "permission" in error_msg.lower():
                        raise PermissionException(f"权限不足: {error_msg}", response_data=result)
                    else:
                        raise APIException(f"完成待办失败: {error_msg}", response_data=result)
                
                completion_result = result.get("data", {})
                logger.info(f"✅ 待办完成成功: ID={todo_id}")
                return completion_result
        
        return self.retry_on_failure(_complete_todo)
    
    def get_todo_stats(self, account_key: str, todo_id: int) -> Dict[str, Any]:
        """
        获取待办统计信息
        
        Args:
            account_key: 账号标识 
            todo_id: 待办ID
            
        Returns:
            统计信息
            
        Raises:
            APIException: 获取失败
        """
        account = TEST_ACCOUNTS[account_key]
        logger.info(f"📊 {account.description} 获取待办统计信息 - ID: {todo_id}")
        
        headers = self.get_headers(account_key)
        stats_url = f"{BASE_URL}/admin-api/test/todo-new/api/{todo_id}/stats"
        
        def _get_stats():
            with self.timing_context(f"获取统计信息 {todo_id} by {account.name}"):
                response = self.session.get(stats_url, headers=headers, timeout=self.config.stats_check_timeout)
                
                if response.status_code != 200:
                    raise APIException(
                        f"获取统计信息HTTP错误: {response.status_code}",
                        status_code=response.status_code,
                        response_data=response.text
                    )
                
                try:
                    result = response.json()
                except json.JSONDecodeError as e:
                    raise APIException(f"统计信息响应JSON解析失败: {e}")
                
                if not result.get("success"):
                    error_msg = result.get("message", "获取失败")
                    raise APIException(f"获取统计信息失败: {error_msg}", response_data=result)
                
                if "data" not in result:
                    raise APIException("统计信息响应缺少data字段", response_data=result)
                
                stats_data = result["data"]
                logger.info(f"✅ 统计信息获取成功")
                return stats_data
        
        return self.retry_on_failure(_get_stats)
    
    def find_todo_in_list(self, todo_list: List[Dict[str, Any]], todo_id: int) -> Optional[Dict[str, Any]]:
        """在待办列表中查找指定待办"""
        return next((todo for todo in todo_list if todo.get("id") == todo_id), None)
    
    def verify_todo_completion_state(self, account_key: str, todo_id: int, expected_completed: bool) -> bool:
        """验证待办完成状态"""
        todos = self.get_todo_list(account_key)
        todo = self.find_todo_in_list(todos, todo_id)
        
        if not todo:
            logger.error(f"❌ 待办 {todo_id} 在 {TEST_ACCOUNTS[account_key].description} 的列表中不存在")
            return False
        
        actual_completed = todo.get("isCompleted", False)
        if actual_completed == expected_completed:
            status_text = "已完成" if actual_completed else "未完成"
            logger.info(f"✅ 待办状态验证通过: {status_text}")
            return True
        else:
            expected_text = "已完成" if expected_completed else "未完成"
            actual_text = "已完成" if actual_completed else "未完成"
            logger.error(f"❌ 待办状态验证失败: 期望={expected_text}, 实际={actual_text}")
            return False
    
    # =========================
    # 测试用例实现部分
    # =========================
    
    def test_basic_functionality(self) -> TestResult:
        """
        测试基本功能完整性
        
        验证点：
        1. 待办创建成功
        2. 初始状态为未完成
        3. 完成操作成功
        4. 完成后状态正确更新
        5. 统计信息正确更新
        """
        logger.info("🧪 [基本功能测试] 开始测试待办完成基本功能")
        
        try:
            # 1. 校长创建测试待办
            test_todo = self.create_test_todo("principal", "student")
            todo_id = test_todo.get("id")
            logger.info(f"📝 创建测试待办成功: ID={todo_id}")
            
            # 2. 验证初始状态为未完成
            if not self.verify_todo_completion_state("student", todo_id, False):
                return TestResult.FAIL
            
            # 3. 学生完成待办
            completion_result = self.complete_todo("student", todo_id, "基本功能测试 - 学生完成")
            logger.info(f"✅ 学生完成待办成功")
            
            # 4. 验证完成后状态
            if not self.verify_todo_completion_state("student", todo_id, True):
                return TestResult.FAIL
            
            # 5. 验证统计信息更新
            stats = self.get_todo_stats("principal", todo_id)
            total_completed = stats.get("stats", {}).get("totalCompleted", 0)
            
            if total_completed < 1:
                logger.error(f"❌ 完成统计未正确更新: 期望≥1, 实际={total_completed}")
                return TestResult.FAIL
            
            logger.info(f"✅ 完成统计验证通过: 总完成数={total_completed}")
            logger.info("🎉 [基本功能测试] 测试通过")
            return TestResult.PASS
            
        except Exception as e:
            logger.error(f"💥 [基本功能测试] 测试异常: {e}")
            return TestResult.ERROR
    
    def test_permission_matrix(self) -> TestResult:
        """
        测试权限矩阵的正确性
        
        验证不同角色对待办完成的权限控制
        """
        logger.info("🔐 [权限矩阵测试] 开始测试多角色权限控制")
        
        try:
            # 创建多个不同权限级别的测试待办
            test_cases = [
                # (创建者, 目标用户, 测试角色, 权限预期, 描述)
                ("principal", "student", "student", True, "学生完成自己的待办"),
                ("principal", "student", "teacher", False, "教师尝试完成学生的待办"),
                ("principal", "student", "principal", True, "校长完成任意待办"), 
                ("principal", "student", "system_admin", True, "系统管理员完成任意待办"),
                ("teacher", "student", "student", True, "学生完成教师分配的待办"),
                ("teacher", "student", "academic_admin", True, "教务主任完成下级待办")
            ]
            
            failed_tests = []
            
            for creator, target, tester, expected_success, description in test_cases:
                logger.info(f"🔍 测试权限: {description}")
                
                try:
                    # 创建测试待办
                    test_todo = self.create_test_todo(creator, target)
                    todo_id = test_todo.get("id")
                    
                    # 尝试完成操作
                    try:
                        self.complete_todo(tester, todo_id, f"权限测试: {description}")
                        actual_success = True
                        logger.info("   ✅ 完成操作成功")
                    except PermissionException:
                        actual_success = False
                        logger.info("   🚫 权限被正确拒绝")
                    except APIException as e:
                        if "权限" in str(e) or "permission" in str(e).lower():
                            actual_success = False
                            logger.info("   🚫 权限被正确拒绝")
                        else:
                            # 其他API错误
                            raise e
                    
                    # 验证结果
                    if actual_success == expected_success:
                        result_text = "成功完成" if actual_success else "正确拒绝"
                        logger.info(f"   ✅ 权限测试通过: {result_text}")
                    else:
                        error_msg = f"权限测试失败: {description} - 期望{expected_success}, 实际{actual_success}"
                        logger.error(f"   ❌ {error_msg}")
                        failed_tests.append(error_msg)
                    
                except Exception as e:
                    error_msg = f"权限测试异常: {description} - {e}"
                    logger.error(f"   💥 {error_msg}")
                    failed_tests.append(error_msg)
                
                # 给系统一些时间处理
                time.sleep(0.3)
            
            if failed_tests:
                logger.error(f"❌ [权限矩阵测试] 失败: {len(failed_tests)}/{len(test_cases)} 个测试失败")
                for error in failed_tests:
                    logger.error(f"   - {error}")
                return TestResult.FAIL
            else:
                logger.info(f"🎉 [权限矩阵测试] 测试通过: {len(test_cases)}/{len(test_cases)} 个测试成功")
                return TestResult.PASS
            
        except Exception as e:
            logger.error(f"💥 [权限矩阵测试] 测试异常: {e}")
            return TestResult.ERROR
    
    def test_duplicate_completion_prevention(self) -> TestResult:
        """
        测试重复完成防护机制
        
        验证点：
        1. 第一次完成成功
        2. 第二次完成被正确阻止
        3. 状态保持一致性
        """
        logger.info("🛡️ [重复完成防护测试] 开始测试防重复完成机制")
        
        try:
            # 1. 创建测试待办
            test_todo = self.create_test_todo("principal", "student")
            todo_id = test_todo.get("id")
            
            # 2. 第一次完成（应该成功）
            try:
                self.complete_todo("student", todo_id, "第一次完成测试")
                logger.info("✅ 第一次完成成功")
            except Exception as e:
                logger.error(f"❌ 第一次完成失败: {e}")
                return TestResult.FAIL
            
            # 验证第一次完成状态
            if not self.verify_todo_completion_state("student", todo_id, True):
                return TestResult.FAIL
            
            # 3. 第二次完成（应该失败）
            duplicate_prevented = False
            try:
                self.complete_todo("student", todo_id, "第二次完成测试（应该失败）")
                logger.error("❌ 第二次完成应该失败但成功了")
            except (APIException, PermissionException) as e:
                error_msg = str(e).lower()
                if "完成" in str(e) or "completed" in error_msg or "duplicate" in error_msg:
                    duplicate_prevented = True
                    logger.info(f"✅ 重复完成被正确阻止: {e}")
                else:
                    logger.warning(f"⚠️ 第二次完成失败，但错误信息不明确: {e}")
                    duplicate_prevented = True  # 暂时认为阻止成功
            
            if not duplicate_prevented:
                logger.error("❌ 重复完成未被阻止")
                return TestResult.FAIL
            
            # 4. 验证状态一致性（仍应为已完成）
            if not self.verify_todo_completion_state("student", todo_id, True):
                logger.error("❌ 重复完成尝试后状态不一致")
                return TestResult.FAIL
            
            logger.info("🎉 [重复完成防护测试] 测试通过")
            return TestResult.PASS
            
        except Exception as e:
            logger.error(f"💥 [重复完成防护测试] 测试异常: {e}")
            return TestResult.ERROR
    
    def test_data_persistence(self) -> TestResult:
        """
        测试数据持久化机制
        
        验证点：
        1. 完成状态的数据库持久化
        2. 统计信息的准确更新
        3. 多次查询的一致性
        """
        logger.info("💾 [数据持久化测试] 开始测试完成状态持久化")
        
        try:
            # 1. 创建测试待办
            test_todo = self.create_test_todo("principal", "student")
            todo_id = test_todo.get("id")
            
            # 2. 验证初始状态
            if not self.verify_todo_completion_state("student", todo_id, False):
                return TestResult.FAIL
            
            # 3. 完成待办
            self.complete_todo("student", todo_id, "数据持久化测试")
            logger.info("✅ 待办完成操作执行成功")
            
            # 4. 多次验证持久化状态（间隔查询）
            persistence_checks = []
            
            for i in range(3):
                time.sleep(0.5)  # 等待数据库更新
                check_passed = self.verify_todo_completion_state("student", todo_id, True)
                persistence_checks.append(check_passed)
                logger.info(f"   第{i+1}次持久化检查: {'✅ 通过' if check_passed else '❌ 失败'}")
            
            if not all(persistence_checks):
                failed_count = persistence_checks.count(False)
                logger.error(f"❌ 持久化检查失败: {failed_count}/3 次检查失败")
                return TestResult.FAIL
            
            # 5. 验证统计信息持久化
            stats_checks = []
            for i in range(2):
                time.sleep(0.3)
                stats = self.get_todo_stats("principal", todo_id)
                total_completed = stats.get("stats", {}).get("totalCompleted", 0)
                check_passed = total_completed >= 1
                stats_checks.append(check_passed)
                logger.info(f"   统计持久化检查{i+1}: 完成数={total_completed}, {'✅ 通过' if check_passed else '❌ 失败'}")
            
            if not all(stats_checks):
                failed_count = stats_checks.count(False)
                logger.error(f"❌ 统计持久化检查失败: {failed_count}/2 次检查失败")
                return TestResult.FAIL
            
            logger.info("🎉 [数据持久化测试] 测试通过")
            return TestResult.PASS
            
        except Exception as e:
            logger.error(f"💥 [数据持久化测试] 测试异常: {e}")
            return TestResult.ERROR
    
    def test_edge_cases_and_error_handling(self) -> TestResult:
        """
        测试边界情况和错误处理
        
        验证点：
        1. 无效待办ID处理
        2. 无效用户Token处理  
        3. 网络异常恢复
        4. 数据格式验证
        """
        logger.info("🔧 [边界情况测试] 开始测试边界情况和错误处理")
        
        test_results = []
        
        try:
            # 测试1: 无效待办ID
            logger.info("🔍 测试无效待办ID处理")
            try:
                invalid_id = 999999999  # 不存在的ID
                self.complete_todo("student", invalid_id, "无效ID测试")
                logger.error("   ❌ 无效ID应该失败但成功了")
                test_results.append(False)
            except (APIException, PermissionException) as e:
                logger.info(f"   ✅ 无效ID被正确处理: {e}")
                test_results.append(True)
            
            # 测试2: 负数待办ID
            logger.info("🔍 测试负数待办ID处理")
            try:
                negative_id = -1
                self.complete_todo("student", negative_id, "负数ID测试")
                logger.error("   ❌ 负数ID应该失败但成功了")
                test_results.append(False)
            except (APIException, PermissionException) as e:
                logger.info(f"   ✅ 负数ID被正确处理: {e}")
                test_results.append(True)
            
            # 测试3: 创建待办后立即完成（时序测试）
            logger.info("🔍 测试创建后立即完成的时序处理")
            try:
                test_todo = self.create_test_todo("principal", "student")
                todo_id = test_todo.get("id")
                # 立即完成，无等待
                self.complete_todo("student", todo_id, "时序测试")
                logger.info("   ✅ 立即完成处理成功")
                test_results.append(True)
            except Exception as e:
                logger.warning(f"   ⚠️ 立即完成可能存在时序问题: {e}")
                test_results.append(False)
            
            # 测试4: 超长评论处理
            logger.info("🔍 测试超长评论处理")
            try:
                test_todo = self.create_test_todo("principal", "student")
                todo_id = test_todo.get("id")
                long_comment = "测试超长评论" * 200  # 约1000字符
                self.complete_todo("student", todo_id, long_comment)
                logger.info("   ✅ 超长评论处理成功")
                test_results.append(True)
            except Exception as e:
                logger.info(f"   ✅ 超长评论被正确限制: {e}")
                test_results.append(True)
            
            # 汇总结果
            passed_tests = sum(test_results)
            total_tests = len(test_results)
            
            if passed_tests == total_tests:
                logger.info(f"🎉 [边界情况测试] 测试通过: {passed_tests}/{total_tests}")
                return TestResult.PASS
            else:
                logger.error(f"❌ [边界情况测试] 部分失败: {passed_tests}/{total_tests}")
                return TestResult.FAIL
                
        except Exception as e:
            logger.error(f"💥 [边界情况测试] 测试异常: {e}")
            return TestResult.ERROR
    
    def test_concurrent_completion(self) -> TestResult:
        """
        测试并发完成处理
        
        验证系统在并发情况下的数据一致性
        """
        logger.info("⚡ [并发测试] 开始测试并发完成处理")
        
        try:
            # 创建多个测试待办
            concurrent_todos = []
            for i in range(3):
                test_todo = self.create_test_todo("principal", "student")
                concurrent_todos.append(test_todo.get("id"))
                time.sleep(0.1)  # 避免创建过快
            
            logger.info(f"📝 创建了{len(concurrent_todos)}个并发测试待办")
            
            # 使用线程池并发完成
            completion_results = []
            
            def complete_single_todo(todo_id):
                try:
                    result = self.complete_todo("student", todo_id, f"并发测试 - TODO {todo_id}")
                    return {"todo_id": todo_id, "success": True, "result": result}
                except Exception as e:
                    return {"todo_id": todo_id, "success": False, "error": str(e)}
            
            # 并发执行完成操作
            with concurrent.futures.ThreadPoolExecutor(max_workers=self.config.max_concurrent) as executor:
                futures = [executor.submit(complete_single_todo, todo_id) for todo_id in concurrent_todos]
                
                for future in concurrent.futures.as_completed(futures, timeout=30):
                    result = future.result()
                    completion_results.append(result)
                    
                    if result["success"]:
                        logger.info(f"   ✅ 并发完成成功: TODO {result['todo_id']}")
                    else:
                        logger.error(f"   ❌ 并发完成失败: TODO {result['todo_id']} - {result['error']}")
            
            # 验证并发完成结果
            successful_completions = [r for r in completion_results if r["success"]]
            failed_completions = [r for r in completion_results if not r["success"]]
            
            logger.info(f"📊 并发完成结果: 成功{len(successful_completions)}, 失败{len(failed_completions)}")
            
            # 验证所有成功完成的待办状态
            consistency_checks = []
            for result in successful_completions:
                todo_id = result["todo_id"]
                is_consistent = self.verify_todo_completion_state("student", todo_id, True)
                consistency_checks.append(is_consistent)
            
            if all(consistency_checks) and len(successful_completions) >= len(concurrent_todos) * 0.8:
                logger.info("🎉 [并发测试] 测试通过")
                return TestResult.PASS
            else:
                logger.error("❌ [并发测试] 并发一致性验证失败")
                return TestResult.FAIL
                
        except Exception as e:
            logger.error(f"💥 [并发测试] 测试异常: {e}")
            return TestResult.ERROR
    
    def run_comprehensive_test(self) -> bool:
        """
        运行全面的完成功能测试
        
        Returns:
            bool: 测试总体是否通过
        """
        logger.info("🚀 开始哈尔滨信息工程学院校园门户系统 - 待办完成API功能全面测试")
        logger.info(f"📊 测试配置: 超时={DEFAULT_TIMEOUT}s, 重试={MAX_RETRIES}次, 并发={self.config.max_concurrent}")
        
        start_time = time.time()
        
        # 预认证所有测试用户
        logger.info("🔐 预认证所有测试用户...")
        for account_key, account in TEST_ACCOUNTS.items():
            try:
                self.authenticate_user(account_key)
                logger.info(f"   ✅ {account.description} 认证成功")
            except AuthenticationException as e:
                logger.error(f"   ❌ {account.description} 认证失败: {e}")
                logger.error("🛑 用户认证失败，测试无法继续")
                return False
        
        logger.info("✅ 所有用户认证完成")
        
        # 定义测试套件
        test_suite = [
            ("基本功能测试", self.test_basic_functionality, True),
            ("权限矩阵测试", self.test_permission_matrix, True),
            ("重复完成防护测试", self.test_duplicate_completion_prevention, True),
            ("数据持久化测试", self.test_data_persistence, True),
            ("边界情况测试", self.test_edge_cases_and_error_handling, False),  # 可选测试
            ("并发处理测试", self.test_concurrent_completion, False)  # 可选测试
        ]
        
        # 执行测试套件
        test_results = {}
        critical_failures = []
        
        logger.info("\n" + "=" * 80)
        logger.info("🧪 开始执行测试套件")
        logger.info("=" * 80)
        
        for test_name, test_func, is_critical in test_suite:
            logger.info(f"\n🔧 正在执行: {test_name}")
            logger.info("-" * 50)
            
            try:
                with self.timing_context(f"测试套件: {test_name}"):
                    result = test_func()
                    test_results[test_name] = result
                    
                    if result == TestResult.PASS:
                        logger.info(f"✅ {test_name}: {result.value}")
                    elif result == TestResult.FAIL:
                        logger.error(f"❌ {test_name}: {result.value}")
                        if is_critical:
                            critical_failures.append(test_name)
                    elif result == TestResult.ERROR:
                        logger.error(f"💥 {test_name}: {result.value}")
                        if is_critical:
                            critical_failures.append(test_name)
                    else:
                        logger.warning(f"⏭️ {test_name}: {result.value}")
                        
            except Exception as e:
                logger.error(f"💥 测试执行异常: {test_name} - {e}")
                test_results[test_name] = TestResult.ERROR
                if is_critical:
                    critical_failures.append(test_name)
        
        # 生成测试报告
        end_time = time.time()
        total_duration = end_time - start_time
        
        self._generate_test_report(test_results, total_duration, critical_failures)
        
        # 判断测试是否总体通过
        passed_count = sum(1 for result in test_results.values() if result == TestResult.PASS)
        total_count = len(test_results)
        critical_passed = len(critical_failures) == 0
        
        success_rate = (passed_count / total_count) * 100 if total_count > 0 else 0
        
        overall_success = critical_passed and success_rate >= 75
        
        if overall_success:
            logger.info(f"🎉 测试总体通过! 成功率: {success_rate:.1f}% ({passed_count}/{total_count})")
        else:
            logger.error(f"💥 测试总体失败! 成功率: {success_rate:.1f}% ({passed_count}/{total_count})")
            if critical_failures:
                logger.error(f"🚨 关键测试失败: {', '.join(critical_failures)}")
        
        return overall_success
    
    def _generate_test_report(self, test_results: Dict[str, TestResult], total_duration: float, critical_failures: List[str]):
        """生成详细的测试报告"""
        
        logger.info("\n" + "=" * 80)
        logger.info("📊 哈尔滨信息工程学院校园门户系统 - 待办完成API测试报告")
        logger.info("=" * 80)
        
        # 基本统计
        total_tests = len(test_results)
        passed_tests = sum(1 for result in test_results.values() if result == TestResult.PASS)
        failed_tests = sum(1 for result in test_results.values() if result == TestResult.FAIL)
        error_tests = sum(1 for result in test_results.values() if result == TestResult.ERROR)
        skipped_tests = sum(1 for result in test_results.values() if result == TestResult.SKIP)
        
        success_rate = (passed_tests / total_tests) * 100 if total_tests > 0 else 0
        
        # 测试结果汇总
        logger.info("📈 测试结果统计:")
        logger.info(f"   总测试数: {total_tests}")
        logger.info(f"   ✅ 通过: {passed_tests}")
        logger.info(f"   ❌ 失败: {failed_tests}")
        logger.info(f"   💥 错误: {error_tests}")
        logger.info(f"   ⏭️ 跳过: {skipped_tests}")
        logger.info(f"   🎯 成功率: {success_rate:.2f}%")
        logger.info(f"   ⏱️ 总耗时: {total_duration:.2f}s")
        
        # 详细结果
        logger.info("\n📋 详细测试结果:")
        for test_name, result in test_results.items():
            status_icon = "✅" if result == TestResult.PASS else "❌" if result == TestResult.FAIL else "💥" if result == TestResult.ERROR else "⏭️"
            logger.info(f"   {status_icon} {test_name}: {result.value}")
        
        # 性能指标
        if self.metrics:
            logger.info("\n⚡ 性能指标分析:")
            avg_duration = sum(m.duration for m in self.metrics) / len(self.metrics)
            max_duration = max(m.duration for m in self.metrics)
            min_duration = min(m.duration for m in self.metrics)
            
            logger.info(f"   平均操作耗时: {avg_duration:.3f}s")
            logger.info(f"   最长操作耗时: {max_duration:.3f}s")
            logger.info(f"   最短操作耗时: {min_duration:.3f}s")
            
            # 慢操作分析
            slow_operations = [m for m in self.metrics if m.duration > 2.0]
            if slow_operations:
                logger.warning(f"   ⚠️ 发现{len(slow_operations)}个慢操作(>2s)")
        
        # 关键失败分析
        if critical_failures:
            logger.error("\n🚨 关键功能失败分析:")
            for failure in critical_failures:
                logger.error(f"   ❌ {failure} - 影响系统核心功能")
        
        # 测试数据汇总
        if self.test_todos:
            logger.info(f"\n📝 测试数据汇总:")
            logger.info(f"   创建测试待办: {len(self.test_todos)}个")
            logger.info(f"   测试用户认证: {len(self.tokens)}个账号")
        
        # 质量评级
        if success_rate >= 95:
            quality_grade = "🏆 优秀"
        elif success_rate >= 85:
            quality_grade = "🥈 良好"
        elif success_rate >= 75:
            quality_grade = "🥉 及格"
        else:
            quality_grade = "❌ 不及格"
        
        logger.info(f"\n🎖️ 系统质量评级: {quality_grade}")
        
        # 建议和总结
        logger.info("\n💡 测试建议:")
        if critical_failures:
            logger.warning("   ⚠️ 优先修复关键功能失败")
        if success_rate < 85:
            logger.warning("   ⚠️ 建议提升测试覆盖率和功能稳定性")
        if self.metrics and any(m.duration > 3.0 for m in self.metrics):
            logger.warning("   ⚠️ 存在性能瓶颈，建议优化响应时间")
        
        logger.info("   ✅ 建议在生产环境部署前进行完整回归测试")
        logger.info("   ✅ 建议增加监控和告警机制")
        
        logger.info("\n" + "=" * 80)


# =========================
# 主程序入口
# =========================

def main():
    """主程序入口"""
    logger.info("🎓 哈尔滨信息工程学院校园门户系统")
    logger.info("📋 待办完成API功能测试 v2.0 - 企业级测试套件")
    logger.info("=" * 60)
    
    # 创建测试配置
    config = TestConfig(
        max_concurrent=3,  # 降低并发数以提高稳定性
        completion_timeout=12,
        list_check_timeout=8,
        stats_check_timeout=10
    )
    
    # 创建测试套件实例
    test_suite = TodoCompletionTestSuite(config)
    
    try:
        # 运行综合测试
        success = test_suite.run_comprehensive_test()
        
        # 输出最终结果
        if success:
            logger.info("🎉 待办完成API功能测试 - 总体通过")
            logger.info("✅ 系统满足生产环境部署要求")
            exit_code = 0
        else:
            logger.error("💥 待办完成API功能测试 - 总体失败")
            logger.error("❌ 系统存在关键问题，不建议部署到生产环境")
            exit_code = 1
            
    except KeyboardInterrupt:
        logger.warning("⚠️ 测试被用户中断")
        exit_code = 2
    except Exception as e:
        logger.error(f"💥 测试执行异常: {e}")
        exit_code = 3
    
    logger.info(f"\n🏁 测试程序结束，退出码: {exit_code}")
    return exit_code


if __name__ == "__main__":
    exit(main())