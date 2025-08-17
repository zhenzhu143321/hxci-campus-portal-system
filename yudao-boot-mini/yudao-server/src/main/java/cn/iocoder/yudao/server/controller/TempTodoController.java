package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.infra.integration.MockSchoolApiIntegration;
import cn.iocoder.yudao.module.infra.integration.MockSchoolApiIntegration.UserInfo;
import cn.iocoder.yudao.server.util.NotificationScopeManager;
import cn.iocoder.yudao.server.util.SafeSQLExecutor;
import cn.iocoder.yudao.server.util.SecurityEnhancementUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 📋 T13待办通知系统Controller
 * 专用于处理Level 5待办通知功能，从TempNotificationController解耦
 * 基于TempWeatherController架构设计，采用双重认证模式
 * 
 * @author Claude AI  
 * @since 2025-08-15
 */
@Tag(name = "T13待办通知系统API")
@RestController
@RequestMapping("/admin-api/test/todo")
@Validated
@TenantIgnore
@Slf4j
public class TempTodoController {

    @Autowired
    private MockSchoolApiIntegration mockSchoolApiIntegration;

    // 📊 数据库字段映射常量
    private static final Map<String, Integer> PRIORITY_MAP = Map.of(
        "low", 1,
        "medium", 2, 
        "high", 3
    );
    
    private static final Map<Integer, String> PRIORITY_REVERSE_MAP = Map.of(
        1, "low",
        2, "medium",
        3, "high"
    );
    
    private static final Map<String, Integer> STATUS_MAP = Map.of(
        "pending", 0,
        "completed", 2,
        "overdue", 3
    );
    
    private static final Map<Integer, String> STATUS_REVERSE_MAP = Map.of(
        0, "pending",
        2, "completed", 
        3, "overdue"
    );

    /**
     * 🧪 服务测试接口
     */
    @GetMapping("/api/ping")
    @Operation(summary = "待办通知服务Ping测试")
    @PermitAll
    @TenantIgnore
    public CommonResult<String> ping() {
        log.info("🏓 [TODO-PING] 待办通知服务ping测试");
        return success("pong from TempTodoController - server module");
    }

