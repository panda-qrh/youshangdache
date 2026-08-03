package com.qrh.youshangdache.rules.controller;

import com.qrh.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.qrh.youshangdache.model.vo.rules.RewardRuleResponseVo;
import com.qrh.youshangdache.rules.service.RewardRuleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rules/reward")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RewardRuleController {
    @Resource
    private RewardRuleService rewardRuleService;

    /**
     * 计算订单奖励费用
     *
     * @param rewardRuleRequestForm
     * @return
     */
    @Operation(summary = "计算订单奖励费用")
    @PostMapping("calculateOrderRewardFee")
    public RewardRuleResponseVo calculateOrderRewardFee(@RequestBody RewardRuleRequestForm rewardRuleRequestForm) {
        return rewardRuleService.calculateOrderRewardFee(rewardRuleRequestForm);
    }

}

