package com.qrh.youshangdache.rules.service.impl;

import com.qrh.youshangdache.model.form.rules.ProfitsharingRuleRequest;
import com.qrh.youshangdache.model.form.rules.ProfitsharingRuleRequestForm;
import com.qrh.youshangdache.model.vo.rules.ProfitsharingRuleResponse;
import com.qrh.youshangdache.model.vo.rules.ProfitsharingRuleResponseVo;
import com.qrh.youshangdache.rules.mapper.ProfitsharingRuleMapper;
import com.qrh.youshangdache.rules.service.ProfitsharingRuleService;
import com.qrh.youshangdache.rules.utils.DroolsUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfitsharingRuleServiceImpl implements ProfitsharingRuleService {

    @Autowired
    private ProfitsharingRuleMapper profitsharingRuleMapper;

    @Autowired
    private DroolsUtils droolsUtils;

    @Override
    public ProfitsharingRuleResponseVo calculateProfitSharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        ProfitsharingRuleRequest profitsharingRuleRequest = ProfitsharingRuleRequest.builder()
                .orderNum(profitsharingRuleRequestForm.getOrderNum())
                .orderAmount(profitsharingRuleRequestForm.getOrderAmount())
                .build();

        ProfitsharingRuleResponse profitsharingRuleResponse = droolsUtils.execute(
                profitsharingRuleRequest, "profitsharingRuleResponse", ProfitsharingRuleResponse.class);

        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = new ProfitsharingRuleResponseVo();
        BeanUtils.copyProperties(profitsharingRuleResponse, profitsharingRuleResponseVo);
        return profitsharingRuleResponseVo;
    }
}