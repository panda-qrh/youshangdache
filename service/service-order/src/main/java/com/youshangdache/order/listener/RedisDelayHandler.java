package com.youshangdache.order.listener;

import com.youshangdache.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author QRH
 * @date 2024/8/25 19:36
 * @description Redisson延迟队列处理器类
 */
@Component
public class RedisDelayHandler {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OrderInfoService orderInfoService;

    /**
     * 利用redissonClient 发送延迟消息。<br>
     * 创还能一个线程，然后循环从队列中获取数据，然后调用取消订单方法。<br>
     * 步骤：
     * <pre>{@code
     * redissonClient.getBlockingDeque(): 创建一个阻塞队列
     * redissonClient.getDelayedQueue(): 获取延迟队列
     * delayedQueue.offer(): 向队列中存储数据
     * blockingDeque.take(): 从队列中获取数据
     * }</pre>
     */
    @PostConstruct
    public void listener() {
        new Thread(() -> {
            while (true) {
                //获取延迟队列里面的阻塞队列
                RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");
                //从队列获取消息
                try {
                    String orderId = blockingQueue.take();
                    //取消订单
                    if (StringUtils.hasText(orderId)) {
                        //掉用取消订单方法
                        orderInfoService.orderCancel(Long.parseLong(orderId));
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


}
