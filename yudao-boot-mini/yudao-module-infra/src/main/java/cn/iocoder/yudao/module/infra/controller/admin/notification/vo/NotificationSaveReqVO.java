package cn.iocoder.yudao.module.infra.controller.admin.notification.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 智能通知创建/修改 Request VO")
@Data
public class NotificationSaveReqVO {

    @Schema(description = "通知编号", example = "1024")
    private Long id;

    @Schema(description = "通知标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "重要通知：期末考试安排")
    @NotBlank(message = "通知标题不能为空")
    @Size(max = 200, message = "通知标题不能超过200个字符")
    private String title;

    @Schema(description = "通知内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "请各位同学注意...")
    @NotBlank(message = "通知内容不能为空")
    private String content;

    @Schema(description = "通知摘要", example = "期末考试时间安排通知")
    @Size(max = 500, message = "通知摘要不能超过500个字符")
    private String summary;

    @Schema(description = "通知级别", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "通知级别不能为空")
    private Integer level;

    @Schema(description = "通知分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "发布者姓名", example = "张老师")
    @Size(max = 50, message = "发布者姓名不能超过50个字符")
    private String publisherName;

    @Schema(description = "发布者角色", example = "TEACHER")
    @Size(max = 30, message = "发布者角色不能超过30个字符")
    private String publisherRole;

    @Schema(description = "定时发布时间", example = "2024-07-30 14:00:00")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime scheduledTime;

    @Schema(description = "过期时间", example = "2024-08-30 23:59:59")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expiredTime;

    @Schema(description = "推送渠道", example = "[1,2,5]")
    private List<Integer> pushChannels;

    @Schema(description = "是否需要确认", example = "true")
    private Boolean requireConfirm;

    @Schema(description = "是否置顶", example = "false")
    private Boolean pinned;
}