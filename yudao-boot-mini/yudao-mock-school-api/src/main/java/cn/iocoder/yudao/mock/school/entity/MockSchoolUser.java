package cn.iocoder.yudao.mock.school.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock School 用户实体
 * 模拟教育机构中的用户信息，包含角色和权限数据
 * 
 * @author Claude
 */
@Entity
@Table(name = "mock_school_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockSchoolUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * School系统用户ID
     */
    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    /**
     * 角色编码
     */
    @Column(name = "role_code", nullable = false, length = 30)
    private String roleCode;

    /**
     * 角色名称
     */
    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    /**
     * 部门ID
     */
    @Column(name = "department_id")
    private Long departmentId;

    /**
     * 部门名称
     */
    @Column(name = "department_name", length = 100)
    private String departmentName;

    // 注释掉permissions字段，因为数据库表中没有此字段
    // /**
    //  * 权限列表（JSON格式）
    //  */
    // @Column(name = "permissions", columnDefinition = "JSON")
    // private String permissions;

    /**
     * Mock Token
     */
    @Column(name = "token", length = 200)
    private String token;

    /**
     * Token过期时间
     */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (tokenExpiresTime == null) {
            // 默认token 24小时后过期
            tokenExpiresTime = LocalDateTime.now().plusHours(24);
        }
    }

    /**
     * 检查token是否过期
     */
    public boolean isTokenExpired() {
        return tokenExpiresTime != null && tokenExpiresTime.isBefore(LocalDateTime.now());
    }

    /**
     * 检查是否有指定权限
     */
    public boolean hasPermission(String permission) {
        // 暂时返回false，因为permissions字段不存在
        return false;
    }
}