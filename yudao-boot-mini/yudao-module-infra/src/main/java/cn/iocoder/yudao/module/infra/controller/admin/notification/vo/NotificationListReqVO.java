package cn.iocoder.yudao.module.infra.controller.admin.notification.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 智能通知列表 Request VO")
@Data
public class NotificationListReqVO {

    @Schema(description = "通知标题，模糊匹配", example = "考试")
    private String title;

    @Schema(description = "通知级别", example = "2")
    private Integer level;

    @Schema(description = "通知状态", example = "3")
    private Integer status;

    @Schema(description = "通知分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "发布者角色", example = "TEACHER")
    private String publisherRole;

    @Schema(description = "开始创建时间", example = "2024-07-01 00:00:00")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginCreateTime;

    @Schema(description = "结束创建时间", example = "2024-07-31 23:59:59")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endCreateTime;

    @Schema(description = "是否置顶", example = "true")
    private Boolean pinned;

    @Schema(description = "最大返回数量", example = "100")
    private Integer limit = 100;
}