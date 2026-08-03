package com.youshangdache.rules.service.impl;

import com.youshangdache.model.form.rules.RewardRuleRequest;
import com.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.youshangdache.model.vo.rules.RewardRuleResponse;
import com.youshangdache.model.vo.rules.RewardRuleResponseVo;
import com.youshangdache.rules.service.RewardRuleService;
import com.youshangdache.rules.utils.DroolsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RewardRuleServiceImpl implements RewardRuleService {

    @Autowired
    private DroolsUtils droolsUtils;

    @Override
    public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {
        RewardRuleRequest rewardRuleRequest = new RewardRuleRequest();
        rewardRuleRequest.setOrderNum(rewardRuleRequestForm.getOrderNum());

        RewardRuleResponse response = droolsUtils.execute(rewardRuleRequest, "rewardRuleResponse", RewardRuleResponse.class);

        RewardRuleResponseVo rewardRuleResponseVo = new RewardRuleResponseVo();
        rewardRuleResponseVo.setRewardAmount(response.getRewardAmount());
        return rewardRuleResponseVo;
    }
}