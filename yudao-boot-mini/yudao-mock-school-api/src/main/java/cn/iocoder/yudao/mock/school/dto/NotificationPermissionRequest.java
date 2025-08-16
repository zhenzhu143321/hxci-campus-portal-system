package cn.iocoder.yudao.mock.school.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 通知权限验证请求DTO
 * 
 * @author Claude
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPermissionRequest {

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 通知级别: 1=紧急, 2=重要, 3=常规, 4=提醒
     */
    @NotNull(message = "通知级别不能为空")
    @Min(value = 1, message = "通知级别必须在1-4之间")
    @Max(value = 4, message = "通知级别必须在1-4之间")
    private Integer notificationLevel;

    /**
     * 目标范围: ALL_SCHOOL, DEPARTMENT, CLASS等
     */
    @NotBlank(message = "目标范围不能为空")
    private String targetScope;

    /**
     * 通知标题（用于日志记录）
     */
    private String title;
}