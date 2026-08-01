package com.qrh.youshangdache.rules.service.impl;

import com.qrh.youshangdache.model.form.rules.RewardRuleRequest;
import com.qrh.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.qrh.youshangdache.model.vo.rules.RewardRuleResponse;
import com.qrh.youshangdache.model.vo.rules.RewardRuleResponseVo;
import com.qrh.youshangdache.rules.service.RewardRuleService;
import com.qrh.youshangdache.rules.utils.DroolsUtils;
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