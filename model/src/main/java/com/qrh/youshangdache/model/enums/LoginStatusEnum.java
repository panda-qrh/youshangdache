package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum LoginStatusEnum {
    SUCCESS(1, "登录成功"),
    FAIL(0, "登录失败");

    @EnumValue
    private int code;
    private String message;

    private LoginStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static LoginStatusEnum of(int code) {
        for (LoginStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的登录状态码: " + code);
    }
}
