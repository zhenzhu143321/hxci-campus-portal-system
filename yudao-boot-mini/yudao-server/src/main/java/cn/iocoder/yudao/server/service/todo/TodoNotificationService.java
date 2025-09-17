package cn.iocoder.yudao.server.service.todo;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.server.dal.dataobject.todo.TodoNotificationDO;
import cn.iocoder.yudao.server.dal.dataobject.todo.TodoCompletionDO;
import cn.iocoder.yudao.server.dal.mysql.todo.TodoNotificationMapper;
import cn.iocoder.yudao.server.dal.mysql.todo.TodoCompletionMapper;
import cn.iocoder.yudao.server.security.AccessControlListManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 待办通知服务
 * 使用 MyBatis Plus 的 @TableLogic 自动处理逻辑删除
 * 
 * @author 芋道源码
 */
@Service
@Slf4j
public class TodoNotificationService {

    @Autowired
    private TodoNotificationMapper todoNotificationMapper;

    @Autowired
    private TodoCompletionMapper todoCompletionMapper;

    /**
     * 分页查询用户的待办列表（自动处理逻辑删除）
     * 
     * @param page 页码
     * @param pageSize 页大小
     * @param status 状态筛选
     * @param priority 优先级筛选
     * @param userInfo 用户信息
     * @return 分页结果
     */
    public PageResult<TodoNotificationDO> getMyTodos(int page, int pageSize, 
                                                    Integer status, Integer priority, 
                                                    AccessControlListManager.UserInfo userInfo) {
        
        log.info("🔍 [TODO_SERVICE] 查询待办列表: user={}, page={}, pageSize={}, status={}, priority={}", 
                userInfo.getUsername(), page, pageSize, status, priority);

        // 构建查询条件（MyBatis Plus自动处理 deleted = 0）
        LambdaQueryWrapper<TodoNotificationDO> wrapper = Wrappers.lambdaQuery();

        // 状态筛选
        if (status != null) {
            wrapper.eq(TodoNotificationDO::getStatus, status);
        }

        // 优先级筛选
        if (priority != null) {
            wrapper.eq(TodoNotificationDO::getPriority, priority);
        }

        // 应用范围权限过滤
        applyScopeFilter(wrapper, userInfo);

        // 排序：优先级降序，截止时间升序
        wrapper.orderByDesc(TodoNotificationDO::getPriority)
               .orderByAsc(TodoNotificationDO::getDeadline);

        // 分页查询
        Page<TodoNotificationDO> pageReq = new Page<>(page, pageSize);
        IPage<TodoNotificationDO> pageResult = todoNotificationMapper.selectPage(pageReq, wrapper);

        log.info("✅ [TODO_SERVICE] 查询完成: 总数={}, 当前页数据={}", 
                pageResult.getTotal(), pageResult.getRecords().size());

        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal());
    }

    /**
     * 根据ID查询单个待办（自动处理逻辑删除）
     */
    public TodoNotificationDO getTodoById(Long id) {
        if (id == null) {
            return null;
        }
        return todoNotificationMapper.selectById(id);
    }

    /**
     * 逻辑删除待办
     */
    public boolean deleteTodoById(Long id) {
        if (id == null) {
            return false;
        }
        int result = todoNotificationMapper.deleteById(id);
        return result > 0;
    }

    /**
     * 应用范围权限过滤
     * 将原来的 buildScopeFilter 逻辑转换为 Wrapper 条件
     */
    private void applyScopeFilter(LambdaQueryWrapper<TodoNotificationDO> wrapper, 
                                 AccessControlListManager.UserInfo userInfo) {
        
        String roleCode = userInfo.getRoleCode();
        
        if ("SYSTEM_ADMIN".equals(roleCode) || "PRINCIPAL".equals(roleCode)) {
            // 系统管理员和校长可以看到所有待办
            log.debug("🔓 [SCOPE_FILTER] 管理员权限，无需过滤");
            return;
        }
        
        if ("ACADEMIC_ADMIN".equals(roleCode)) {
            // 教务主任可以看到全校、部门和年级级别的待办
            wrapper.and(w -> w.eq(TodoNotificationDO::getTargetScope, "SCHOOL_WIDE")
                            .or().eq(TodoNotificationDO::getTargetScope, "DEPARTMENT")
                            .or().eq(TodoNotificationDO::getTargetScope, "GRADE"));
            log.debug("🔒 [SCOPE_FILTER] 教务主任权限过滤应用");
            return;
        }

        if ("TEACHER".equals(roleCode) || "CLASS_TEACHER".equals(roleCode)) {
            // 🔧 修复：教师和班主任可以看到全校、部门、年级和班级级别的待办
            wrapper.and(w -> w.eq(TodoNotificationDO::getTargetScope, "SCHOOL_WIDE")
                            .or().eq(TodoNotificationDO::getTargetScope, "DEPARTMENT")
                            .or().eq(TodoNotificationDO::getTargetScope, "GRADE")
                            .or().eq(TodoNotificationDO::getTargetScope, "CLASS"));
            log.debug("🔒 [SCOPE_FILTER] 教师权限过滤应用 - 包含SCHOOL_WIDE");
            return;
        }

        if ("STUDENT".equals(roleCode)) {
            // 🔧 修复：学生可以看到全校和班级级别的待办
            wrapper.and(w -> w.eq(TodoNotificationDO::getTargetScope, "SCHOOL_WIDE")
                            .or().eq(TodoNotificationDO::getTargetScope, "CLASS"));
            log.debug("🔒 [SCOPE_FILTER] 学生权限过滤应用 - 包含SCHOOL_WIDE");
            return;
        }
        
        // 默认情况：只能看到班级级别的待办
        wrapper.eq(TodoNotificationDO::getTargetScope, "CLASS");
        log.warn("⚠️  [SCOPE_FILTER] 未知角色 {}，应用默认权限过滤", roleCode);
    }

    // ==================== 用户状态管理功能 ====================

    /**
     * 获取用户的待办状态映射
     *
     * @param todoIds 待办ID列表
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 待办ID -> 状态记录的映射
     */
    public Map<Long, TodoCompletionDO> getUserTodoStatusMap(List<Long> todoIds, Long tenantId, String userId) {
        if (todoIds == null || todoIds.isEmpty() || userId == null) {
            return Map.of();
        }

        List<TodoCompletionDO> statusList = todoCompletionMapper.selectBatchByTodoIds(todoIds, tenantId, userId);
        return statusList.stream()
            .collect(Collectors.toMap(TodoCompletionDO::getTodoId, status -> status));
    }

    /**
     * 获取单个待办的用户状态
     *
     * @param todoId 待办ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 待办状态记录
     */
    public TodoCompletionDO getUserTodoStatus(Long todoId, Long tenantId, String userId) {
        if (todoId == null || userId == null) {
            return null;
        }
        return todoCompletionMapper.selectByTodoAndUser(todoId, tenantId, userId);
    }

    /**
     * 初始化用户待办状态（当用户第一次看到待办时）
     *
     * @param todoId 待办ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param userRole 用户角色
     * @return 是否初始化成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean initUserTodoStatus(Long todoId, Long tenantId, String userId, String userName, String userRole) {
        try {
            // 检查是否已存在记录
            TodoCompletionDO existing = getUserTodoStatus(todoId, tenantId, userId);
            if (existing != null) {
                log.debug("待办状态已存在，无需初始化: todoId={}, userId={}", todoId, userId);
                return true;
            }

            // 插入新记录
            int result = todoCompletionMapper.insertOrUpdateStatus(todoId, tenantId, userId, userName, userRole);
            return result > 0;
        } catch (Exception e) {
            log.error("初始化待办状态失败: todoId={}, userId={}", todoId, userId, e);
            return false;
        }
    }

    /**
     * 标记待办为已读
     *
     * @param todoId 待办ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 是否标记成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markTodoAsRead(Long todoId, Long tenantId, String userId) {
        try {
            // 获取当前状态
            TodoCompletionDO status = getUserTodoStatus(todoId, tenantId, userId);
            if (status == null) {
                log.warn("待办状态不存在，无法标记已读: todoId={}, userId={}", todoId, userId);
                return false;
            }

            // 如果已经是已读或更高状态，直接返回成功
            if (status.isRead()) {
                log.debug("待办已处于已读状态: todoId={}, userId={}", todoId, userId);
                return true;
            }

            // 执行更新（使用乐观锁）
            int result = todoCompletionMapper.markAsRead(status.getId(), tenantId, userId, status.getVersion());
            if (result > 0) {
                log.info("✅ 标记待办已读成功: todoId={}, userId={}", todoId, userId);
                return true;
            } else {
                log.warn("标记待办已读失败（乐观锁冲突）: todoId={}, userId={}", todoId, userId);
                return false;
            }
        } catch (Exception e) {
            log.error("标记待办已读异常: todoId={}, userId={}", todoId, userId, e);
            return false;
        }
    }

    /**
     * 标记待办为已完成
     *
     * @param todoId 待办ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param userRole 用户角色
     * @return 是否标记成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markTodoAsCompleted(Long todoId, Long tenantId, String userId, String userName, String userRole) {
        try {
            // 获取当前状态
            TodoCompletionDO status = getUserTodoStatus(todoId, tenantId, userId);

            // 如果不存在，先初始化
            if (status == null) {
                log.info("待办状态不存在，先初始化: todoId={}, userId={}", todoId, userId);
                boolean initSuccess = initUserTodoStatus(todoId, tenantId, userId, userName, userRole);
                if (!initSuccess) {
                    log.error("初始化待办状态失败，无法标记完成");
                    return false;
                }
                status = getUserTodoStatus(todoId, tenantId, userId);
            }

            // 如果已经完成，直接返回成功
            if (status.isCompleted()) {
                log.debug("待办已处于完成状态: todoId={}, userId={}", todoId, userId);
                return true;
            }

            // 执行更新（使用乐观锁）
            int result = todoCompletionMapper.markAsCompleted(status.getId(), tenantId, userId, status.getVersion());
            if (result > 0) {
                log.info("✅ 标记待办完成成功: todoId={}, userId={}", todoId, userId);
                return true;
            } else {
                log.warn("标记待办完成失败（乐观锁冲突）: todoId={}, userId={}", todoId, userId);
                return false;
            }
        } catch (Exception e) {
            log.error("标记待办完成异常: todoId={}, userId={}", todoId, userId, e);
            return false;
        }
    }

    /**
     * 标记待办为隐藏
     *
     * @param todoId 待办ID
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 是否标记成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markTodoAsHidden(Long todoId, Long tenantId, String userId) {
        try {
            // 获取当前状态
            TodoCompletionDO status = getUserTodoStatus(todoId, tenantId, userId);
            if (status == null) {
                log.warn("待办状态不存在，无法标记隐藏: todoId={}, userId={}", todoId, userId);
                return false;
            }

            // 如果已经隐藏，直接返回成功
            if (status.isHidden()) {
                log.debug("待办已处于隐藏状态: todoId={}, userId={}", todoId, userId);
                return true;
            }

            // 执行更新（使用乐观锁）
            int result = todoCompletionMapper.markAsHidden(status.getId(), tenantId, userId, status.getVersion());
            if (result > 0) {
                log.info("✅ 标记待办隐藏成功: todoId={}, userId={}", todoId, userId);
                return true;
            } else {
                log.warn("标记待办隐藏失败（乐观锁冲突）: todoId={}, userId={}", todoId, userId);
                return false;
            }
        } catch (Exception e) {
            log.error("标记待办隐藏异常: todoId={}, userId={}", todoId, userId, e);
            return false;
        }
    }

    /**
     * 批量标记待办为已读
     *
     * @param todoIds 待办ID列表
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 成功标记的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchMarkAsRead(List<Long> todoIds, Long tenantId, String userId) {
        if (todoIds == null || todoIds.isEmpty()) {
            return 0;
        }

        try {
            int result = todoCompletionMapper.batchMarkAsRead(todoIds, tenantId, userId);
            log.info("✅ 批量标记已读成功: 数量={}, userId={}", result, userId);
            return result;
        } catch (Exception e) {
            log.error("批量标记已读异常: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 获取用户的待办状态统计
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 各状态的数量统计
     */
    public Map<String, Long> getUserTodoStatusCount(Long tenantId, String userId) {
        List<TodoCompletionMapper.StatusCountVO> counts = todoCompletionMapper.selectStatusCount(tenantId, userId);

        // 转换为更友好的格式
        Map<String, Long> result = Map.of(
            "unread", 0L,
            "read", 0L,
            "completed", 0L,
            "hidden", 0L
        );

        for (TodoCompletionMapper.StatusCountVO count : counts) {
            switch (count.getStatus()) {
                case 0 -> result.put("unread", count.getCount());
                case 1 -> result.put("read", count.getCount());
                case 2 -> result.put("completed", count.getCount());
                case 3 -> result.put("hidden", count.getCount());
            }
        }

        return result;
    }

}