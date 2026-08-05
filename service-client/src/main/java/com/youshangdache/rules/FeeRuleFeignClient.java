package com.youshangdache.rules;

import com.youshangdache.model.form.rules.FeeRuleRequestForm;
import com.youshangdache.model.vo.rules.FeeRuleResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service")
public interface FeeRuleFeignClient {
    /**
     * 计算预预估的订单费用
     * @param calculateOrderFeeForm
     * @return
     */
    @PostMapping("/rules/fee/calculateOrderFee")
    FeeRuleResponseVo calculateOrderFee(@RequestBody FeeRuleRequestForm calculateOrderFeeForm);
}