package cn.iocoder.yudao.server.dal.dataobject.todo;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户待办状态数据对象
 * 对应数据库表：todo_completions
 *
 * 功能说明：
 * 1. 记录每个用户对待办通知的个人状态（未读/已读/已完成/已隐藏）
 * 2. 支持用户级别的状态隔离，不影响其他用户
 * 3. 支持乐观锁防止并发更新冲突
 *
 * @author Claude AI
 * @since 2025-09-15
 */
@TableName("todo_completions")
@Data
@EqualsAndHashCode(callSuper = false)
public class TodoCompletionDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 待办通知ID
     */
    @TableField("todo_id")
    private Long todoId;

    /**
     * 用户ID（使用用户名作为ID）
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 用户角色
     */
    @TableField("user_role")
    private String userRole;

    /**
     * 状态：0=未读，1=已读，2=已完成，3=已隐藏
     */
    @TableField("status")
    private Integer status;

    /**
     * 已读时间
     */
    @TableField("read_at")
    private LocalDateTime readAt;

    /**
     * 完成时间
     */
    @TableField("completed_time")
    private LocalDateTime completedTime;

    /**
     * 隐藏时间
     */
    @TableField("hidden_at")
    private LocalDateTime hiddenAt;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 状态常量定义 ====================

    /**
     * 状态枚举
     */
    public static class Status {
        /** 未读 */
        public static final Integer UNREAD = 0;
        /** 已读 */
        public static final Integer READ = 1;
        /** 已完成 */
        public static final Integer COMPLETED = 2;
        /** 已隐藏 */
        public static final Integer HIDDEN = 3;
    }

    // ==================== 辅助方法 ====================

    /**
     * 是否已读
     */
    public boolean isRead() {
        return status != null && status >= Status.READ;
    }

    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return status != null && status.equals(Status.COMPLETED);
    }

    /**
     * 是否已隐藏
     */
    public boolean isHidden() {
        return status != null && status.equals(Status.HIDDEN);
    }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        if (this.status == null || this.status.equals(Status.UNREAD)) {
            this.status = Status.READ;
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * 标记为已完成
     */
    public void markAsCompleted() {
        this.status = Status.COMPLETED;
        this.completedTime = LocalDateTime.now();
        // 完成时自动标记为已读
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * 标记为隐藏
     */
    public void markAsHidden() {
        this.status = Status.HIDDEN;
        this.hiddenAt = LocalDateTime.now();
    }

    /**
     * 重写hashCode方法，处理id为null的情况
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, todoId, userId, tenantId);
    }

    /**
     * 重写equals方法，处理id为null的情况
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TodoCompletionDO)) return false;
        TodoCompletionDO that = (TodoCompletionDO) o;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.todoId, that.todoId) &&
               Objects.equals(this.userId, that.userId) &&
               Objects.equals(this.tenantId, that.tenantId);
    }

    /**
     * 提供主键访问方法，避免TransPojo接口导致的NPE
     * MyBatis Plus可能会调用此方法
     */
    public Serializable getPk() {
        return this.id;
    }
}