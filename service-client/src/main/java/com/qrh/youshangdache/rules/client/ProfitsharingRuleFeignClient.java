package com.qrh.youshangdache.rules.client;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.form.rules.ProfitsharingRuleRequestForm;
import com.qrh.youshangdache.model.vo.rules.ProfitsharingRuleResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-rules")
public interface ProfitsharingRuleFeignClient {

    @PostMapping("/rules/profitsharing/calculateProfitsharingFee")
    public Result<ProfitsharingRuleResponseVo> calculateProfitSharingFee(@RequestBody ProfitsharingRuleRequestForm profitsharingRuleRequestForm);


}