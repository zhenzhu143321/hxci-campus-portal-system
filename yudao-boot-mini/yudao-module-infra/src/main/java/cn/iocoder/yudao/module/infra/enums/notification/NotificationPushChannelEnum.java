package cn.iocoder.yudao.module.infra.enums.notification;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum NotificationPushChannelEnum implements ArrayValuable<Integer> {

    APP(1, "APP推送", "app"),
    SMS(2, "短信推送", "sms"),
    EMAIL(3, "邮件推送", "email"),
    WECHAT(4, "微信推送", "wechat"),
    SYSTEM(5, "站内信", "system");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NotificationPushChannelEnum::getChannel).toArray(Integer[]::new);

    private final Integer channel;
    private final String name;
    private final String code;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static NotificationPushChannelEnum valueOf(Integer channel) {
        return Arrays.stream(values()).filter(pushChannel -> pushChannel.getChannel().equals(channel)).findFirst().orElse(null);
    }

    public static NotificationPushChannelEnum valueOfByCode(String code) {
        return Arrays.stream(values()).filter(pushChannel -> pushChannel.getCode().equals(code)).findFirst().orElse(null);
    }

    public boolean isRealTimePush() {
        return this == APP || this == SYSTEM;
    }

    public boolean requiresExternalService() {
        return this == SMS || this == EMAIL || this == WECHAT;
    }
}