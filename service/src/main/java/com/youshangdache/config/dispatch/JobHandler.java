package com.youshangdache.config.dispatch;

import com.youshangdache.mapper.dispatch.XxlJobLogMapper;
import com.youshangdache.service.dispatch.NewOrderService;
import com.youshangdache.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

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

    private String getExceptionMsg(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {
        log.info("新订单调度任务：{}", XxlJobHelper.getJobId());

        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();
        try {
            newOrderService.executeTask(XxlJobHelper.getJobId());
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(getExceptionMsg(e));
            log.error("定时任务执行失败，任务id为：{}", XxlJobHelper.getJobId());
            e.printStackTrace();
        } finally {
            int times = (int) (System.currentTimeMillis() - startTime);
            xxlJobLog.setTimes(times);
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}
