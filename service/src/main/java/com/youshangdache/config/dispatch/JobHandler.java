package com.youshangdache.config.dispatch;

import com.alibaba.nacos.common.utils.ExceptionUtil;
import com.youshangdache.mapper.dispatch.XxlJobLogMapper;
import com.youshangdache.service.dispatch.NewOrderService;
import com.youshangdache.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author QRH
 * @date 2024/7/27 21:43
 * @description 任务调度执行器，在这里编写定时任务
 */
@Component
@Slf4j
public class JobHandler {
    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private NewOrderService newOrderService;

    /**
     * 创建订单的调度任务
     */
    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {
        log.info("新订单调度任务：{}", XxlJobHelper.getJobId());

        //记录定时任务相关的日志信息
        //封装日志对象
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();
        try {
            //执行任务
            newOrderService.executeTask(XxlJobHelper.getJobId());

            xxlJobLog.setStatus(1);//成功
        } catch (Exception e) {
            xxlJobLog.setStatus(0);//失败
            xxlJobLog.setError(ExceptionUtil.getAllExceptionMsg(e));
            log.error("定时任务执行失败，任务id为：{}", XxlJobHelper.getJobId());
            e.printStackTrace();
        } finally {
            //耗时
            int times = (int) (System.currentTimeMillis() - startTime);
            xxlJobLog.setTimes(times);
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}
