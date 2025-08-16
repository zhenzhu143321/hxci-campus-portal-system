package cn.iocoder.yudao.mock.school.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 权限验证结果DTO
 * 
 * @author Claude
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResult {

    /**
     * 是否授权
     */
    private Boolean permissionGranted;

    /**
     * 权限级别
     */
    private String permissionLevel;

    /**
     * 是否需要审批
     */
    private Boolean approvalRequired;

    /**
     * 审批者角色
     */
    private String approverRole;

    /**
     * 权限验证消息
     */
    private String message;

    /**
     * 允许的目标范围
     */
    private String allowedScope;

    /**
     * 创建成功的权限结果
     */
    public static PermissionResult granted(String level, Boolean requiresApproval, String message) {
        return new PermissionResult(true, level, requiresApproval, null, message, null);
    }

    /**
     * 创建拒绝的权限结果
     */
    public static PermissionResult denied(String message) {
        return new PermissionResult(false, null, null, null, message, null);
    }

    /**
     * 创建需要审批的权限结果
     */
    public static PermissionResult requiresApproval(String level, String approverRole, String message) {
        return new PermissionResult(true, level, true, approverRole, message, null);
    }
}