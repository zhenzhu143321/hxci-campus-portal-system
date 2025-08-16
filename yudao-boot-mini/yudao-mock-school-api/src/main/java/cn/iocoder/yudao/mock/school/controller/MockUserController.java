package cn.iocoder.yudao.mock.school.controller;

import cn.iocoder.yudao.mock.school.dto.MockApiResponse;
import cn.iocoder.yudao.mock.school.dto.UserInfo;
import cn.iocoder.yudao.mock.school.service.MockSchoolUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mock School 用户管理控制器
 * 提供用户信息查询接口
 * 
 * @author Claude
 */
@RestController
@RequestMapping("/mock-school-api/users")
public class MockUserController {

    private static final Logger log = LoggerFactory.getLogger(MockUserController.class);

    @Autowired
    private MockSchoolUserService userService;

    /**
     * 根据用户ID获取用户信息
     * GET /mock-school-api/users/{userId}
     */
    @GetMapping("/{userId}")
    public MockApiResponse<UserInfo> getUserById(@PathVariable String userId) {
        log.info("获取用户信息: {}", userId);
        
        try {
            UserInfo userInfo = userService.getUserInfo(userId);
            
            if (userInfo == null) {
                return MockApiResponse.badRequest("用户不存在");
            }
            
            return MockApiResponse.success(userInfo, "获取用户信息成功");
            
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            return MockApiResponse.serverError("获取用户信息异常: " + e.getMessage());
        }
    }

    /**
     * 根据角色获取用户列表
     * GET /mock-school-api/users/by-role/{roleCode}
     */
    @GetMapping("/by-role/{roleCode}")
    public MockApiResponse<List<UserInfo>> getUsersByRole(@PathVariable String roleCode) {
        log.info("根据角色获取用户列表: {}", roleCode);
        
        try {
            List<UserInfo> users = userService.getUsersByRole(roleCode);
            return MockApiResponse.success(users, "获取角色用户列表成功");
            
        } catch (Exception e) {
            log.error("获取角色用户列表异常", e);
            return MockApiResponse.serverError("获取角色用户列表异常: " + e.getMessage());
        }
    }

    /**
     * 根据部门获取用户列表
     * GET /mock-school-api/users/by-department/{departmentId}
     */
    @GetMapping("/by-department/{departmentId}")
    public MockApiResponse<List<UserInfo>> getUsersByDepartment(@PathVariable Long departmentId) {
        log.info("根据部门获取用户列表: {}", departmentId);
        
        try {
            List<UserInfo> users = userService.getUsersByDepartment(departmentId);
            return MockApiResponse.success(users, "获取部门用户列表成功");
            
        } catch (Exception e) {
            log.error("获取部门用户列表异常", e);
            return MockApiResponse.serverError("获取部门用户列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取所有角色列表
     * GET /mock-school-api/users/roles
     */
    @GetMapping("/roles")
    public MockApiResponse<List<String>> getAllRoles() {
        log.info("获取所有角色列表");
        
        try {
            List<String> roles = userService.getAllRoles();
            return MockApiResponse.success(roles, "获取角色列表成功");
            
        } catch (Exception e) {
            log.error("获取角色列表异常", e);
            return MockApiResponse.serverError("获取角色列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取角色用户统计
     * GET /mock-school-api/users/count-by-role/{roleCode}
     */
    @GetMapping("/count-by-role/{roleCode}")
    public MockApiResponse<Long> countUsersByRole(@PathVariable String roleCode) {
        log.info("统计角色用户数量: {}", roleCode);
        
        try {
            long count = userService.countUsersByRole(roleCode);
            return MockApiResponse.success(count, "获取角色用户统计成功");
            
        } catch (Exception e) {
            log.error("获取角色用户统计异常", e);
            return MockApiResponse.serverError("获取角色用户统计异常: " + e.getMessage());
        }
    }
}