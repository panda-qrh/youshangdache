package com.youshangdache.model.vo.dispatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Builder
public class NewOrderTaskVo {
    /**
     * 订单id
     */
    @Schema(description = "订单id")
    private Long orderId;
    /**
     * 起始地点
     */
    @Schema(description = "起始地点")
    private String startLocation;
    /**
     * 起始地点经度
     */
    @Schema(description = "起始地点经度")
    private BigDecimal startPointLongitude;
    /**
     * 起始点纬度
     */
    @Schema(description = "起始点伟度")
    private BigDecimal startPointLatitude;
    /**
     * 结束地点
     */
    @Schema(description = "结束地点")
    private String endLocation;
    /**
     * 结束地点经度
     */
    @Schema(description = "结束地点经度")
    private BigDecimal endPointLongitude;
    /**
     * 结束地点纬度
     */
    @Schema(description = "结束地点纬度")
    private BigDecimal endPointLatitude;
    /**
     * 预估订单金额
     */
    @Schema(description = "预估订单金额")
    private BigDecimal expectAmount;
    /**
     * 预估里程
     */
    @Schema(description = "预估里程")
    private BigDecimal expectDistance;
    /**
     * 预估时间
     */
    @Schema(description = "预估时间")
    private BigDecimal expectTime;
    /**
     * 顾客好处费
     */
    @Schema(description = "顾客好处费")
    private BigDecimal favourFee;
    /**
     * 下单时间
     */
    @Schema(description = "下单时间")
    private Date createTime;
}