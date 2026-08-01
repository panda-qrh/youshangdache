package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
//    1等待接单，2已接单，3司机已到达，4开始代驾，5结束代驾，6未付款，7已付款，8订单已结束，9顾客撤单，10司机撤单，11事故关闭，12其他
    WAITING_ACCEPT(1, "等待接单"),
    ACCEPTED(2, "司机已接单"),
    DRIVER_ARRIVED(3, "司机已到达上车位置"),
    START_SERVICE(4, "开始代驾"),
    END_SERVICE(5, "结束代驾"),
    ORDER_UNPAID(6, "未付款"),
    ORDER_PAID(7, "已付款"),
    ORDER_FINISHED(8, "订单已结束"),
    ORDER_CANCELED_BY_USER(9, "乘客撤单"),
    ORDER_CANCELED_BY_DRIVER(10, "司机撤单"),
    ORDER_CLOSED_CASE_ACCIDENT(11, "订单因事故关闭"),
    UPDATE_CAR_INFO(12, "更新代驾车辆信息"),
    ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER(-1, "没有司机接单，取消订单"),
    ORDER_NOT_EXIST(-100, "订单不存在");
    /**
     *
     * 订单状态
     */
    @EnumValue
    private final int code;
    /**
     * 订单状态描述
     */
    private final String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static OrderStatusEnum fromCode(int code) {
        return of(code);
    }

    public static OrderStatusEnum of(int code) {
        for (OrderStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的订单状态码: " + code);
    }
}
