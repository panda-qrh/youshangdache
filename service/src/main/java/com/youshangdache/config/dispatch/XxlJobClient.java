package com.youshangdache.config.dispatch;

import com.alibaba.fastjson.JSONObject;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.model.entity.dispatch.XxlJobInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
public class XxlJobClient {

    @Autowired
    private XxlJobClientConfig xxlJobClientConfig;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 添加调度任务
     *
     * @param executorHandler 执行器
     * @param param           执行参数
     * @param corn            cron表达式
     * @param desc            任务描述
     * @return 响应体中content字段的内容
     */
    @SneakyThrows
    public Long addJob(String executorHandler, String param, String corn, String desc) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        xxlJobInfo.setJobDesc(desc);
        xxlJobInfo.setAuthor("QRH");
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(corn);
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(executorHandler);
        xxlJobInfo.setExecutorParam(param);
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        ResponseEntity<JSONObject> response = restTemplate.postForEntity(
                xxlJobClientConfig.getAddUrl(),
                request,
                JSONObject.class);
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("增加xxl执行任务成功,返回信息:{}", response.getBody().toJSONString());
            //content为任务id
            return response.getBody().getLong("content");
        }
        log.info("调用xxl增加执行任务失败:{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    /**
     * 开始任务调度
     *
     * @param jobId 调度任务的jobId
     * @return true | false
     */
    public Boolean startJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getStartJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("任务开始执行:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        log.info("启动xxl执行任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    /**
     * 停止任务调度
     *
     * @param jobId 调度任务的jobId
     * @return true | false
     */
    public Boolean stopJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getStopJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("停止调度任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        log.info("停止调度任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    /**
     * 删除任务调度
     *
     * @param jobId 调度任务的jobId
     * @return true | false
     */
    public Boolean removeJob(Long jobId) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setId(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getRemoveUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("删除调度任务成功:{},返回信息:{}", jobId, response.getBody().toJSONString());
            return true;
        }
        log.info("删除调度任务失败:{},返回信息:{}", jobId, response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

    /**
     * 添加并启动调度任务
     *
     * @param executorHandler 执行器
     * @param param           执行参数
     * @param corn            cron表达式
     * @param desc            任务描述
     * @return 响应体中content字段的内容
     */
    public Long addAndStart(String executorHandler, String param, String corn, String desc) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobClientConfig.getJobGroupId());
        xxlJobInfo.setJobDesc(desc);
        xxlJobInfo.setAuthor("QRH");
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(corn);
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(executorHandler);
        xxlJobInfo.setExecutorParam(param);
        xxlJobInfo.setExecutorRouteStrategy("FIRST");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setMisfireStrategy("FIRE_ONCE_NOW");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<XxlJobInfo> request = new HttpEntity<>(xxlJobInfo, headers);

        String url = xxlJobClientConfig.getAddAndStartUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
        if (response.getStatusCode().value() == 200 && response.getBody().getIntValue("code") == 200) {
            log.info("增加并开始执行调度任务成功,返回信息:{}", response.getBody().toJSONString());
            //content为任务id
            return response.getBody().getLong("content");
        }
        log.info("增加并开始执行调度任务失败:{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.XXL_JOB_ERROR);
    }

}