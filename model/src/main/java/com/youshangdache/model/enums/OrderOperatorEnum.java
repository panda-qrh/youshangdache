package com.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态变更的操作人类型
 *
 * <p>用于订单状态流转审计日志：一次迁移到底是谁触发的。</p>
 */
@Getter
@AllArgsConstructor
public enum OrderOperatorEnum {

    /**
     * 乘客（小程序）
     */
    CUSTOMER(1, "乘客"),
    /**
     * 司机（小程序）
     */
    DRIVER(2, "司机"),
    /**
     * 系统（定时任务、消息队列、支付回调）
     */
    SYSTEM(3, "系统"),
    /**
     * 后台管理员
     */
    ADMIN(4, "后台管理员");

    @EnumValue
    private final int code;
    private final String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static OrderOperatorEnum fromCode(int code) {
        return of(code);
    }

    public static OrderOperatorEnum of(int code) {
        for (OrderOperatorEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的操作人类型码: " + code);
    }
}
