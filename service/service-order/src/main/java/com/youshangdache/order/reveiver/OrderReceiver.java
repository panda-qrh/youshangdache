package com.youshangdache.order.reveiver;

import com.youshangdache.common.mq.constant.ExchangeConst;
import com.youshangdache.common.mq.constant.QueueConst;
import com.youshangdache.common.mq.constant.RoutingConst;
import com.youshangdache.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
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
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            //1.处理业务
            if (orderId != null) {
                log.info("【订单微服务模块】关闭订单消息：{}", orderId);
                orderInfoService.systemCancelOrder(Long.parseLong(orderId));
            }
            //2.手动应答
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            //业务异常不能让消息悬空：既不 ack 也不 nack 会导致消息在 channel 关闭后被重复投递且无法控制。
            //取消订单本身是幂等的（内部会先判断状态），因此这里直接确认掉并记录日志，避免无效重投。
            log.error("【订单微服务模块】关闭订单业务异常，orderId={}", orderId, e);
            channel.basicAck(deliveryTag, false);
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
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            orderInfoService.updateProfitsharingStatus(orderNo);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            //更新分账状态是幂等操作（按 orderNo 更新为已分账），失败直接确认并记录，避免无限重回队列
            log.error("【订单微服务模块】更新分账状态异常，orderNo={}", orderNo, e);
            channel.basicAck(deliveryTag, false);
        }
    }

}