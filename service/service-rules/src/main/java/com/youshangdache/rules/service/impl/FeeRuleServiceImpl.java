package com.youshangdache.rules.service.impl;

import com.youshangdache.model.form.rules.FeeRuleRequest;
import com.youshangdache.model.form.rules.FeeRuleRequestForm;
import com.youshangdache.model.vo.rules.FeeRuleResponse;
import com.youshangdache.model.vo.rules.FeeRuleResponseVo;
import com.youshangdache.rules.service.FeeRuleService;
import com.youshangdache.rules.utils.DroolsUtils;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FeeRuleServiceImpl implements FeeRuleService {

    @Autowired
    private DroolsUtils droolsUtils;

    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm) {
        FeeRuleRequest feeRuleRequest = FeeRuleRequest.builder()
                .distance(feeRuleRequestForm.getDistance())
                .startTime(new DateTime(feeRuleRequestForm.getStartTime()).toString("HH:mm:ss"))
                .waitMinute(feeRuleRequestForm.getWaitMinute())
                .build();

        FeeRuleResponse feeRuleResponse = droolsUtils.execute(feeRuleRequest, "feeRuleResponse", FeeRuleResponse.class);

        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        BeanUtils.copyProperties(feeRuleResponse, feeRuleResponseVo);
        return feeRuleResponseVo;
    }
}