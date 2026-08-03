package com.qrh.youshangdache.driver.client;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.vo.order.TextAuditingVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-driver")
public interface CiFeignClient {

    @PostMapping("/textAuditing")
    public Result<TextAuditingVo> textAuditing(@RequestBody String content);
}