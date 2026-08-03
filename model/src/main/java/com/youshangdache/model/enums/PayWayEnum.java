package com.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 支付方式枚举
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum PayWayEnum {
    WECHAT(1, "微信"),
    ALIPAY(2, "支付宝");
    @EnumValue
    private int code;
    private String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static PayWayEnum fromCode(int code) {
        for (PayWayEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的支付方式码: " + code);
    }

    public static PayWayEnum of(int code) {
        for (PayWayEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }
}
