package cn.iocoder.yudao.mock.school.controller;

import cn.iocoder.yudao.mock.school.dto.*;
import cn.iocoder.yudao.mock.school.service.MockSchoolUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock School 认证控制器
 * 提供用户token验证和权限查询接口
 * 
 * @author Claude
 */
@RestController
@RequestMapping("/mock-school-api/auth")
@Validated
public class MockAuthController {

    private static final Logger log = LoggerFactory.getLogger(MockAuthController.class);

    @Autowired
    private MockSchoolUserService userService;

    /**
     * 用户认证接口（支持用户名密码登录和工号+姓名+密码登录）
     * POST /mock-school-api/auth/authenticate
     */
    @PostMapping("/authenticate")
    public MockApiResponse<UserInfo> authenticate(@RequestBody AuthenticateRequest request) {
        log.info("收到用户认证请求: employeeId={}, name={}, username={}", 
                request.getEmployeeId(), request.getName(), request.getUsername());
        
        try {
            // 手动验证密码
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return MockApiResponse.badRequest("密码不能为空");
            }
            
            UserInfo userInfo = null;
            
            // 优先使用工号+姓名+密码登录方式（新方式）
            if (request.getEmployeeId() != null && !request.getEmployeeId().trim().isEmpty() &&
                request.getName() != null && !request.getName().trim().isEmpty()) {
                
                log.info("使用工号+姓名+密码认证方式: employeeId={}, name={}", 
                        request.getEmployeeId(), request.getName());
                userInfo = userService.authenticateUserByEmployeeId(
                        request.getEmployeeId(), request.getName(), request.getPassword());
            } 
            // 向后兼容：使用用户名密码登录方式（旧方式）
            else if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                
                log.info("使用用户名+密码认证方式: username={}", request.getUsername());
                userInfo = userService.authenticateUser(request.getUsername(), request.getPassword());
            }
            else {
                log.warn("认证参数不完整：缺少必要的登录信息");
                return MockApiResponse.badRequest("请提供工号+姓名+密码或用户名+密码进行登录");
            }
            
            if (userInfo == null) {
                return MockApiResponse.unauthorized("用户名或密码错误");
            }
            
            log.info("用户认证成功: 用户={}, 角色={}", userInfo.getUsername(), userInfo.getRoleName());
            return MockApiResponse.success(userInfo, "用户认证成功");
            
        } catch (Exception e) {
            log.error("用户认证异常", e);
            return MockApiResponse.serverError("用户认证服务异常: " + e.getMessage());
        }
    }

    /**
     * Token验证接口
     * POST /mock-school-api/auth/verify
     */
    @PostMapping("/verify")
    public MockApiResponse<UserInfo> verifyToken(@Valid @RequestBody TokenVerifyRequest request) {
        log.info("收到token验证请求: {}", request.getToken());
        
        try {
            UserInfo userInfo = userService.verifyToken(request.getToken());
            
            if (userInfo == null) {
                return MockApiResponse.unauthorized("Token无效或已过期");
            }
            
            log.info("Token验证成功: 用户={}, 角色={}", userInfo.getUsername(), userInfo.getRoleName());
            return MockApiResponse.success(userInfo, "Token验证成功");
            
        } catch (Exception e) {
            log.error("Token验证异常", e);
            return MockApiResponse.serverError("Token验证服务异常: " + e.getMessage());
        }
    }

    // 🚫 [REFACTORED] 权限相关接口已移除 - 职责转移到主通知服务
    // 原权限查询和验证接口已删除，现在由主服务统一处理权限逻辑

    /**
     * 刷新用户Token
     * POST /mock-school-api/auth/refresh/{userId}
     */
    @PostMapping("/refresh/{userId}")
    public MockApiResponse<String> refreshToken(@PathVariable String userId) {
        log.info("刷新用户Token: {}", userId);
        
        try {
            String newToken = userService.refreshUserToken(userId);
            
            if (newToken == null) {
                return MockApiResponse.badRequest("用户不存在");
            }
            
            return MockApiResponse.success(newToken, "Token刷新成功");
            
        } catch (Exception e) {
            log.error("Token刷新异常", e);
            return MockApiResponse.serverError("Token刷新异常: " + e.getMessage());
        }
    }

    // 🚫 [REFACTORED] 移除权限验证接口 - 职责转移到主通知服务
    // 原 verify-permission 接口已删除，权限验证现在由主服务负责
    
    /**
     * 🆕 获取用户基础信息（用于主服务权限查询）
     * POST /mock-school-api/auth/user-info
     */
    @PostMapping("/user-info")
    public MockApiResponse<Map<String, Object>> getUserInfo(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("👤 [USER_INFO] 收到用户信息查询请求");
        
        try {
            // 提取Bearer Token
            String token = authHeader;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            // 通过token验证用户身份
            UserInfo userInfo = userService.verifyToken(token);
            if (userInfo == null) {
                return MockApiResponse.unauthorized("Token无效或已过期");
            }
            
            // 返回用户基础信息（不含权限验证逻辑）
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("employeeId", userInfo.getEmployeeId());
            responseData.put("username", userInfo.getUsername());
            responseData.put("realName", userInfo.getRealName());
            responseData.put("roleCode", userInfo.getRoleCode());
            responseData.put("roleName", userInfo.getRoleName());
            responseData.put("userType", userInfo.getUserType());
            responseData.put("departmentId", userInfo.getDepartmentId());
            responseData.put("departmentName", userInfo.getDepartmentName());
            
            log.info("✅ [USER_INFO] 用户信息查询成功: user={}, role={}", 
                    userInfo.getEmployeeId(), userInfo.getRoleCode());
            
            return MockApiResponse.success(responseData, "用户信息查询成功");
            
        } catch (Exception e) {
            log.error("❌ [USER_INFO] 用户信息查询异常", e);
            return MockApiResponse.serverError("用户信息查询异常: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     * GET /mock-school-api/auth/health
     */
    @GetMapping("/health")
    public MockApiResponse<String> health() {
        return MockApiResponse.success("OK", "Mock School API认证服务正常运行");
    }
}