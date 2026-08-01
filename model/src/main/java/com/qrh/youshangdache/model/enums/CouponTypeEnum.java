package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券类型 枚举类
 */
@Getter
@AllArgsConstructor
public enum CouponTypeEnum {
    CASH(1, "现金券"),
    DISCOUNT(2, "折扣券");

    /**
     * 优惠券类型代号
     */
    @EnumValue
    private final Integer code;

    /**
     * 优惠券类型的描述（值）
     */
    private final String type;

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static CouponTypeEnum fromCode(int code) {
        return of(code);
    }

    public static CouponTypeEnum of(int code) {
        for (CouponTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的优惠券类型码: " + code);
    }
}
