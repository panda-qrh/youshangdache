package com.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单申述状态枚举类
 */
@Getter
@AllArgsConstructor
public enum OrderComplaintEnum {
    /**
     * 订单申述状态：0-未申诉，1-申诉中，2-申诉成功，3-申诉失败
     */
    NOT_COMPLAINT(1, "未申诉"),
    COMPLAINT_PROCESSING(2, "申诉中"),
    COMPLAINT_SUCCESS(3, "申诉成功"),
    COMPLAINT_FAIL(4, "申诉失败");
    /**
     * 订单申述状态代号
     */
    @EnumValue
    private final int code;
    /**
     * 订单申述状态代号对应的描述
     */
    private final String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static OrderComplaintEnum fromCode(int code) {
        return of(code);
    }

    public static OrderComplaintEnum of(int code) {
        for (OrderComplaintEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的订单申诉状态码: " + code);
    }
}
