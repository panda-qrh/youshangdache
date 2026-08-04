package com.youshangdache.driver;

import com.youshangdache.model.vo.order.TextAuditingVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-driver")
public interface CiFeignClient {

    @PostMapping("/textAuditing")
    TextAuditingVo textAuditing(@RequestBody String content);
}