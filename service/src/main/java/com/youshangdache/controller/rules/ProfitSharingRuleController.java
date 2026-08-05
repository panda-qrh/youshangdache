package com.youshangdache.controller.rules;

import com.youshangdache.model.form.rules.ProfitsharingRuleRequestForm;
import com.youshangdache.model.vo.rules.ProfitsharingRuleResponseVo;
import com.youshangdache.service.rules.ProfitsharingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rules/profitsharing")
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProfitSharingRuleController {
    @Resource
    private ProfitsharingRuleService  profitsharingRuleService;

    @Operation(summary = "计算订单奖励费用")
    @PostMapping("/calculateProfitsharingFee")
    public ProfitsharingRuleResponseVo calculateProfitSharingFee(@RequestBody ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        return profitsharingRuleService.calculateProfitSharingFee(profitsharingRuleRequestForm);
    }

}

