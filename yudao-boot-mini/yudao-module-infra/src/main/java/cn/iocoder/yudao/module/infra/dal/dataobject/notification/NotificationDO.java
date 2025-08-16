package cn.iocoder.yudao.module.infra.dal.dataobject.notification;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.infra.enums.notification.NotificationLevelEnum;
import cn.iocoder.yudao.module.infra.enums.notification.NotificationPushChannelEnum;
import cn.iocoder.yudao.module.infra.enums.notification.NotificationStatusEnum;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("notification_info")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String title;

    private String content;

    private String summary;

    private Integer level;

    private Integer status;

    private Long categoryId;

    private Long publisherId;

    private String publisherName;

    private String publisherRole;

    private LocalDateTime scheduledTime;

    private LocalDateTime expiredTime;

    private String pushChannels;

    private Boolean requireConfirm;

    private Boolean pinned;

    private Integer pushCount;

    private Integer readCount;

    private Integer confirmCount;

    public NotificationLevelEnum getLevelEnum() {
        return NotificationLevelEnum.valueOf(this.level);
    }

    public NotificationStatusEnum getStatusEnum() {
        return NotificationStatusEnum.valueOf(this.status);
    }
}