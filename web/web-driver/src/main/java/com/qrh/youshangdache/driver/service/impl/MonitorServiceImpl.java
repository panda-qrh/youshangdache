package com.qrh.youshangdache.driver.service.impl;

import com.qrh.youshangdache.driver.feign.CiFeignClient;
import com.qrh.youshangdache.driver.service.FileService;
import com.qrh.youshangdache.driver.service.MonitorService;
import com.qrh.youshangdache.model.entity.order.OrderMonitor;
import com.qrh.youshangdache.model.entity.order.OrderMonitorRecord;
import com.qrh.youshangdache.model.form.order.OrderMonitorForm;
import com.qrh.youshangdache.model.vo.order.TextAuditingVo;
import com.qrh.youshangdache.order.feign.OrderMonitorFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorServiceImpl implements MonitorService {
    @Resource
    private FileService fileService;
    @Resource
    private OrderMonitorFeignClient orderMonitorFeignClient;
    @Resource
    private CiFeignClient ciFeignClient;

    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
        //上传对话文件
        String url = fileService.upload(file);
        log.info("upload: {}", url);

        //保存订单监控记录信息
        OrderMonitorRecord orderMonitorRecord = new OrderMonitorRecord();
        orderMonitorRecord.setOrderId(orderMonitorForm.getOrderId());
        orderMonitorRecord.setFileUrl(url);
        orderMonitorRecord.setContent(orderMonitorForm.getContent());
        //记录审核结果
        TextAuditingVo textAuditingVo = ciFeignClient.textAuditing(orderMonitorForm.getContent()).getData();
        orderMonitorRecord.setResult(textAuditingVo.getResult());
        orderMonitorRecord.setKeywords(textAuditingVo.getKeywords());
        orderMonitorFeignClient.saveMonitorRecord(orderMonitorRecord);

        //更新订单监控统计
        OrderMonitor orderMonitor = orderMonitorFeignClient.getOrderMonitor(orderMonitorForm.getOrderId()).getData();
        int fileNum = orderMonitor.getFileNum() + 1;
        orderMonitor.setFileNum(fileNum);
        //审核结果: 0（审核正常），1 （判定为违规敏感文件），2（疑似敏感，建议人工复核）。
        if("3".equals(orderMonitorRecord.getResult())) {
            int auditNum = orderMonitor.getAuditNum() + 1;
            orderMonitor.setAuditNum(auditNum);
        }
        orderMonitorFeignClient.updateOrderMonitor(orderMonitor);
        return true;
    }
}
