package com.qrh.youshangdache.model.form.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RewardRuleRequest {

    @Schema(description = "代驾时间")
    private String startTime;

    @Schema(description = "订单个数")
    private Long orderNum;

}
