package com.youshangdache.mq.receiver;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ConfirmReceiver {

    /**
     * 确认交换机名称
     */
    public static final String EXCHANGE_CONFIRM = "exchange.confirm";
    /**
     * 死信交换机名称
     */
    public static final String EXCHANGE_DEAD = "exchange.dead";

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = EXCHANGE_CONFIRM),
            //给确认队列绑定死信交换机：消息被拒绝或过期后会进入 exchange.dead，
            //否则 exchange.dead/queue.dead.2 只是一组永远不会收到消息的空声明。
            value = @Queue(value = "queue.confirm", durable = "true", arguments = {
                    @Argument(name = "x-dead-letter-exchange", value = EXCHANGE_DEAD),
                    @Argument(name = "x-dead-letter-routing-key", value = "routing.dead.2")
            }),
            key = "routing.confirm"))
    public void process(Message message, Channel channel) {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 监听死信队列
     *
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.dead.2", durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = EXCHANGE_DEAD),
            key = "routing.dead.2"
    ))
    public void getDeadLetterMsg(String msg, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.warn("收到死信消息，需要人工介入处理：{}", msg);
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("【消息模块】处理死信消息异常：{}", msg, e);
        }
    }

}