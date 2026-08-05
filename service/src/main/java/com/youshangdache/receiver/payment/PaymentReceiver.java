package com.youshangdache.receiver.payment;

import com.alibaba.fastjson2.JSONObject;
import com.youshangdache.common.mq.constant.ExchangeConst;
import com.youshangdache.common.mq.constant.QueueConst;
import com.youshangdache.common.mq.constant.RoutingConst;
import com.youshangdache.model.form.payment.ProfitsharingForm;
import com.youshangdache.service.payment.WxPayService;
import com.youshangdache.service.payment.WxProfitsharingService;
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
    @Resource
    private WxPayService wxPayService;


    @Resource
    private WxProfitsharingService wxProfitsharingService;

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = ExchangeConst.ORDER),
            value = @Queue(value = QueueConst.PAY_SUCCESS, durable = "ture"),
            key = {RoutingConst.PAY_SUCCESS}
    ))
    public void paySuccess(String orderNo, Message message, Channel channel) throws IOException {
        wxPayService.handlerOrder(orderNo);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
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
        try {
            ProfitsharingForm profitsharingForm = JSONObject.parseObject(param, ProfitsharingForm.class);
            log.info("分账：{}", param);
            wxProfitsharingService.profitsharing(profitsharingForm);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.info("分账调用失败：{}", e.getMessage());
            //任务执行失败，就退回队列继续执行，优化：设置退回次数，超过次数记录日志
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }


}
