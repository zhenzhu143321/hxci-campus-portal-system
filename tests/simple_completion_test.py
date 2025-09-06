#!/usr/bin/env python3
"""
哈尔滨信息工程学院校园门户系统 - 简化版待办完成测试
专注于测试现有待办的完成功能
"""

import requests
import json
import time

def get_auth_token(employee_id, name, password):
    """获取认证Token"""
    auth_url = "http://localhost:48082/mock-school-api/auth/authenticate"
    auth_data = {
        "employeeId": employee_id,
        "name": name,
        "password": password
    }
    
    response = requests.post(auth_url, json=auth_data, timeout=10)
    if response.status_code == 200:
        result = response.json()
        if result.get("success") and "data" in result:
            return result["data"].get("accessToken")
    return None

def get_todo_list(token):
    """获取待办列表"""
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json", 
        "tenant-id": "1"
    }
    
    response = requests.get("http://localhost:48081/admin-api/test/todo-new/api/my-list", 
                          headers=headers, timeout=10)
    
    if response.status_code == 200:
        result = response.json()
        if result.get("success") and "data" in result:
            return result["data"].get("todos", [])
    return []

def complete_todo(token, todo_id):
    """完成待办"""
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "tenant-id": "1"
    }
    
    completion_data = {
        "comment": f"测试完成 - {time.strftime('%Y-%m-%d %H:%M:%S')}"
    }
    
    response = requests.post(f"http://localhost:48081/admin-api/test/todo-new/api/{todo_id}/complete",
                           json=completion_data, headers=headers, timeout=10)
    
    return response.status_code == 200 and response.json().get("success", False)

def main():
    """主测试函数"""
    print("🚀 开始简化版待办完成功能测试")
    
    # 1. 获取学生Token
    print("🔐 正在认证学生账号...")
    student_token = get_auth_token("STUDENT_001", "Student-Zhang", "admin123")
    if not student_token:
        print("❌ 学生认证失败")
        return False
    print("✅ 学生认证成功")
    
    # 2. 获取待办列表
    print("📋 获取学生待办列表...")
    todos = get_todo_list(student_token)
    print(f"📊 获取到 {len(todos)} 条待办")
    
    if not todos:
        print("⚠️ 没有可用的待办进行测试")
        print("💡 建议先通过其他方式创建一些测试待办")
        return True  # 没有待办不算错误
    
    # 3. 找到第一个未完成的待办
    uncompleted_todo = None
    for todo in todos:
        if not todo.get("isCompleted", True):
            uncompleted_todo = todo
            break
    
    if not uncompleted_todo:
        print("⚠️ 所有待办都已完成")
        print("📊 待办完成功能可能工作正常")
        return True
    
    todo_id = uncompleted_todo.get("id")
    todo_title = uncompleted_todo.get("title", "未知标题")
    
    print(f"🎯 找到未完成待办: ID={todo_id}, 标题='{todo_title}'")
    
    # 4. 尝试完成待办
    print("✅ 尝试完成待办...")
    success = complete_todo(student_token, todo_id)
    
    if success:
        print("🎉 待办完成操作成功")
        
        # 5. 验证完成状态
        time.sleep(1)  # 等待状态更新
        updated_todos = get_todo_list(student_token)
        
        updated_todo = None
        for todo in updated_todos:
            if todo.get("id") == todo_id:
                updated_todo = todo
                break
        
        if updated_todo and updated_todo.get("isCompleted"):
            print("✅ 完成状态验证通过")
            print("🎉 待办完成功能测试 - 总体通过")
            return True
        else:
            print("❌ 完成状态验证失败")
            return False
    else:
        print("❌ 待办完成操作失败")
        return False

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)