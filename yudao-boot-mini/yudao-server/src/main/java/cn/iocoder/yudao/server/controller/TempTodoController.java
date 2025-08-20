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

            // 🔍 Step 2: 构建查询条件 - 使用独立的todo_notifications表
            StringBuilder whereClause = new StringBuilder();
            whereClause.append("WHERE deleted = 0"); // 独立表简化条件
            
            // 添加状态过滤
            if (status != null && STATUS_MAP.containsKey(status)) {
                whereClause.append(" AND status = ").append(STATUS_MAP.get(status));
            }
            
            // 添加优先级过滤
            if (priority != null && PRIORITY_MAP.containsKey(priority)) {
                whereClause.append(" AND priority = ").append(PRIORITY_MAP.get(priority));
            }
            
            // 添加范围权限过滤（学生只能看到班级和年级相关的）
            if ("STUDENT".equals(userInfo.getRoleCode())) {
                whereClause.append(" AND (target_scope IN ('SCHOOL_WIDE', 'CLASS', 'GRADE') OR publisher_id = '")
                          .append(userInfo.getUserId()).append("')");
            }

            // 📋 Step 3: 查询待办列表数据 - 使用独立表
            String countSql = "SELECT COUNT(*) as total FROM todo_notifications " + whereClause;
            
            String dataSql = String.format(
                "SELECT id, title, content, priority, " +
                "DATE_FORMAT(deadline, '%%Y-%%m-%%d') as due_date, " +
                "status, publisher_name as assigner_name, " +
                "DATE_FORMAT(create_time, '%%Y-%%m-%%d %%H:%%i:%%s') as create_time " +
                "FROM todo_notifications %s " +
                "ORDER BY priority DESC, deadline ASC " +
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
     * 📝 T13.3 发布待办通知 - 双重认证版本 (使用DTO模式)
     */
    @PostMapping("/api/publish")
    @Operation(summary = "发布待办通知(双重认证+DTO)")
    @PermitAll
    @TenantIgnore
    public CommonResult<Map<String, Object>> publishTodoNotification(
            @RequestBody String jsonRequest,
            HttpServletRequest httpRequest) {
        
        log.info("📝 [TODO-PUBLISH] 发布待办通知请求开始");
        
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

            // 📝 Step 2: 使用类型安全的JSON解析 - 参考普通通知的成功模式
            TodoRequest request = parseTodoJsonRequest(jsonRequest);
            
            // 🛡️ Step 3: 验证请求参数 - 使用DTO对象进行验证
            List<String> validationErrors = validateTodoRequest(request);
            if (!validationErrors.isEmpty()) {
                log.warn("❌ [TODO-PUBLISH] 参数验证失败: {}", validationErrors);
                return CommonResult.error(400, "参数验证失败: " + String.join(", ", validationErrors));
            }

            // 🎯 Step 4: 权限验证 - 待办通知发布权限
            boolean hasPermission = validateTodoPublishPermission(userInfo.getRoleCode(), request.targetScope);
            if (!hasPermission) {
                log.warn("❌ [TODO-PUBLISH] 用户{}无权限发布{}范围的待办通知", 
                        userInfo.getUsername(), request.targetScope);
                return CommonResult.error(403, "无权限发布该范围的待办通知");
            }

            // 📋 Step 5: 构建待办通知数据 - 使用DTO对象
            Map<String, Object> notificationData = buildTodoNotificationDataFromDTO(request, userInfo);
            
            // 🗄️ Step 6: 插入数据库
            String insertSql = buildTodoInsertSQL(notificationData);
            log.info("🗄️ [TODO-PUBLISH] 执行插入SQL: {}", insertSql);
            
            boolean insertSuccess = executeSQLUpdate(insertSql);
            if (!insertSuccess) {
                log.error("❌ [TODO-PUBLISH] 数据库插入失败");
                return CommonResult.error(500, "发布待办通知失败");
            }

            // 🔍 Step 7: 获取插入的记录ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Map<String, Object> idResult = executeQueryAndReturnSingle(lastIdSql);
            Long notificationId = idResult != null ? 
                Long.parseLong(idResult.get("id").toString()) : null;

            // ✅ Step 8: 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("id", notificationId);
            result.put("title", request.title);
            result.put("level", 5);
            result.put("priority", request.priority);
            result.put("deadline", request.deadline);
            result.put("status", "pending");
            result.put("assignerName", userInfo.getUsername());
            result.put("targetScope", request.targetScope);
            result.put("publishedBy", userInfo.getUsername());
            result.put("publishedTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅ [TODO-PUBLISH] 待办通知发布成功 - id: {}, title: {}", notificationId, request.title);
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
     * 🛡️ 验证TodoRequest对象 - 替代原来的Map验证
     */
    private List<String> validateTodoRequest(TodoRequest request) {
        List<String> errors = new ArrayList<>();
        
        // 验证标题
        if (request.title == null || request.title.trim().isEmpty()) {
            errors.add("待办标题不能为空");
        } else if (request.title.length() > 200) {
            errors.add("待办标题长度不能超过200个字符");
        }
        
        // 验证内容
        if (request.content == null || request.content.trim().isEmpty()) {
            errors.add("待办内容不能为空");
        } else if (request.content.length() > 2000) {
            errors.add("待办内容长度不能超过2000个字符");
        }
        
        // 验证优先级
        if (request.priority == null || !Arrays.asList("low", "medium", "high").contains(request.priority)) {
            errors.add("待办优先级必须是 low、medium 或 high");
        }
        
        // 验证截止日期
        if (request.deadline == null || request.deadline.trim().isEmpty()) {
            errors.add("待办截止日期不能为空");
        } else if (!request.deadline.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            errors.add("待办截止日期格式不正确，应为 YYYY-MM-DD 或 YYYY-MM-DDTHH:mm:ss");
        }
        
        // 验证目标范围
        if (request.targetScope != null && !Arrays.asList("SCHOOL_WIDE", "DEPARTMENT", "GRADE", "CLASS").contains(request.targetScope)) {
            errors.add("目标范围必须是 SCHOOL_WIDE、DEPARTMENT、GRADE 或 CLASS");
        }
        
        log.info("📋 [TODO-VALIDATE] 待办DTO验证完成: {}, 错误数量: {}", 
                errors.isEmpty() ? "通过" : "失败", errors.size());
        return errors;
    }
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
        log.info("🔧 [TODO-BUILD] 开始构建待办数据 - 输入参数类型检查");
        request.forEach((key, value) -> {
            log.info("🔧 [TODO-BUILD] 输入字段 {}: 类型={}, 值={}", 
                key, value != null ? value.getClass().getSimpleName() : "null", value);
        });
        
        Map<String, Object> data = new HashMap<>();
        
        // 基本信息 - 使用安全转换方法
        String title = safeGetString(request, "title", "默认标题");
        String content = safeGetString(request, "content", "默认内容");
        
        data.put("title", SecurityEnhancementUtil.escapeHTML(title));
        data.put("content", SecurityEnhancementUtil.escapeHTML(content));
        data.put("summary", content.length() > 100 ? content.substring(0, 100) + "..." : content);
        data.put("level", 5); // 固定Level 5
        data.put("status", 3); // 直接发布状态
        
        // 修复category_id类型转换 - 使用安全转换方法
        Integer categoryId = safeGetInteger(request, "categoryId", 1);
        data.put("category_id", categoryId);
        
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
        
        // 目标范围 - 使用安全转换方法
        String targetScope = safeGetString(request, "targetScope", "CLASS");
        data.put("target_scope", targetScope);
        
        // 待办特有字段 - 使用安全类型转换方法
        String priorityStr = safeGetString(request, "priority", "medium");
        Integer priorityValue = PRIORITY_MAP.getOrDefault(priorityStr, 2); // 默认medium=2
        data.put("todo_priority", priorityValue);
        
        // 修复字段名不匹配 - API使用deadline，数据库使用dueDate，使用安全转换
        String deadline = safeGetString(request, "deadline", null);
        if (deadline == null) {
            deadline = safeGetString(request, "dueDate", "2025-12-31T23:59:59"); // 向后兼容
        }
        data.put("todo_deadline", deadline.contains(" ") ? deadline : deadline + " 23:59:59");
        
        data.put("todo_status", 0); // 初始状态pending=0
        
        // 系统字段
        data.put("push_channels", "1,5"); // 系统通知+待办提醒
        data.put("require_confirm", 1); // 需要确认
        data.put("pinned", 0);
        data.put("tenant_id", 1);
        data.put("creator", userInfo.getUsername());
        data.put("updater", userInfo.getUsername());
        
        // 调试日志 - 输出构建结果的类型检查
        log.info("🔧 [TODO-BUILD] 构建待办数据完成 - 输出数据类型检查:");
        data.forEach((key, value) -> {
            log.info("🔧 [TODO-BUILD] 输出字段 {}: 类型={}, 值={}", 
                key, value != null ? value.getClass().getSimpleName() : "null", value);
        });
        
        return data;
    }
    
    /**
     * 🚀 构建待办通知数据 - DTO版本 (类型安全)
     */
    private Map<String, Object> buildTodoNotificationDataFromDTO(TodoRequest request, UserInfo userInfo) {
        log.info("🔧 [TODO-BUILD-DTO] 开始构建待办数据 - 使用DTO对象");
        
        Map<String, Object> data = new HashMap<>();
        
        // 基本信息 - 直接从DTO对象获取，无需类型转换
        data.put("title", SecurityEnhancementUtil.escapeHTML(request.title));
        data.put("content", SecurityEnhancementUtil.escapeHTML(request.content));
        data.put("summary", request.content.length() > 100 ? request.content.substring(0, 100) + "..." : request.content);
        data.put("level", 5); // 固定Level 5
        data.put("status", 3); // 直接发布状态
        
        // 分类ID - 已经是Integer类型，无需转换
        data.put("category_id", request.categoryId);
        
        // 发布者信息
        String userId = userInfo.getUserId();
        Long publisherId = 999L; // 默认发布者ID
        if (userId != null && userId.contains("_")) {
            try {
                String numPart = userId.substring(userId.lastIndexOf("_") + 1);
                publisherId = Long.parseLong(numPart);
            } catch (Exception e) {
                log.warn("⚠️ [TODO-BUILD-DTO] 无法解析用户ID数字部分，使用默认: {}", userId);
            }
        }
        data.put("publisher_id", publisherId);
        data.put("publisher_name", userInfo.getUsername());
        data.put("publisher_role", userInfo.getRoleCode());
        
        // 目标范围 - 直接从DTO获取
        data.put("target_scope", request.targetScope);
        
        // 待办特有字段 - 类型安全处理
        Integer priorityValue = PRIORITY_MAP.getOrDefault(request.priority, 2); // 默认medium=2
        data.put("todo_priority", priorityValue);
        
        // 截止时间处理
        String deadline = request.deadline;
        if (!deadline.contains(" ")) {
            deadline = deadline + " 23:59:59"; // 补充时间部分
        }
        data.put("todo_deadline", deadline);
        data.put("todo_status", 0); // 修复：使用数值类型，0=pending
        
        // 系统字段
        data.put("push_channels", "1,5"); // 系统通知+待办提醒
        data.put("require_confirm", 1); // 需要确认
        data.put("pinned", 0);
        data.put("tenant_id", 1);
        data.put("creator", userInfo.getUsername());
        data.put("updater", userInfo.getUsername());
        
        log.info("🔧 [TODO-BUILD-DTO] 构建待办数据完成 - 标题: {}, 优先级: {}, 截止时间: {}", 
                request.title, request.priority, deadline);
                
        // 🔍 调试：输出构建数据的类型信息
        data.forEach((key, value) -> {
            log.info("🔍 [TODO-BUILD-DTO] 字段 {}: 类型={}, 值={}", 
                key, value != null ? value.getClass().getSimpleName() : "null", value);
        });
        
        return data;
    }

    /**
     * 🚀 构建待办插入SQL - 直接使用简化SQL方法
     */
    private String buildTodoInsertSQL(Map<String, Object> data) {
        log.info("🔧 [TODO-SQL] 使用简化SQL方法构建待办通知SQL (绕过SafeSQLExecutor Level限制)");
        // 直接使用简化SQL，因为SafeSQLExecutor不支持Level 5
        return buildSimpleTodoSQL(data);
    }
    
    /**
     * 🔧 独立待办表SQL构建 - 使用todo_notifications表
     */
    private String buildSimpleTodoSQL(Map<String, Object> data) {
        return String.format(
            "INSERT INTO todo_notifications " +
            "(tenant_id, title, content, priority, deadline, status, publisher_id, publisher_name, publisher_role, target_scope, " +
            "category_id, push_channels, require_confirm, creator, updater) " +
            "VALUES " +
            "(1, '%s', '%s', %d, '%s', %d, %d, '%s', '%s', '%s', " +
            "%d, '%s', %d, '%s', '%s')",
            
            SecurityEnhancementUtil.escapeSQL((String) data.get("title")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("content")), 
            (Integer) data.get("todo_priority"), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("todo_deadline")), 
            (Integer) data.get("todo_status"),
            (Long) data.get("publisher_id"),
            SecurityEnhancementUtil.escapeSQL((String) data.get("publisher_name")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("publisher_role")), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("target_scope")),
            (Integer) data.get("category_id"), 
            SecurityEnhancementUtil.escapeSQL((String) data.get("push_channels")), 
            (Integer) data.get("require_confirm"),
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
    
    // ========================= Linux环境类型安全转换方法 =========================
    
    /**
     * 🔧 安全获取字符串值 - 解决Linux环境下类型转换问题
     */
    private String safeGetString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value.toString();
    }
    
    /**
     * 🔧 安全获取整数值 - 解决Linux环境下类型转换问题
     */
    private Integer safeGetInteger(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("⚠️ [SAFE-CONVERT] {}字段格式错误: {}, 使用默认值: {}", key, value, defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 📝 待办通知请求DTO - 参考NotificationRequest的成功模式
     */
    public static class TodoRequest {
        public String title;
        public String content;
        public String priority;      // low/medium/high
        public String deadline;      // ISO 8601格式
        public Integer categoryId;   // 分类ID
        public String targetScope;   // SCHOOL_WIDE/DEPARTMENT/GRADE/CLASS
        
        // 无参构造函数 - Jackson反序列化必需
        public TodoRequest() {}
    }
    
    /**
     * 🔧 安全JSON解析方法 - 参考parseJsonRequest的成功经验
     */
    private TodoRequest parseTodoJsonRequest(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            TodoRequest request = new TodoRequest();
            
            // 使用JsonNode的类型安全方法 - 避免类型转换问题
            request.title = jsonNode.has("title") ? jsonNode.get("title").asText("待办事项") : "待办事项";
            request.content = jsonNode.has("content") ? jsonNode.get("content").asText("待办内容") : "待办内容";
            request.priority = jsonNode.has("priority") ? jsonNode.get("priority").asText("medium") : "medium";
            request.deadline = jsonNode.has("deadline") ? jsonNode.get("deadline").asText("2025-12-31T23:59:59") : "2025-12-31T23:59:59";
            request.categoryId = jsonNode.has("categoryId") ? jsonNode.get("categoryId").asInt(1) : 1;
            request.targetScope = jsonNode.has("targetScope") ? jsonNode.get("targetScope").asText("CLASS") : "CLASS";
            
            log.info("🔧 [TODO-JSON-PARSE] 成功解析: title={}, priority={}, deadline={}, categoryId={}", 
                    request.title, request.priority, request.deadline, request.categoryId);
            
            return request;
        } catch (Exception e) {
            log.warn("🔧 [TODO-JSON-PARSE] JSON解析失败，使用默认值: {}", e.getMessage());
            // 返回默认请求对象
            TodoRequest defaultRequest = new TodoRequest();
            defaultRequest.title = "默认待办事项";
            defaultRequest.content = "默认待办内容";
            defaultRequest.priority = "medium";
            defaultRequest.deadline = "2025-12-31T23:59:59";
            defaultRequest.categoryId = 1;
            defaultRequest.targetScope = "CLASS";
            return defaultRequest;
        }
    }
}