package com.youshangdache.service.rules;

import com.youshangdache.model.form.payment.ProfitsharingForm;
import com.youshangdache.model.form.rules.ProfitsharingRuleRequestForm;
import com.youshangdache.model.vo.rules.ProfitsharingRuleResponseVo;

public interface ProfitsharingRuleService {

    ProfitsharingRuleResponseVo calculateProfitSharingFee(ProfitsharingRuleRequestForm  profitsharingRuleRequestForm);
}
