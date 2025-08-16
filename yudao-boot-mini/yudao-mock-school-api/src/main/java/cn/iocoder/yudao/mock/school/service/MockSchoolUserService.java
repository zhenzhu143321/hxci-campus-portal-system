package cn.iocoder.yudao.mock.school.service;

import cn.iocoder.yudao.mock.school.dto.UserInfo;
import cn.iocoder.yudao.mock.school.entity.MockSchoolUser;

import java.util.List;

/**
 * Mock School User Service 接口 - 精简版
 * 专注身份认证：提供学号/工号认证和JWT token生成
 * 权限验证职责已转移到主通知服务
 * 
 * @author Claude
 */
public interface MockSchoolUserService {

    /**
     * 用户认证（用户名密码登录）- 向后兼容
     */
    UserInfo authenticateUser(String username, String password);

    /**
     * 用户认证（工号+姓名+密码登录）- 新增支持
     */
    UserInfo authenticateUserByEmployeeId(String employeeId, String name, String password);

    /**
     * 🆕 生成JWT Token（包含学号/工号信息）
     * 用于主通知服务的权限验证
     */
    String generateJwtToken(UserInfo userInfo);

    /**
     * 🆕 解析JWT Token获取学号/工号
     * 用于主通知服务验证token并提取用户信息
     */
    UserInfo parseJwtToken(String jwtToken);

    /**
     * 根据token验证用户并返回用户信息
     */
    UserInfo verifyToken(String token);

    /**
     * 根据用户ID获取用户信息
     */
    UserInfo getUserInfo(String userId);

    /**
     * 根据角色编码获取用户列表
     */
    List<UserInfo> getUsersByRole(String roleCode);

    /**
     * 根据部门ID获取用户列表
     */
    List<UserInfo> getUsersByDepartment(Long departmentId);

    // 🚫 [REFACTORED] 权限验证相关方法已移除 - 职责转移到主通知服务
    // 原权限验证、权限查询等方法已删除，现在专注于身份认证

    /**
     * 创建测试用户（开发测试用）
     */
    MockSchoolUser createTestUser(String username, String userId, String roleCode, String roleName, 
                                  Long departmentId, String departmentName);

    /**
     * 刷新用户token
     */
    String refreshUserToken(String userId);

    /**
     * 获取所有角色列表
     */
    List<String> getAllRoles();

    /**
     * 获取角色用户统计
     */
    long countUsersByRole(String roleCode);
}