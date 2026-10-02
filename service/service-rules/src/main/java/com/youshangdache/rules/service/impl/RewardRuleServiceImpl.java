package com.youshangdache.rules.service.impl;

import com.youshangdache.model.form.rules.RewardRuleRequest;
import com.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.youshangdache.model.vo.rules.RewardRuleResponse;
import com.youshangdache.model.vo.rules.RewardRuleResponseVo;
import com.youshangdache.rules.service.RewardRuleService;
import com.youshangdache.rules.utils.DroolsUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RewardRuleServiceImpl implements RewardRuleService {

    @Autowired
    private DroolsUtils droolsUtils;

    @Override
    public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {
        //奖励规则的条件里包含 startTime 时段判断，这里必须把开始服务时间一起传进去，
        //否则 startTime 为 null，规则永远匹配不上、奖励金额恒为空。
        RewardRuleRequest rewardRuleRequest = RewardRuleRequest.builder()
                .startTime(new DateTime(rewardRuleRequestForm.getStartTime()).toString("HH:mm:ss"))
                .orderNum(rewardRuleRequestForm.getOrderNum())
                .build();

        RewardRuleResponse response = droolsUtils.execute(rewardRuleRequest, "rewardRuleResponse", RewardRuleResponse.class);

        RewardRuleResponseVo rewardRuleResponseVo = new RewardRuleResponseVo();
        rewardRuleResponseVo.setRewardAmount(response.getRewardAmount());
        return rewardRuleResponseVo;
    }
}