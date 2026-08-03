package com.youshangdache.model.entity.driver;

import com.youshangdache.model.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youshangdache.model.enums.DriverServiceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 司机配置
 */
@Data
@Schema(description = "DriverSet")
@TableName("driver_set")
public class DriverSet extends BaseEntity {

	private static final long serialVersionUID = 1L;

    @Schema(description = "司机ID")
	@TableField("driver_id")
	private Long driverId;

	@Schema(description = "服务状态 1：开始接单 0：未接单")
	@TableField("service_status")
	private DriverServiceStatusEnum serviceStatus;

    @Schema(description = "订单里程设置，0表示无限制")
	@TableField("order_distance")
	private BigDecimal orderDistance;

    @Schema(description = "接单里程设置")
	@TableField("accept_distance")
	private BigDecimal acceptDistance;

    @Schema(description = "是否自动接单")
	@TableField("is_auto_accept")
	private Boolean isAutoAccept=Boolean.FALSE;

}