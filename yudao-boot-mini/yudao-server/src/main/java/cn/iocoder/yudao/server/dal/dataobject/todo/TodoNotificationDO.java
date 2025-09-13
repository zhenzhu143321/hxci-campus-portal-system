package cn.iocoder.yudao.server.dal.dataobject.todo;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 待办通知数据对象
 * 
 * @author 芋道源码
 */
@TableName("todo_notifications")
@Data
@EqualsAndHashCode(callSuper = false)
public class TodoNotificationDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 优先级：1=高优先级，2=中优先级，3=低优先级
     */
    private Integer priority;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 状态：1=待处理，2=进行中，3=已完成，4=已过期
     */
    private Integer status;

    /**
     * 发布者名称
     */
    @TableField("publisher_name")
    private String publisherName;

    /**
     * 发布者角色
     */
    @TableField("publisher_role")
    private String publisherRole;

    /**
     * 目标范围：SCHOOL_WIDE/DEPARTMENT/GRADE/CLASS
     */
    @TableField("target_scope")
    private String targetScope;

    /**
     * 目标学生ID列表（JSON格式）
     */
    @TableField("target_student_ids")
    private String targetStudentIds;

    /**
     * 目标年级ID列表（JSON格式）
     */
    @TableField("target_grade_ids")
    private String targetGradeIds;

    /**
     * 目标班级ID列表（JSON格式）
     */
    @TableField("target_class_ids")
    private String targetClassIds;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Integer categoryId;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 重写hashCode方法，处理id为null的情况
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * 重写equals方法，处理id为null的情况
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TodoNotificationDO)) return false;
        TodoNotificationDO that = (TodoNotificationDO) o;
        return Objects.equals(this.id, that.id);
    }

    /**
     * 提供主键访问方法，避免TransPojo接口导致的NPE
     * MyBatis Plus可能会调用此方法
     */
    public Serializable getPk() {
        return this.id;
    }

}