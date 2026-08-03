package com.qrh.youshangdache.rules.controller;

import com.qrh.youshangdache.model.form.rules.FeeRuleRequestForm;
import com.qrh.youshangdache.model.vo.rules.FeeRuleResponseVo;
import com.qrh.youshangdache.rules.service.FeeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rules/fee")
public class FeeRuleController {

    @Resource
    private FeeRuleService feeRuleService;

    @Operation(summary = "计算订单费用")
    @PostMapping("/calculateOrderFee")
    public FeeRuleResponseVo calculateOrderFee(@RequestBody FeeRuleRequestForm calculateOrderFeeForm) {
        return feeRuleService.calculateOrderFee(calculateOrderFeeForm);
    }

}

