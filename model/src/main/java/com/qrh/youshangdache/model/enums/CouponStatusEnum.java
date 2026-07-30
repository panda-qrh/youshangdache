package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券状态 枚举类
 */
@Getter
@AllArgsConstructor
public enum CouponStatusEnum {
    NOT_USED(1, "未使用"),
    USED(2, "已使用");

    /**
     * 优惠券状态代号
     */
    @EnumValue
    private final int code;
    /**
     * 状态描述（值）
     */
    private final String message;

    public static CouponStatusEnum of(int code) {
        for (CouponStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的优惠券状态码: " + code);
    }
}
