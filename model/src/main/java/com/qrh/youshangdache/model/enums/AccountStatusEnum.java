package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AccountStatusEnum {

    NORMAL(1, "正常"),
    DISABLED(2, "禁用"),
    FREEZE(3, "冻结"),
    DEACTIVATE(4, "注销");
    @EnumValue
    private int code;
    private String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static AccountStatusEnum fromCode(int code) {
        return of(code);
    }

    AccountStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static AccountStatusEnum of(int code) {
        for (AccountStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的账号状态码: " + code);
    }
}
