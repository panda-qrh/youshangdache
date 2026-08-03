package com.qrh.youshangdache.rules.client;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.qrh.youshangdache.model.vo.rules.RewardRuleResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-rules")
public interface RewardRuleFeignClient {

    @PostMapping("/rules/reward/calculateOrderRewardFee")
    public Result<RewardRuleResponseVo> calculateOrderRewardFee(@RequestBody RewardRuleRequestForm rewardRuleRequestForm);




}