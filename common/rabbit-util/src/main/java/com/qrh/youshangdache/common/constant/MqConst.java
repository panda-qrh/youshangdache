package com.qrh.youshangdache.common.constant;

/**
 * RabbitMQ 相关常量（已拆分到 {@link ExchangeConst}、{@link RoutingConst}、{@link QueueConst}）
 * @deprecated 请使用对应的独立接口
 */
@Deprecated
public interface MqConst extends ExchangeConst, RoutingConst, QueueConst {
}
