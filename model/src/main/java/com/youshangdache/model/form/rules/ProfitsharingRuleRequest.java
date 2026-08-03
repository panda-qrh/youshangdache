package com.youshangdache.model.form.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfitsharingRuleRequest {

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "当天完成订单个数")
    private Long orderNum;

}
