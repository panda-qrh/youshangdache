package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 司机服务状态枚举类
 */
@Getter
@AllArgsConstructor
public enum DriverServiceStatusEnum {
    /**
     * 司机未接单
     */
    DRIVER_NOT_SERVICE(0, "未接单"),
    /**
     * 司机开始接单
     */
    DRIVER_START_SERVICE(1, "开始接单");

    /**
     * 司机服务状态代号
     */
    @EnumValue
    private final int code;
    /**
     * 司机服务状态代号对应的描述
     */
    private final String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static DriverServiceStatusEnum fromCode(int code) {
        return of(code);
    }

    /**
     * 根据code获取枚举
     * @param code 状态码
     * @return 枚举值
     */
    public static DriverServiceStatusEnum of(int code) {
        for (DriverServiceStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的司机服务状态码: " + code);
    }

}
