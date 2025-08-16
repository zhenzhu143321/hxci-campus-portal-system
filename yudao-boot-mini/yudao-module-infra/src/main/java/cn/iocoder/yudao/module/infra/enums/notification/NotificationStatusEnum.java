package cn.iocoder.yudao.module.infra.enums.notification;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum NotificationStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PENDING_APPROVAL(1, "待审批"),
    APPROVED(2, "已审批"),
    PUBLISHED(3, "已发布"),
    CANCELLED(4, "已取消"),
    EXPIRED(5, "已过期"),
    ARCHIVED(6, "已归档");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NotificationStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static NotificationStatusEnum valueOf(Integer status) {
        return Arrays.stream(values()).filter(notificationStatus -> notificationStatus.getStatus().equals(status)).findFirst().orElse(null);
    }

    public boolean canEdit() {
        return this == DRAFT || this == PENDING_APPROVAL;
    }

    public boolean canApprove() {
        return this == PENDING_APPROVAL;
    }

    public boolean canPublish() {
        return this == APPROVED;
    }

    public boolean canView() {
        return this == PUBLISHED || this == EXPIRED || this == ARCHIVED;
    }
}