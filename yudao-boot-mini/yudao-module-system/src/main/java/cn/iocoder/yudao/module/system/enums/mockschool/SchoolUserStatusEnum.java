package cn.iocoder.yudao.module.system.enums.mockschool;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Mock School API 用户状态枚举
 * 
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum SchoolUserStatusEnum {

    ENABLE(1, "正常"),
    DISABLE(2, "禁用"),
    LOCKED(3, "锁定");
    
    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名称
     */
    private final String name;

}