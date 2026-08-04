package com.youshangdache.rules;

import com.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.youshangdache.model.vo.rules.RewardRuleResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-rules")
public interface RewardRuleFeignClient {

    @PostMapping("/rules/reward/calculateOrderRewardFee")
    RewardRuleResponseVo calculateOrderRewardFee(@RequestBody RewardRuleRequestForm rewardRuleRequestForm);


}