package com.youshangdache.service.rules;

import com.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.youshangdache.model.vo.rules.RewardRuleResponseVo;

public interface RewardRuleService {
    /**
     * 计算订单奖励费用
     *
     * @param rewardRuleRequestForm
     * @return
     */
    RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm);
}
