package com.youshangdache.driver.service;

import com.youshangdache.driver.mapper.DriverLoginLogMapper;
import com.youshangdache.model.entity.driver.DriverLoginLog;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 司机登录日志记录器
 *
 * <p>单独抽成一个 Spring Bean 的原因：{@code @Async} 依赖 Spring 代理生效，
 * 如果直接在 DriverInfoServiceImpl 内部用 this 调用自己的 {@code @Async} 方法，
 * 会绕过代理、退化成同步执行。放到独立 Bean 里由外部调用，异步才真正生效。</p>
 */
@Slf4j
@Component
public class DriverLoginLogRecorder {

    @Resource
    private DriverLoginLogMapper driverLoginLogMapper;

    /**
     * 异步记录司机登录日志
     *
     * @param loginLog 登录日志
     */
    @Async("loginLogExecutor")
    public void recordLoginLog(DriverLoginLog loginLog) {
        try {
            driverLoginLogMapper.insert(loginLog);
        } catch (Exception e) {
            //异步线程里的异常不会影响主流程，但必须记录，否则日志会静默丢失
            log.error("记录司机登录日志失败：{}", loginLog, e);
        }
    }
}
