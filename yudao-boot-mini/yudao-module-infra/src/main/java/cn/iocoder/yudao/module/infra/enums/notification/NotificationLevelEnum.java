package cn.iocoder.yudao.module.infra.enums.notification;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum NotificationLevelEnum implements ArrayValuable<Integer> {

    EMERGENCY(1, "紧急通知", "#ef4444", "全校停课、安全警报等紧急情况"),
    IMPORTANT(2, "重要通知", "#f97316", "缴费通知、奖助申请等重要事项"),
    REGULAR(3, "常规通知", "#3b82f6", "作业布置、课程调整等日常事务"),
    REMINDER(4, "提醒通知", "#8b5cf6", "上课提醒、作业截止等温馨提示");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NotificationLevelEnum::getLevel).toArray(Integer[]::new);

    private final Integer level;
    private final String name;
    private final String color;
    private final String description;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static NotificationLevelEnum valueOf(Integer level) {
        return Arrays.stream(values()).filter(notificationLevel -> notificationLevel.getLevel().equals(level)).findFirst().orElse(null);
    }

    public boolean isAutoConfirmRequired() {
        return this == EMERGENCY || this == IMPORTANT;
    }

    public boolean isImmediatePush() {
        return this == EMERGENCY;
    }

    public int getPriority() {
        return switch (this) {
            case EMERGENCY -> 1;
            case IMPORTANT -> 2;
            case REGULAR -> 3;
            case REMINDER -> 4;
        };
    }
}