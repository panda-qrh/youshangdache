package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作状态枚举类
 */
@Getter
@AllArgsConstructor
public enum OperationStatusEnum {
    NORMAL(0,"操作正常"),
    EXCEPTION(1,"操作异常");

    @EnumValue
    private final int code;
    private final String message;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static OperationStatusEnum fromCode(int code) {
        return of(code);
    }

    public static OperationStatusEnum of(int code) {
        for (OperationStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的操作状态码: " + code);
    }
}
