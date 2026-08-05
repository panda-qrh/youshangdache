package com.youshangdache.controller.rules;

import com.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.youshangdache.model.vo.rules.RewardRuleResponseVo;
import com.youshangdache.service.rules.RewardRuleService;
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

