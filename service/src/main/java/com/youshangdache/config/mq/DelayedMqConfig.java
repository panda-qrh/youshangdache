package com.youshangdache.config.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DelayedMqConfig {

    public static final String EXCHANGE_DELAY = "exchange.delay";
    public static final String ROUTING_DELAY = "routing.delay";
    public static final String QUEUE_DELAY_1 = "queue.delay.1";

    @Bean
    public Queue delayQueue1() {
        return new Queue(QUEUE_DELAY_1, true);
    }

    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_DELAY, "x-delayed-message", true, false, args);
    }

    @Bean
    public Binding delayBinding1() {
        return BindingBuilder.bind(delayQueue1()).to(delayExchange()).with(ROUTING_DELAY).noargs();
    }
}
