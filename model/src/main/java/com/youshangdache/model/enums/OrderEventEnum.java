package com.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单事件枚举类
 *
 * <p>状态机改造后，订单状态不再由各业务方法直接 setStatus 推进，
 * 而是统一发送"事件"、由状态机决定能否迁移。</p>
 *
 * <p>注意：对外的接口契约应该暴露"事件"（意图）而不是"目标状态"，
 * 客户端只能表达"我要抢单/我要取消"，不能指定"把订单改成 8"。</p>
 */
@Getter
@AllArgsConstructor
public enum OrderEventEnum {

    /**
     * 司机抢单：等待接单 -> 司机已接单
     */
    DRIVER_ACCEPT(1, "司机抢单"),
    /**
     * 司机到达上车点：司机已接单 -> 司机已到达
     */
    DRIVER_ARRIVE(2, "司机到达上车点"),
    /**
     * 录入代驾车辆信息：司机已到达 -> 更新代驾车辆信息
     */
    UPDATE_CAR(3, "录入代驾车辆信息"),
    /**
     * 开始代驾：司机已到达/更新代驾车辆信息 -> 开始代驾
     */
    START_DRIVE(4, "开始代驾"),
    /**
     * 结束代驾：开始代驾 -> 结束代驾
     */
    END_DRIVE(5, "结束代驾"),
    /**
     * 发送账单：结束代驾 -> 未付款
     */
    SEND_BILL(6, "发送账单"),
    /**
     * 支付成功：未付款 -> 已付款
     */
    PAY_SUCCESS(7, "支付成功"),
    /**
     * 订单完成：已付款 -> 订单已结束
     */
    FINISH_ORDER(8, "订单完成"),
    /**
     * 超时无人接单：等待接单 -> 没有司机接单，取消订单
     */
    CANCEL_TIMEOUT(9, "超时无人接单取消"),
    /**
     * 乘客取消：等待接单/司机已接单/司机已到达 -> 乘客撤单
     */
    CANCEL_BY_CUSTOMER(10, "乘客取消订单"),
    /**
     * 司机撤单：司机已接单/司机已到达 -> 司机撤单
     */
    CANCEL_BY_DRIVER(11, "司机撤单"),
    /**
     * 事故关闭：开始代驾 -> 订单因事故关闭
     */
    CLOSE_BY_CASE_ACCIDENT(12, "事故关闭订单");

    /**
     * 事件代号
     */
    @EnumValue
    private final int code;
    /**
     * 事件描述
     */
    private final String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static OrderEventEnum fromCode(int code) {
        return of(code);
    }

    public static OrderEventEnum of(int code) {
        for (OrderEventEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的订单事件码: " + code);
    }
}
