package cn.iocoder.yudao.mock.school.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息响应DTO
 * 支持两步认证架构：Mock API认证 + 主服务权限查询
 * 
 * @author Claude
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 学号/工号（关键字段 - 用于主服务权限查询）
     */
    private String employeeId;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    // 🚫 [REFACTORED] 移除权限列表字段 - 权限验证转移到主服务
    // 原 permissions 字段已删除，Mock API 只负责身份认证

    /**
     * 用户是否启用
     */
    private Boolean enabled;

    /**
     * 🆕 JWT Token - 用于主通知服务的权限验证
     */
    private String accessToken;

    /**
     * 🆕 Token过期时间
     */
    private LocalDateTime tokenExpireTime;

    /**
     * 🆕 用户类型（STUDENT学生, TEACHER教师, ADMIN管理员等）
     */
    private String userType;
}