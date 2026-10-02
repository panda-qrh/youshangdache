package com.youshangdache.driver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author QRH
 * @date 2024/8/24 18:12
 * @description 司机端并发计算账单用的线程池
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 结束代驾时会并发提交若干个远程调用任务（订单信息、轨迹、里程、计价、奖励、分账），
     * 原来的队列容量只有 3 且使用 AbortPolicy，在核数较少的机器上很容易直接抛
     * RejectedExecutionException。这里把队列调大，并改用 CallerRunsPolicy：
     * 线程池打满时由调用线程自己执行，既不丢任务也不会直接失败。
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = processors + 1;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize * 2,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(64),
                new NamedThreadFactory("driver-bill-calc-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        //允许核心线程超时回收，避免低峰期一直占用线程
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    /**
     * 带业务前缀的线程工厂，便于线上排查线程问题
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, prefix + counter.getAndIncrement());
        }
    }
}
