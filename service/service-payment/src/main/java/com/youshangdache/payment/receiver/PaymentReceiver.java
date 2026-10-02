package com.youshangdache.payment.receiver;

import com.alibaba.fastjson2.JSONObject;
import com.youshangdache.common.mq.constant.ExchangeConst;
import com.youshangdache.common.mq.constant.QueueConst;
import com.youshangdache.common.mq.constant.RoutingConst;
import com.youshangdache.model.form.payment.ProfitsharingForm;
import com.youshangdache.payment.service.WxPayService;
import com.youshangdache.payment.service.WxProfitsharingService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author QRH
 * @date 2024/8/25 16:09
 * @description 与支付有关的消息队列监听器
 */
@Component
@Slf4j
public class PaymentReceiver {

    /**
     * 分账消息的最大重投次数，超过后不再重回队列，避免永久性失败的消息无限循环
     */
    private static final int MAX_RETRY_COUNT = 3;

    @Resource
    private WxPayService wxPayService;


    @Resource
    private WxProfitsharingService wxProfitsharingService;

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = ExchangeConst.ORDER),
            //durable 原来拼写成了 "ture"，导致队列被声明为非持久化
            value = @Queue(value = QueueConst.PAY_SUCCESS, durable = "true"),
            key = {RoutingConst.PAY_SUCCESS}
    ))
    public void paySuccess(String orderNo, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            wxPayService.handlerOrder(orderNo);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            //支付成功后的处理（更新订单状态、奖励入账、发起分账）失败时需要重投，
            //但必须限制次数，否则会无限循环；超过次数后确认掉并记录，交由人工补偿。
            int retryCount = getRetryCount(message);
            if (retryCount < MAX_RETRY_COUNT) {
                log.warn("支付成功消息处理失败，第 {} 次重投，orderNo={}", retryCount + 1, orderNo, e);
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("支付成功消息处理失败已达最大重试次数({})，不再重投，orderNo={}", MAX_RETRY_COUNT, orderNo, e);
                channel.basicAck(deliveryTag, false);
            }
        }
    }


    /**
     * 分账消息监听方法，采用延时队列实现
     *
     * @param param
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = ExchangeConst.PROFITSHARING, type = "x-delayed-message", arguments = {@Argument(name = "x-delayed-type", value = "direct")}, durable = "true", autoDelete = "false"),
            value = @Queue(value = QueueConst.PROFITSHARING, durable = "true"),
            key = RoutingConst.PROFITSHARING

    ))
    public void profitsharingMessage(String param, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            ProfitsharingForm profitsharingForm = JSONObject.parseObject(param, ProfitsharingForm.class);
            log.info("分账：{}", param);
            wxProfitsharingService.profitsharing(profitsharingForm);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            //任务执行失败，退回队列继续执行；但必须设置退回次数上限，
            //否则永久性失败（例如参数非法）的消息会无限重回队列、把日志和 CPU 打满。
            int retryCount = getRetryCount(message);
            if (retryCount < MAX_RETRY_COUNT) {
                log.warn("分账调用失败，第 {} 次重投，param={}", retryCount + 1, param, e);
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("分账调用失败已达最大重试次数({})，不再重投，param={}", MAX_RETRY_COUNT, param, e);
                channel.basicAck(deliveryTag, false);
            }
        }
    }

    /**
     * 获取当前消息的重投次数
     *
     * <p>优先取 Redis 中的重试计数（由 RabbitService 维护），
     * 取不到时退化为读取消息头里的 x-death 次数。</p>
     *
     * @param message 消息
     * @return 已重试次数
     */
    private int getRetryCount(Message message) {
        Object death = message.getMessageProperties().getHeaders().get("x-death");
        if (death instanceof java.util.List<?> deathList && !deathList.isEmpty()
                && deathList.get(0) instanceof java.util.Map<?, ?> deathMap) {
            Object count = deathMap.get("count");
            if (count instanceof Number number) {
                return number.intValue();
            }
        }
        return 0;
    }
}
