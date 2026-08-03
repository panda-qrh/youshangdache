package com.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分账状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProfitsharingStatusEnum {

    NOT_SHARED(0, "未分账"),
    SHARED(1, "已分账");

    @EnumValue
    private final Integer code;

    private final String message;

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static ProfitsharingStatusEnum fromCode(int code) {
        return of(code);
    }

    public static ProfitsharingStatusEnum of(int code) {
        for (ProfitsharingStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的分账状态码: " + code);
    }
}
