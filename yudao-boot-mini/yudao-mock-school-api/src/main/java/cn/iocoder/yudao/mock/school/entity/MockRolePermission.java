package cn.iocoder.yudao.mock.school.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mock 角色权限实体
 * 定义教育机构中不同角色的权限配置
 * 
 * @author Claude
 */
@Entity
@Table(name = "mock_role_permissions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_code", "permission_code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 角色编码
     */
    @Column(name = "role_code", nullable = false, length = 30)
    private String roleCode;

    /**
     * 权限编码
     */
    @Column(name = "permission_code", nullable = false, length = 50)
    private String permissionCode;

    /**
     * 权限名称
     */
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    /**
     * 可发布通知级别：1,2,3,4 对应紧急、重要、常规、提醒
     */
    @Column(name = "notification_levels", length = 20)
    private String notificationLevels;

    /**
     * 通知范围: ALL_SCHOOL, DEPARTMENT, CLASS等
     */
    @Column(name = "target_scope", length = 100)
    private String targetScope;

    /**
     * 是否需要审批
     */
    @Column(name = "approval_required")
    private Boolean approvalRequired = false;

    /**
     * 权限描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    /**
     * 检查是否允许发布指定级别的通知
     */
    public boolean canPublishNotificationLevel(int level) {
        if (notificationLevels == null) {
            return false;
        }
        return notificationLevels.contains(String.valueOf(level));
    }

    /**
     * 检查目标范围是否匹配
     */
    public boolean matchesTargetScope(String scope) {
        if (targetScope == null) {
            return false;
        }
        return "ALL_SCHOOL".equals(targetScope) || targetScope.equals(scope);
    }
}