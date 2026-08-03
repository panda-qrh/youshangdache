package com.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum LoginStatusEnum {
    SUCCESS(1, "登录成功"),
    FAIL(0, "登录失败");

    @EnumValue
    private int code;
    private String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static LoginStatusEnum fromCode(int code) {
        return of(code);
    }

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
