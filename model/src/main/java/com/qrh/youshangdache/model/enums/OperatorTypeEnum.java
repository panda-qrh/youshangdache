package com.qrh.youshangdache.model.enums;


import lombok.Getter;

/**
 * 操作人类别枚举类
 */
@Getter
public enum OperatorTypeEnum {
    /**
     * 其它
     */
    OTHER,

    /**
     * 后台用户
     */
    MANAGEMENT,

    /**
     * 手机端用户
     */
    MOBILE_PHONE_USER
}
