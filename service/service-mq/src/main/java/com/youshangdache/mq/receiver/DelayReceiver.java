package com.youshangdache.mq.receiver;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DelayReceiver {

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 监听到延迟消息
     *
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.delay.1", durable = "true", autoDelete = "false"),
            //交换机类型必须与 DelayedMqConfig 中声明的 x-delayed-type=direct 保持一致，
            //否则同一个交换机两处声明参数冲突，会触发 406 PRECONDITION_FAILED。
            exchange = @Exchange(value = "exchange.delay", type = ExchangeTypes.DIRECT, delayed = "true"),
            key = "routing.delay"
    )
    )
    public void getDelayMsg(String msg, Message message, Channel channel) {
        String key = "mq:" + msg;
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            //如果业务保证幂等性，基于redis setnx保证
            Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "", 2, TimeUnit.SECONDS);
            if (!flag) {
                //说明该业务数据已经被执行过，直接确认丢弃
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (StringUtils.isNotBlank(msg)) {
                log.info("延迟插件监听消息：{}", msg);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            //处理失败时删除幂等键，让这条消息可以被重投；同时必须显式确认或拒绝，
            //否则消息会一直处于未确认状态。
            log.error("延迟消息处理异常，msg={}", msg, e);
            try {
                redisTemplate.delete(key);
                //requeue=true，交给 broker 重新投递
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("延迟消息 nack 失败，msg={}", msg, ex);
            }
        }
    }
}