package cn.iocoder.yudao.server.service.todo;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.server.dal.dataobject.todo.TodoNotificationDO;
import cn.iocoder.yudao.server.dal.mysql.todo.TodoNotificationMapper;
import cn.iocoder.yudao.server.security.AccessControlListManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

}