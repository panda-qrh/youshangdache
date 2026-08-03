package com.youshangdache.common.mq.entity;

import lombok.Data;
import lombok.ToString;
import org.springframework.amqp.rabbit.connection.CorrelationData;

@Data
@ToString
public class GuiguCorrelationData extends CorrelationData {

    //消息体
    private Object message;
    //交换机
    private String exchange;
    //路由键
    private String routingKey;
    //重试次数
    private int retryCount = 0;
    //是否延迟消息
    private boolean isDelay = false;
    //延迟时长
    private int delayTime = 10;

    private GuiguCorrelationData(String id, Object message, String routingKey, int retryCount, boolean isDelay, int delayTime) {
        super.setId(id);
        this.message = message;
        this.routingKey = routingKey;
        this.retryCount = retryCount;
        this.isDelay = isDelay;
        this.delayTime = delayTime;
    }

    public static GuiguCorrelationDataBuilder builder() {
        return new GuiguCorrelationDataBuilder();
    }

    public static class GuiguCorrelationDataBuilder {
        /**
         * CorrelationData的id
         */
        private String id;
        private Object message;
        private String exchange;
        private String routingKey;
        private int retryCount = 0;
        private boolean isDelay = false;
        private int delayTime = 10;

        public GuiguCorrelationDataBuilder() {
        }

        public GuiguCorrelationDataBuilder id(String id) {
            this.id = id;
            return this;
        }

        public GuiguCorrelationDataBuilder message(Object message) {
            this.message = message;
            return this;
        }

        public GuiguCorrelationDataBuilder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }

        public GuiguCorrelationDataBuilder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        public GuiguCorrelationDataBuilder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public GuiguCorrelationDataBuilder delay(boolean delay) {
            this.isDelay = delay;
            return this;
        }

        public GuiguCorrelationDataBuilder delayTime(int delayTime) {
            this.delayTime = delayTime;
            return this;
        }

        public GuiguCorrelationData build() {
            return new GuiguCorrelationData(id, message, routingKey, retryCount, isDelay, delayTime);
        }
    }
}
