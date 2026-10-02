package com.youshangdache.common.mq.listener;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.youshangdache.common.mq.entity.GuiguCorrelationData;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RabbitInitConfigApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * 消息投递失败的最大重发次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        this.setupCallbacks();
    }

    private void setupCallbacks() {

        /**
         * 只确认消息是否正确到达 Exchanger 中,成功与否都会回调
         *
         * @param correlation 相关数据  非消息本身业务数据
         * @param ack             应答结果
         * @param reason           如果发送消息到交换器失败，错误原因
         */
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, reason) -> {
            if (ack) {
                //消息到交换器成功
                log.info("消息发送到Exchange成功：{}", correlationData);
            } else {
                //消息到交换器失败
                log.error("消息发送到Exchange失败：{}", reason);

                //执行消息重发
                this.retrySendMsg(correlationData);
            }
        });

        /**
         * 消息没有正确到达队列时触发回调，如果正确到达队列不执行
         */
        this.rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Returned: {}\n replyCode: {}\n replyText: {}\n exchange/rk: {}/{}",
                    returned.getMessage(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());

            //当路由队列失败 也需要重发
            //1.构建相关数据对象
            Object correlationId = returned.getMessage().getMessageProperties().getHeader("spring_returned_message_correlation");
            if (correlationId == null) {
                log.error("路由失败的消息缺少 correlationId，无法重发");
                return;
            }
            String correlationDataStr = stringRedisTemplate.opsForValue().get(correlationId.toString());
            if (!StringUtils.hasText(correlationDataStr)) {
                //Redis 中的关联数据已过期（默认 10 分钟），此时无法重发，只能告警
                log.error("路由失败的消息在 Redis 中找不到关联数据（可能已过期），correlationId={}", correlationId);
                return;
            }
            GuiguCorrelationData guiguCorrelationData = JSON.parseObject(correlationDataStr, GuiguCorrelationData.class);
            //延迟消息同样需要重发：retrySendMsg 内部会重新设置延迟时间
            //2.调用消息重发方法
            this.retrySendMsg(guiguCorrelationData);
        });
    }

    /**
     * 消息重新发送
     *
     * @param correlationData
     */
    private void retrySendMsg(CorrelationData correlationData) {
        //获取相关数据
        GuiguCorrelationData guiguCorrelationData = (GuiguCorrelationData) correlationData;

        //获取redis中存放重试次数
        //先重发，在写会到redis中次数
        int retryCount = guiguCorrelationData.getRetryCount();
        if (retryCount >= MAX_RETRY_COUNT) {
            //超过最大重试次数：这里只做日志与告警，失败消息的落库/人工处理需要接入告警系统后补充
            log.error("生产者重试已达上限({}次)，消息最终投递失败，需人工介入。exchange={}, routingKey={}, message={}",
                    MAX_RETRY_COUNT, guiguCorrelationData.getExchange(), guiguCorrelationData.getRoutingKey(), guiguCorrelationData.getMessage());
            return;
        }
        //重发次数+1
        retryCount += 1;
        guiguCorrelationData.setRetryCount(retryCount);
        stringRedisTemplate.opsForValue().set(guiguCorrelationData.getId(), JSON.toJSONString(guiguCorrelationData), 10, TimeUnit.MINUTES);
        log.info("进行消息重发！");
        //重发消息
        //todo 方式二：如果是延迟消息，依然需要设置消息延迟时间
        if (guiguCorrelationData.isDelay()) {
            //延迟消息
            rabbitTemplate.convertAndSend(guiguCorrelationData.getExchange(),
                    guiguCorrelationData.getRoutingKey(),
                    guiguCorrelationData.getMessage(),
                    message -> {
                        message.getMessageProperties().setDelay(guiguCorrelationData.getDelayTime() * 1000);
                        return message;
                    },
                    guiguCorrelationData
            );
        } else {
            //普通消息
            rabbitTemplate.convertAndSend(guiguCorrelationData.getExchange(), guiguCorrelationData.getRoutingKey(), guiguCorrelationData.getMessage(), guiguCorrelationData);
        }
    }


}
