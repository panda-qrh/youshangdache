package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券 使用门槛 枚举类
 */
@Getter
@AllArgsConstructor
public enum CouponUsageThresholdEnum {
    NO_THRESHOLD(0, "无门槛");

    /**
     * 使用门槛代号
     */
    @EnumValue
    private final int code;
    /**
     * 使用门槛对应的描述（值）
     */
    private final String type;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static CouponUsageThresholdEnum fromCode(int code) {
        return of(code);
    }

    public static CouponUsageThresholdEnum of(int code) {
        for (CouponUsageThresholdEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的使用门槛码: " + code);
    }
}
