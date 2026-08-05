package com.youshangdache.service.impl.rules;

import com.youshangdache.model.form.rules.ProfitsharingRuleRequest;
import com.youshangdache.model.form.rules.ProfitsharingRuleRequestForm;
import com.youshangdache.model.vo.rules.ProfitsharingRuleResponse;
import com.youshangdache.model.vo.rules.ProfitsharingRuleResponseVo;
import com.youshangdache.mapper.rules.ProfitsharingRuleMapper;
import com.youshangdache.service.rules.ProfitsharingRuleService;
import com.youshangdache.utils.DroolsUtils;
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