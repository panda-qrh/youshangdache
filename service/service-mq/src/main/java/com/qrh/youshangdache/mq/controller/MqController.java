package com.qrh.youshangdache.mq.controller;

import com.qrh.youshangdache.common.mq.service.RabbitService;
import com.qrh.youshangdache.mq.config.DeadLetterMqConfig;
import com.qrh.youshangdache.mq.config.DelayedMqConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mq")
public class MqController {


    @Resource
    private RabbitService rabbitService;


    /**
     * 消息发送
     */
    @GetMapping("/sendConfirm")
    public void sendConfirm() {
        rabbitService.sendMessage("exchange.confirm", "routing.confirm", "来人了，开始接客吧！");
    }

    /**
     * 消息发送延迟消息：基于延迟插件使用
     */
    @GetMapping("/sendDelayMsg")
    public void sendDelayMsg() {
        //调用工具方法发送延迟消息
        int delayTime = 10;
        rabbitService.sendDelayMessage(DelayedMqConfig.EXCHANGE_DELAY, DelayedMqConfig.ROUTING_DELAY, "我是延迟消息", delayTime);
        log.info("基于延迟插件-发送延迟消息成功");
    }

}