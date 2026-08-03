package com.youshangdache.rules;

import com.youshangdache.model.form.rules.ProfitsharingRuleRequestForm;
import com.youshangdache.model.vo.rules.ProfitsharingRuleResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-rules")
public interface ProfitsharingRuleFeignClient {

    @PostMapping("/rules/profitsharing/calculateProfitsharingFee")
    public ProfitsharingRuleResponseVo calculateProfitSharingFee(@RequestBody ProfitsharingRuleRequestForm profitsharingRuleRequestForm);


}