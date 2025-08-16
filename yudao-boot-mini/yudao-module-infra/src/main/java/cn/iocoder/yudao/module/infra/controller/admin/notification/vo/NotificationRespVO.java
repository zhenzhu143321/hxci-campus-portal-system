package cn.iocoder.yudao.module.infra.controller.admin.notification.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 智能通知信息 Response VO")
@Data
public class NotificationRespVO {

    @Schema(description = "通知编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @Schema(description = "通知标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "重要通知：期末考试安排")
    private String title;

    @Schema(description = "通知内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "请各位同学注意...")
    private String content;

    @Schema(description = "通知摘要", example = "期末考试时间安排通知")
    private String summary;

    @Schema(description = "通知级别", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer level;

    @Schema(description = "通知级别名称", example = "重要通知")
    private String levelName;

    @Schema(description = "通知级别颜色", example = "#f97316")
    private String levelColor;

    @Schema(description = "通知状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer status;

    @Schema(description = "通知状态名称", example = "已发布")
    private String statusName;

    @Schema(description = "通知分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "发布者ID", example = "100")
    private Long publisherId;

    @Schema(description = "发布者姓名", example = "张老师")
    private String publisherName;

    @Schema(description = "发布者角色", example = "TEACHER")
    private String publisherRole;

    @Schema(description = "定时发布时间", example = "2024-07-30 14:00:00")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime scheduledTime;

    @Schema(description = "过期时间", example = "2024-08-30 23:59:59")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expiredTime;

    @Schema(description = "推送渠道", example = "APP,SMS,SYSTEM")
    private String pushChannels;

    @Schema(description = "是否需要确认", example = "true")
    private Boolean requireConfirm;

    @Schema(description = "是否置顶", example = "false")
    private Boolean pinned;

    @Schema(description = "推送次数", example = "3")
    private Integer pushCount;

    @Schema(description = "阅读次数", example = "156")
    private Integer readCount;

    @Schema(description = "确认次数", example = "142")
    private Integer confirmCount;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-07-25 10:30:00")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-07-25 15:20:00")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime updateTime;
}