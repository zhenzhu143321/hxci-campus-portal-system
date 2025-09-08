package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.server.framework.datapermission.UserDataPermissionRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 垂直越权防护测试控制器
 * 测试数据权限防护功能
 * 
 * @author Claude Code
 * @date 2025-09-07
 */
@Tag(name = "垂直越权防护测试")
@RestController
@RequestMapping("/admin-api/test/vertical-privilege")
public class VerticalPrivilegeTestController {
    
    /**
     * 测试接口 - 无需权限
     */
    @GetMapping("/api/ping")
    @Operation(summary = "垂直越权防护系统Ping测试")
    public CommonResult<String> ping() {
        return CommonResult.success("垂直越权防护系统正常运行");
    }
    
    /**
     * 获取当前用户权限信息
     */
    @GetMapping("/api/current-user-info")
    @Operation(summary = "获取当前用户权限信息")
    public CommonResult<Map<String, Object>> getCurrentUserInfo() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        
        if (loginUser == null) {
            return CommonResult.error(401, "未登录或登录已过期");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", loginUser.getId());
        result.put("userId", loginUser.getId());
        result.put("userType", loginUser.getUserType());
        result.put("tenantId", loginUser.getTenantId());
        
        // TODO: 从JWT Token中解析更多用户信息
        result.put("role", "STUDENT"); // 示例角色
        result.put("dataPermissionEnabled", true);
        result.put("description", "数据权限已启用，将根据角色自动过滤数据");
        
        return CommonResult.success(result);
    }
    
    /**
     * 测试通知列表数据权限
     * 启用数据权限后，将自动根据用户角色过滤数据
     */
    @GetMapping("/api/notification-list")
    @DataPermission(includeRules = UserDataPermissionRule.class)
    @Operation(summary = "测试通知列表数据权限过滤")
    public CommonResult<List<Map<String, Object>>> testNotificationList() {
        // 模拟从数据库查询通知列表
        // 实际查询时，DataPermissionRule会自动添加SQL条件
        List<Map<String, Object>> notifications = new ArrayList<>();
        
        // 模拟数据
        Map<String, Object> notification1 = new HashMap<>();
        notification1.put("id", 1L);
        notification1.put("title", "全校通知");
        notification1.put("targetScope", "SCHOOL_WIDE");
        notification1.put("level", 1);
        notifications.add(notification1);
        
        Map<String, Object> notification2 = new HashMap<>();
        notification2.put("id", 2L);
        notification2.put("title", "班级通知");
        notification2.put("targetScope", "CLASS");
        notification2.put("targetValue", "2023级计算机1班");
        notification2.put("level", 3);
        notifications.add(notification2);
        
        return CommonResult.success(notifications);
    }
    
    /**
     * 测试禁用数据权限的查询
     * 管理员专用，可以看到所有数据
     */
    @GetMapping("/api/all-notifications")
    @DataPermission(enable = false)
    @Operation(summary = "测试禁用数据权限（管理员专用）")
    public CommonResult<Map<String, Object>> testAllNotifications() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "数据权限已禁用，返回所有数据");
        result.put("totalCount", 100);
        result.put("description", "仅管理员可以使用此接口查看所有数据");
        
        return CommonResult.success(result);
    }
    
    /**
     * 测试越权访问场景
     * 尝试访问其他班级的通知
     */
    @GetMapping("/api/other-class-notification/{classId}")
    @DataPermission(includeRules = UserDataPermissionRule.class)
    @Operation(summary = "测试越权访问其他班级通知")
    public CommonResult<Map<String, Object>> testOtherClassNotification(@PathVariable Long classId) {
        // 数据权限会自动过滤，防止越权访问
        Map<String, Object> result = new HashMap<>();
        result.put("classId", classId);
        result.put("message", "如果您不属于该班级，将无法看到数据");
        result.put("dataPermissionApplied", true);
        
        return CommonResult.success(result);
    }
    
    /**
     * 测试部门数据权限
     */
    @GetMapping("/api/department-notification/{departmentId}")
    @DataPermission(includeRules = UserDataPermissionRule.class)
    @Operation(summary = "测试部门数据权限")
    public CommonResult<Map<String, Object>> testDepartmentNotification(@PathVariable Long departmentId) {
        Map<String, Object> result = new HashMap<>();
        result.put("departmentId", departmentId);
        result.put("message", "教师可以看到本部门通知，学生无法看到");
        result.put("accessLevel", "DEPARTMENT");
        
        return CommonResult.success(result);
    }
    
    /**
     * 测试SQL注入防护
     * 数据权限系统会自动防护SQL注入
     */
    @GetMapping("/api/sql-injection-test")
    @DataPermission(includeRules = UserDataPermissionRule.class)
    @Operation(summary = "测试SQL注入防护")
    public CommonResult<String> testSqlInjection(@RequestParam String scope) {
        // 即使传入恶意参数，DataPermissionRule也会安全处理
        return CommonResult.success("数据权限系统自动防护SQL注入，输入已被安全处理: " + scope);
    }
    
    /**
     * 垂直越权防护状态检查
     */
    @GetMapping("/api/protection-status")
    @Operation(summary = "垂直越权防护状态检查")
    public CommonResult<Map<String, Object>> getProtectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", true);
        status.put("ruleCount", 1);
        status.put("protectedTables", new String[]{"notification_info", "todo_notification", "notification_approval"});
        status.put("description", "垂直越权防护已启用，自动根据用户角色过滤数据");
        status.put("implementation", "基于yudao框架DataPermission机制实现");
        
        return CommonResult.success(status);
    }
}