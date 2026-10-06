package com.youshangdache.rules.service.impl;

import com.youshangdache.model.form.rules.RewardRuleRequest;
import com.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.youshangdache.model.vo.rules.RewardRuleResponse;
import com.youshangdache.model.vo.rules.RewardRuleResponseVo;
import com.youshangdache.rules.enums.RuleType;
import com.youshangdache.rules.service.RewardRuleService;
import com.youshangdache.rules.utils.DroolsUtils;
import jakarta.annotation.Resource;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

@Service
public class RewardRuleServiceImpl implements RewardRuleService {

    @Resource
    private DroolsUtils droolsUtils;

    @Override
    public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {
        RewardRuleRequest rewardRuleRequest = RewardRuleRequest.builder()
                .startTime(new DateTime(rewardRuleRequestForm.getStartTime()).toString("HH:mm:ss"))
                .orderNum(rewardRuleRequestForm.getOrderNum())
                .build();

        RewardRuleResponse response = droolsUtils.execute(rewardRuleRequest, RuleType.REWARD, RewardRuleResponse.class);

        RewardRuleResponseVo rewardRuleResponseVo = new RewardRuleResponseVo();
        rewardRuleResponseVo.setRewardAmount(response.getRewardAmount());
        return rewardRuleResponseVo;
    }
}