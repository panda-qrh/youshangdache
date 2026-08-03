package com.qrh.youshangdache.common.mq.service;


import com.alibaba.fastjson2.JSON;
import com.qrh.youshangdache.common.mq.entity.GuiguCorrelationData;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送消息
     *
     * @param exchange   交换机名
     * @param routingKey 路由键
     * @param message    消息内容
     */
    public void sendMessage(String exchange, String routingKey, Object message) {
        //1.创建自定义相关消息对象-包含业务数据本身，交换器名称，路由键，队列类型，延迟时间,重试次数
        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        GuiguCorrelationData correlationData = GuiguCorrelationData.builder()
                .id(uuid)
                .message(message)
                .exchange(exchange)
                .routingKey(routingKey)
                .build();

        //2.将相关消息封装到发送消息方法中
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);

        //3.将相关消息存入Redis  Key：UUID  相关消息对象  10 分钟
        stringRedisTemplate.opsForValue().set(uuid, JSON.toJSONString(correlationData), 10, TimeUnit.MINUTES);

        //log.info("生产者发送消息成功：{}，{}，{}", exchange, routingKey, message);
    }

    /**
     * 发送<strong>延迟</strong>消息方法
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息数据
     * @param delayTime  延迟时间，单位为：秒
     */
    public void sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        //1.创建自定义相关消息对象-包含业务数据本身，交换器名称，路由键，队列类型，延迟时间,重试次数
        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        GuiguCorrelationData correlationData = GuiguCorrelationData.builder()
                .id(uuid)
                .message(message)
                .exchange(exchange)
                .routingKey(routingKey)
                .delay(true) //开启延迟消息
                .delayTime(delayTime)
                .build();

        //2.将相关消息封装到发送消息方法中，并通过消息后置处理器设置消息的延迟时间
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message,
                messagePostProcessor -> {
                    messagePostProcessor.getMessageProperties().setDelay(delayTime * 1000);
                    return messagePostProcessor;
                },
                correlationData
        );

        //3.将相关消息存入Redis  Key：UUID  相关消息对象  10 分钟
        stringRedisTemplate.opsForValue().set(uuid, JSON.toJSONString(correlationData), 10, TimeUnit.MINUTES);
    }

}
