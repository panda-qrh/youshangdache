package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
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
}
