package com.youshangdache.order.listener;

import com.youshangdache.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author QRH
 * @date 2024/8/25 19:36
 * @description Redisson延迟队列处理器类
 */
@Slf4j
@Component
public class RedisDelayHandler {

    /**
     * 延迟取消订单队列名称
     */
    public static final String QUEUE_CANCEL = "queue_cancel";

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OrderInfoService orderInfoService;

    /**
     * 消费线程的运行标志，用于优雅停机
     */
    private volatile boolean running = true;

    private Thread consumer;

    /**
     * 启动一个消费线程，循环从 Redisson 延迟队列中取出到期的订单并取消。
     *
     * <p>相比原来的实现做了三点修正：</p>
     * <ol>
     *     <li>队列对象只创建一次，不再每轮循环都重新 getBlockingQueue；</li>
     *     <li>用 try/catch 包住每一轮的业务处理，业务异常不会把消费线程直接打死；</li>
     *     <li>线程命名 + {@link PreDestroy} 停机，避免应用关闭时线程泄漏。</li>
     * </ol>
     */
    @PostConstruct
    public void listener() {
        consumer = new Thread(() -> {
            RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(QUEUE_CANCEL);
            while (running) {
                try {
                    String orderId = blockingQueue.take();
                    if (StringUtils.hasText(orderId)) {
                        try {
                            orderInfoService.orderCancel(Long.parseLong(orderId));
                        } catch (Exception e) {
                            //取消订单是幂等的，单条处理失败只记录日志，不能中断整个消费循环
                            log.error("延迟队列取消订单失败，orderId={}", orderId, e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延迟队列消费线程被中断，准备退出");
                    return;
                } catch (Exception e) {
                    log.error("延迟队列消费异常", e);
                }
            }
        }, "redis-delay-cancel-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    /**
     * 应用关闭时停止消费线程
     */
    @PreDestroy
    public void shutdown() {
        running = false;
        if (consumer != null) {
            consumer.interrupt();
        }
    }
}