    /**
     * 📝 T13.1 获取我的待办列表 - 双重认证版本
     */
    @GetMapping("/api/my-list")
    @Operation(summary = "获取我的待办列表(双重认证)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> getMyTodoList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            HttpServletRequest httpRequest) {
        
        log.info("📝 [TODO-LIST] 获取我的待办列表 - page:{}, pageSize:{}, status:{}, priority:{}", 
                page, pageSize, status, priority);
        
        try {
            // 🔐 Step 1: 双重认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                log.warn("❌ [TODO-LIST] 未提供认证Token");
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = mockSchoolApiIntegration.getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                log.warn("❌ [TODO-LIST] Token验证失败");
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [TODO-LIST] 用户认证成功: {} (角色: {})", userInfo.getUsername(), userInfo.getRoleCode());

            // 🔍 Step 2: 构建查询条件
            StringBuilder whereClause = new StringBuilder();
            whereClause.append("WHERE level = 5 AND deleted = 0 AND status IN (1, 3)"); // 审批通过或已发布的待办
            
            // 添加状态过滤
            if (status != null && STATUS_MAP.containsKey(status)) {
                whereClause.append(" AND todo_status = ").append(STATUS_MAP.get(status));
            }
            
            // 添加优先级过滤
            if (priority != null && PRIORITY_MAP.containsKey(priority)) {
                whereClause.append(" AND todo_priority = ").append(PRIORITY_MAP.get(priority));
            }
            
            // 添加范围权限过滤（学生只能看到班级和年级相关的）
            if ("STUDENT".equals(userInfo.getRoleCode())) {
                whereClause.append(" AND (target_scope IN ('SCHOOL_WIDE', 'CLASS', 'GRADE') OR publisher_id = '")
                          .append(userInfo.getUserId()).append("')");
            }

            // 📋 Step 3: 查询待办列表数据
            String countSql = "SELECT COUNT(*) as total FROM notification_info " + whereClause;
            
            String dataSql = String.format(
                "SELECT id, title, content, todo_priority, " +
                "DATE_FORMAT(todo_deadline, '%%Y-%%m-%%d') as due_date, " +
                "todo_status, publisher_name as assigner_name, " +
                "DATE_FORMAT(create_time, '%%Y-%%m-%%d %%H:%%i:%%s') as create_time " +
                "FROM notification_info %s " +
                "ORDER BY todo_priority DESC, todo_deadline ASC " +
                "LIMIT %d OFFSET %d",
                whereClause, pageSize, (page - 1) * pageSize
            );

            log.info("🔍 [TODO-LIST] 执行查询SQL: {}", dataSql);

            // 🎯 Step 4: 执行数据库查询
            List<Map<String, Object>> todos = executeQueryAndReturnList(dataSql);
            Map<String, Object> countResult = executeQueryAndReturnSingle(countSql);
            
            int total = countResult != null ? 
                Integer.parseInt(countResult.get("total").toString()) : 0;

            // 🔄 Step 5: 检查每个待办的个人完成状态
            for (Map<String, Object> todo : todos) {
                Long todoId = Long.parseLong(todo.get("id").toString());
                boolean isCompleted = checkUserTodoCompletion(todoId, userInfo.getUserId());
                
                // 📊 构建前端所需的数据格式
                todo.put("level", 5); // 固定Level 5
                todo.put("priority", PRIORITY_REVERSE_MAP.get(
                    Integer.parseInt(todo.get("todo_priority").toString())));
                todo.put("dueDate", todo.get("due_date"));
                todo.put("status", isCompleted ? "completed" : 
                    STATUS_REVERSE_MAP.get(Integer.parseInt(todo.get("todo_status").toString())));
                todo.put("assignerName", todo.get("assigner_name"));
                todo.put("isCompleted", isCompleted);
                
                // 移除数据库字段
                todo.remove("todo_priority");
                todo.remove("due_date");
                todo.remove("todo_status");
                todo.remove("assigner_name");
            }

            // ✅ Step 6: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("todos", todos);
            result.put("pagination", Map.of(
                "current", page,
                "pageSize", pageSize,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / pageSize)
            ));
            result.put("user", Map.of(
                "username", userInfo.getUsername(),
                "roleCode", userInfo.getRoleCode(),
                "roleName", userInfo.getRoleName()
            ));
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [TODO-LIST] 成功返回{}条待办数据 (用户: {})", todos.size(), userInfo.getUsername());
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [TODO-LIST] 获取待办列表异常", e);
            return CommonResult.error(500, "获取待办列表异常: " + e.getMessage());
        }
    }

    /**
     * ✅ T13.2 标记待办完成 - 双重认证版本
     */
    @PostMapping("/api/{id}/complete")
    @Operation(summary = "标记待办完成(双重认证)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> completeTodo(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        log.info("✅ [TODO-COMPLETE] 标记待办完成 - todoId: {}", id);
        
        try {
            // 🔐 Step 1: 双重认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = mockSchoolApiIntegration.getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [TODO-COMPLETE] 用户认证成功: {} (角色: {})", userInfo.getUsername(), userInfo.getRoleCode());

            // 🔍 Step 2: 检查待办是否存在且有效
            String checkSql = "SELECT id, title, todo_status FROM notification_info " +
                             "WHERE id = " + id + " AND level = 5 AND deleted = 0";
            
            Map<String, Object> todoInfo = executeQueryAndReturnSingle(checkSql);
            if (todoInfo == null) {
                log.warn("❌ [TODO-COMPLETE] 待办不存在或无效: {}", id);
                return CommonResult.error(404, "待办任务不存在");
            }

            // 🔄 Step 3: 检查是否已经完成
            boolean alreadyCompleted = checkUserTodoCompletion(id, userInfo.getUserId());
            if (alreadyCompleted) {
                log.warn("⚠️ [TODO-COMPLETE] 待办已完成: {} (用户: {})", id, userInfo.getUsername());
                return CommonResult.error(409, "该待办任务已完成");
            }

            // ✅ Step 4: 插入完成记录
            String insertSql = String.format(
                "INSERT INTO notification_todo_completion " +
                "(notification_id, user_id, user_name, user_role, completed_time, tenant_id) " +
                "VALUES (%d, '%s', '%s', '%s', NOW(), 1)",
                id, 
                SecurityEnhancementUtil.escapeSQL(userInfo.getUserId()),
                SecurityEnhancementUtil.escapeSQL(userInfo.getUsername()),
                SecurityEnhancementUtil.escapeSQL(userInfo.getRoleCode())
            );

            boolean insertSuccess = executeSQLUpdate(insertSql);
            if (!insertSuccess) {
                log.error("❌ [TODO-COMPLETE] 插入完成记录失败");
                return CommonResult.error(500, "标记完成失败");
            }

            // 📊 Step 5: 更新统计信息（可选）
            String updateStatsSql = String.format(
                "UPDATE notification_info SET confirm_count = confirm_count + 1 WHERE id = %d", id
            );
            executeSQLUpdate(updateStatsSql);

            // ✅ Step 6: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("todoId", id);
            result.put("title", todoInfo.get("title"));
            result.put("completedBy", userInfo.getUsername());
            result.put("completedTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("isCompleted", true);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [TODO-COMPLETE] 待办标记完成成功 - todoId: {}, user: {}", id, userInfo.getUsername());
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [TODO-COMPLETE] 标记待办完成异常", e);
            return CommonResult.error(500, "标记完成异常: " + e.getMessage());
        }
    }

    /**
     * 📝 T13.3 发布待办通知 - 双重认证版本
     */
    @PostMapping("/api/publish")
    @Operation(summary = "发布待办通知(双重认证)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> publishTodoNotification(
            @Valid @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        log.info("📝 [TODO-PUBLISH] 发布待办通知请求: {}", request);
        
        try {
            // 🔐 Step 1: 双重认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = mockSchoolApiIntegration.getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [TODO-PUBLISH] 用户认证成功: {} (角色: {})", userInfo.getUsername(), userInfo.getRoleCode());

            // 🔍 Step 2: 验证请求参数
            List<String> validationErrors = SecurityEnhancementUtil.validateTodoPublishRequest(request);
            if (!validationErrors.isEmpty()) {
                log.warn("❌ [TODO-PUBLISH] 参数验证失败: {}", validationErrors);
                return CommonResult.error(400, "参数验证失败: " + String.join(", ", validationErrors));
            }

            // 🎯 Step 3: 权限验证 - 待办通知发布权限
            String targetScope = (String) request.getOrDefault("targetScope", "CLASS");
            boolean hasPermission = validateTodoPublishPermission(userInfo.getRoleCode(), targetScope);
            if (!hasPermission) {
                log.warn("❌ [TODO-PUBLISH] 用户{}无权限发布{}范围的待办通知", 
                        userInfo.getUsername(), targetScope);
                return CommonResult.error(403, "无权限发布该范围的待办通知");
            }

            // 📋 Step 4: 构建待办通知数据
            Map<String, Object> notificationData = buildTodoNotificationData(request, userInfo);
            
            // 🗄️ Step 5: 插入数据库
            String insertSql = buildTodoInsertSQL(notificationData);
            log.info("🗄️ [TODO-PUBLISH] 执行插入SQL: {}", insertSql);
            
            boolean insertSuccess = executeSQLUpdate(insertSql);
            if (!insertSuccess) {
                log.error("❌ [TODO-PUBLISH] 数据库插入失败");
                return CommonResult.error(500, "发布待办通知失败");
            }

            // 🔍 Step 6: 获取插入的记录ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Map<String, Object> idResult = executeQueryAndReturnSingle(lastIdSql);
            Long notificationId = idResult != null ? 
                Long.parseLong(idResult.get("id").toString()) : null;

            // ✅ Step 7: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("id", notificationId);
            result.put("title", request.get("title"));
            result.put("level", 5);
            result.put("priority", request.get("priority"));
            result.put("dueDate", request.get("dueDate"));
            result.put("status", "pending");
            result.put("assignerName", userInfo.getUsername());
            result.put("targetScope", targetScope);
            result.put("publishedBy", userInfo.getUsername());
            result.put("publishedTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [TODO-PUBLISH] 待办通知发布成功 - id: {}, title: {}", notificationId, request.get("title"));
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [TODO-PUBLISH] 发布待办通知异常", e);
            return CommonResult.error(500, "发布待办通知异常: " + e.getMessage());
        }
    }

    /**
     * 📊 T13.4 获取待办统计 - 双重认证版本
     */
    @GetMapping("/api/{id}/stats")
    @Operation(summary = "获取待办统计(双重认证)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> getTodoStats(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        log.info("📊 [TODO-STATS] 获取待办统计 - todoId: {}", id);
        
        try {
            // 🔐 Step 1: 双重认证验证
            String authToken = httpRequest.getHeader("Authorization");
            if (authToken == null) {
                return CommonResult.error(401, "未提供认证Token");
            }

            UserInfo userInfo = mockSchoolApiIntegration.getUserInfoFromMockApi(authToken);
            if (userInfo == null) {
                return CommonResult.error(401, "Token验证失败");
            }

            log.info("✅ [TODO-STATS] 用户认证成功: {} (角色: {})", userInfo.getUsername(), userInfo.getRoleCode());

            // 🔍 Step 2: 检查待办是否存在
            String checkSql = "SELECT id, title, publisher_name, target_scope, " +
                             "DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') as create_time, " +
                             "DATE_FORMAT(todo_deadline, '%Y-%m-%d') as due_date " +
                             "FROM notification_info " +
                             "WHERE id = " + id + " AND level = 5 AND deleted = 0";
            
            Map<String, Object> todoInfo = executeQueryAndReturnSingle(checkSql);
            if (todoInfo == null) {
                log.warn("❌ [TODO-STATS] 待办不存在: {}", id);
                return CommonResult.error(404, "待办任务不存在");
            }

            // 📊 Step 3: 统计完成情况
            String statsSql = String.format(
                "SELECT " +
                "COUNT(*) as total_completed, " +
                "COUNT(CASE WHEN user_role = 'STUDENT' THEN 1 END) as student_completed, " +
                "COUNT(CASE WHEN user_role = 'TEACHER' THEN 1 END) as teacher_completed, " +
                "COUNT(CASE WHEN user_role = 'CLASS_TEACHER' THEN 1 END) as class_teacher_completed " +
                "FROM notification_todo_completion " +
                "WHERE notification_id = %d", id
            );

            Map<String, Object> statsData = executeQueryAndReturnSingle(statsSql);

            // 🔍 Step 4: 获取最近完成记录
            String recentSql = String.format(
                "SELECT user_name, user_role, " +
                "DATE_FORMAT(completed_time, '%%Y-%%m-%%d %%H:%%i:%%s') as completed_time " +
                "FROM notification_todo_completion " +
                "WHERE notification_id = %d " +
                "ORDER BY completed_time DESC LIMIT 10", id
            );

            List<Map<String, Object>> recentCompletions = executeQueryAndReturnList(recentSql);

            // ✅ Step 5: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("todoInfo", todoInfo);
            result.put("stats", Map.of(
                "totalCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("total_completed").toString()) : 0,
                "studentCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("student_completed").toString()) : 0,
                "teacherCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("teacher_completed").toString()) : 0,
                "classTeacherCompleted", statsData != null ? 
                    Integer.parseInt(statsData.get("class_teacher_completed").toString()) : 0
            ));
            result.put("recentCompletions", recentCompletions);
            result.put("requestedBy", userInfo.getUsername());
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [TODO-STATS] 成功返回待办统计 - todoId: {}, totalCompleted: {}", 
                    id, statsData != null ? statsData.get("total_completed") : 0);
            return success(result);
            
        } catch (Exception e) {
            log.error("❌ [TODO-STATS] 获取待办统计异常", e);
            return CommonResult.error(500, "获取待办统计异常: " + e.getMessage());
        }
    }

    // ========================= 私有辅助方法 =========================

    /**
     * 检查用户待办完成状态
     */
    private boolean checkUserTodoCompletion(Long todoId, String userId) {
        try {
            String checkSql = String.format(
                "SELECT id FROM notification_todo_completion " +
                "WHERE notification_id = %d AND user_id = '%s'",
                todoId, SecurityEnhancementUtil.escapeSQL(userId)
            );
            
            Map<String, Object> result = executeQueryAndReturnSingle(checkSql);
            return result != null;
            
        } catch (Exception e) {
            log.error("❌ [TODO-COMPLETION-CHECK] 检查完成状态异常", e);
            return false;
        }
    }

    /**
     * 验证待办发布权限
     */
    private boolean validateTodoPublishPermission(String roleCode, String targetScope) {
        // 待办通知发布权限矩阵
        Map<String, Set<String>> rolePermissions = Map.of(
            "SYSTEM_ADMIN", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "PRINCIPAL", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "ACADEMIC_ADMIN", Set.of("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS"),
            "TEACHER", Set.of("DEPARTMENT", "CLASS"),
            "CLASS_TEACHER", Set.of("GRADE", "CLASS"),
            "STUDENT", Set.of("CLASS")
        );
        
        Set<String> allowedScopes = rolePermissions.get(roleCode);
        return allowedScopes != null && allowedScopes.contains(targetScope);
    }

    /**
     * 构建待办通知数据
     */
    private Map<String, Object> buildTodoNotificationData(Map<String, Object> request, UserInfo userInfo) {
        Map<String, Object> data = new HashMap<>();
        
        // 基本信息
        data.put("title", SecurityEnhancementUtil.escapeHTML((String) request.get("title")));
        data.put("content", SecurityEnhancementUtil.escapeHTML((String) request.get("content")));
        String content = (String) request.get("content");
        data.put("summary", content != null && content.length() > 100 ? 
                 content.substring(0, 100) + "..." : content);
        data.put("level", 5); // 固定Level 5
        data.put("status", 3); // 直接发布状态
        data.put("category_id", 1);
        
        // 发布者信息 - 修复publisher_id类型转换
        // publisher_id字段是bigint类型，需要转换用户ID字符串为数字
        String userId = userInfo.getUserId();
        Long publisherId = 999L; // 默认发布者ID
        if (userId != null && userId.contains("_")) {
            // 从用户ID中提取数字部分，如 PRINCIPAL_001 -> 1
            try {
                String numPart = userId.substring(userId.lastIndexOf("_") + 1);
                publisherId = Long.parseLong(numPart);
            } catch (Exception e) {
                log.warn("⚠️ [TODO-BUILD] 无法解析用户ID数字部分，使用默认: {}", userId);
            }
        }
        data.put("publisher_id", publisherId);
        data.put("publisher_name", userInfo.getUsername());
        data.put("publisher_role", userInfo.getRoleCode());
        
        // 目标范围
        data.put("target_scope", request.getOrDefault("targetScope", "CLASS"));
        
        // 待办特有字段
        String priority = (String) request.get("priority");
        data.put("todo_priority", PRIORITY_MAP.getOrDefault(priority, 2)); // 默认medium=2
        data.put("todo_deadline", request.get("dueDate") + " 23:59:59"); // 截止时间
        data.put("todo_status", 0); // 初始状态pending=0
        
        // 系统字段
        data.put("push_channels", "1,5"); // 系统通知+待办提醒
        data.put("require_confirm", 1); // 需要确认
        data.put("pinned", 0);
        data.put("tenant_id", 1);
        data.put("creator", userInfo.getUsername());
        data.put("updater", userInfo.getUsername());
        
        // 调试日志
        log.info("🔧 [TODO-BUILD] 构建待办数据完成 - title: {}, priority: {}, dueDate: {}", 
                data.get("title"), data.get("todo_priority"), data.get("todo_deadline"));
        
        return data;
    }

    /**
     * 构建待办插入SQL
     */
    private String buildTodoInsertSQL(Map<String, Object> data) {
        return String.format(
            "INSERT INTO notification_info " +
            "(tenant_id, title, content, summary, level, status, category_id, " +
            "publisher_id, publisher_name, publisher_role, target_scope, " +
            "todo_priority, todo_deadline, todo_status, " +
            "push_channels, require_confirm, pinned, creator, updater, create_time, update_time) " +
            "VALUES " +
            "(1, '%s', '%s', '%s', %d, %d, %d, " +
            "%d, '%s', '%s', '%s', " +
            "%d, '%s', %d, " +
            "'%s', %d, %d, '%s', '%s', NOW(), NOW())",
            
            // 基本字段 - 确保字符串安全转义
            SecurityEnhancementUtil.escapeSQL((String) data.get("title")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("content")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("summary")),
            
            // 数值字段
            (Integer) data.get("level"), 
            (Integer) data.get("status"), 
            (Integer) data.get("category_id"),
            
            // 发布者信息 - 修复类型转换
            (Long) data.get("publisher_id"), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("publisher_name")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("publisher_role")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("target_scope")),
            
            // 待办特有字段
            (Integer) data.get("todo_priority"), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("todo_deadline")), 
            (Integer) data.get("todo_status"),
            
            // 系统字段
            SecurityEnhancementUtil.escapeSQL((String) data.get("push_channels")), 
            (Integer) data.get("require_confirm"), 
            (Integer) data.get("pinned"),
            SecurityEnhancementUtil.escapeSQL((String) data.get("creator")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("updater"))
        );
    }

    /**
     * 执行SQL查询并返回单个结果
     */
    private Map<String, Object> executeQueryAndReturnSingle(String sql) {
        try {
            // 🔧 FIX: 使用数组形式避免shell转义问题
            String[] command = {"mysql", "-u", "root", "ruoyi-vue-pro", "--default-character-set=utf8", "-e", sql};
            Process process = Runtime.getRuntime().exec(command);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
            
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && lines.size() > 1) {
                String[] headers = lines.get(0).split("\t");
                String[] values = lines.get(1).split("\t", -1);
                
                Map<String, Object> result = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    result.put(headers[i], "NULL".equals(values[i]) ? null : values[i]);
                }
                return result;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("❌ [DB-QUERY-SINGLE] 数据库查询异常", e);
            return null;
        }
    }

    /**
     * 执行SQL查询并返回列表结果
     */
    private List<Map<String, Object>> executeQueryAndReturnList(String sql) {
        try {
            // 🔧 FIX: 使用数组形式避免shell转义问题
            String[] command = {"mysql", "-u", "root", "ruoyi-vue-pro", "--default-character-set=utf8", "-e", sql};
            Process process = Runtime.getRuntime().exec(command);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
            
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            int exitCode = process.waitFor();
            
            List<Map<String, Object>> results = new ArrayList<>();
            if (exitCode == 0 && lines.size() > 1) {
                String[] headers = lines.get(0).split("\t");
                
                for (int i = 1; i < lines.size(); i++) {
                    String[] values = lines.get(i).split("\t", -1);
                    Map<String, Object> row = new HashMap<>();
                    
                    for (int j = 0; j < headers.length && j < values.length; j++) {
                        row.put(headers[j], "NULL".equals(values[j]) ? null : values[j]);
                    }
                    results.add(row);
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("❌ [DB-QUERY-LIST] 数据库查询异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 执行SQL更新操作
     */
    private boolean executeSQLUpdate(String sql) {
        try {
            // 🔧 FIX: 使用数组形式避免shell转义问题
            String[] command = {"mysql", "-u", "root", "ruoyi-vue-pro", "--default-character-set=utf8", "-e", sql};
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.debug("✅ [DB-UPDATE] SQL执行成功");
                return true;
            } else {
                log.error("❌ [DB-UPDATE] SQL执行失败，退出码: {}", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [DB-UPDATE] SQL执行异常", e);
            return false;
        }
    }
}