package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 司机和用户认证状态枚举类
 */
@Getter
@AllArgsConstructor
public enum AuthStatusEnum {
    UNAUTHORIZED(0, "未认证"),
    REVIEWING(1, "审核中"),
    AUTHENTICATION_PASSED(2, "认证通过"),
    AUTHENTICATION_FAILED(-1, "认证未通过");
    /**
     * 状态代号
     */
    @EnumValue
    private final Integer code;
    /**
     * 状态代号对应的描述
     */
    private final String message;

    /**
     * 根据code匹配枚举
     * @param code 状态码
     * @return 枚举
     */
    public static AuthStatusEnum of(int code) {
        for (AuthStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的认证状态码: " + code);
    }
}
