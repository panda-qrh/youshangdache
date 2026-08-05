package com.youshangdache.service.rules;

import com.youshangdache.model.form.rules.FeeRuleRequestForm;
import com.youshangdache.model.vo.rules.FeeRuleResponseVo;

public interface FeeRuleService {

    FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm);


}
