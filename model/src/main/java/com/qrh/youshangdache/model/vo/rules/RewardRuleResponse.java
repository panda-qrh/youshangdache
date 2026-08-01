package com.qrh.youshangdache.model.vo.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RewardRuleResponse {


    @Schema(description = "奖励金额")
    private BigDecimal rewardAmount;

}
