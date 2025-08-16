package cn.iocoder.yudao.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 通知发布请求DTO
 * 
 * @author Claude
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPublishRequest {

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 通知摘要
     */
    private String summary;

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
     * 是否需要确认
     */
    private Boolean requireConfirm = false;

    /**
     * 是否置顶
     */
    private Boolean pinned = false;

    /**
     * 定时发布时间
     */
    private String scheduledTime;

    /**
     * 过期时间
     */
    private String expiredTime;

    /**
     * 推送渠道: 1,2,5 对应 APP,SMS,SYSTEM
     */
    private String pushChannels;
}