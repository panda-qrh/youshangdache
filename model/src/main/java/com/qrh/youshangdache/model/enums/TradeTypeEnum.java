package com.qrh.youshangdache.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TradeTypeEnum {
    REWARD(1, "系统奖励"),
    RECHARGE(1201, "充值"),
    LOCK(1202, "锁定"),
    UNLOCK(1203, "解锁"),
    CONSUME(1204, "消费");

    @EnumValue
    private final int code;
    private final String message;

    public static TradeTypeEnum of(int code) {
        for (TradeTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("无效的交易类型码: " + code);
    }
}
