package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券 发布状态 枚举类
 */
@Getter
@AllArgsConstructor
public enum CouponPublishStatusEnum {
    NOT_PUBLISH(0, "未发布"),
    PUBLISHED(1, "已发布"),
    COUPON_EXPIRED(-1, "已过期");

    /**
     * 优惠券发布状态的代号
     */
    @EnumValue
    private final int code;
    /**
     * 优惠券发布的状态描述（值）
     */
    private final String message;

    public static CouponPublishStatusEnum of(int code) {
        for (CouponPublishStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的优惠券发布状态码: " + code);
    }
}
