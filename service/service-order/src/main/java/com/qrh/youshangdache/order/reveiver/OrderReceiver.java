package com.qrh.youshangdache.order.reveiver;

import com.qrh.youshangdache.common.constant.ExchangeConst;
import com.qrh.youshangdache.common.constant.QueueConst;
import com.qrh.youshangdache.common.constant.RoutingConst;
import com.qrh.youshangdache.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author QRH
 * @date 2025/3/3 16:53
 * @description 与订单有关的消息队列监听器
 */
@Slf4j
@Component
public class OrderReceiver {

    @Resource
    private OrderInfoService orderInfoService;

    /**
     * 系统取消订单
     *
     * @param orderId 订单id
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = ExchangeConst.CANCEL_ORDER, type = "x-delayed-message", arguments = @Argument(name = "x-delayed-type", value="direct"),durable = "true", autoDelete = "false"),
            value = @Queue(value = QueueConst.CANCEL_ORDER, durable = "true"),
            key = {RoutingConst.CANCEL_ORDER}
    ))
    public void systemCancelOrder(String orderId, Message message, Channel channel) throws IOException {
        try {
            //1.处理业务
            if (orderId != null) {
                log.info("【订单微服务模块】关闭订单消息：{}", orderId);
                orderInfoService.systemCancelOrder(Long.parseLong(orderId));
            }
            //2.手动应答
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("【订单微服务模块】关闭订单业务异常：{}", e);
        }
    }

    /**
     * 订单分账成功，更新分账状态
     *
     * @param orderNo
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = QueueConst.PROFITSHARING_SUCCESS, durable = "true"),
            exchange = @Exchange(value = ExchangeConst.ORDER),
            key = {RoutingConst.PROFITSHARING_SUCCESS}
    ))
    public void profitsharingSuccess(String orderNo, Message message, Channel channel) throws IOException {
        orderInfoService.updateProfitsharingStatus(orderNo);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}