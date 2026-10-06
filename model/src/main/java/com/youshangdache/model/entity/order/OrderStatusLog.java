package com.youshangdache.model.entity.order;

import com.youshangdache.model.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.model.enums.OrderOperatorEnum;
import com.youshangdache.model.enums.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 订单状态流转审计日志
 *
 * <p>状态机改造后，这里记录的是"一次真实生效的迁移"：
 * 从哪个状态、因为什么事件、变成哪个状态、由谁触发。
 * 只在数据库条件更新（CAS）成功之后写入，因此日志里不会出现"没生效"的迁移。</p>
 */
@Data
@Schema(description = "OrderStatusLog")
@TableName("order_status_log")
public class OrderStatusLog extends BaseEntity {

	private static final long serialVersionUID = 1L;

    @Schema(description = "orderId")
	@TableField("order_id")
	private String orderId;

    @Schema(description = "变更前的状态")
	@TableField("from_status")
	private OrderStatusEnum fromStatus;

    @Schema(description = "变更后的订单状态")
	@TableField("order_status")
	private OrderStatusEnum orderStatus;

    @Schema(description = "触发本次迁移的事件")
	@TableField("event")
	private OrderEventEnum event;

    @Schema(description = "操作人id（乘客id/司机id/管理员id，系统触发为空）")
	@TableField("operator_id")
	private Long operatorId;

    @Schema(description = "操作人类型：1乘客 2司机 3系统 4后台管理员")
	@TableField("operator_type")
	private OrderOperatorEnum operatorType;

    @Schema(description = "备注（例如事故关闭的原因）")
	@TableField("remark")
	private String remark;

    @Schema(description = "操作时间")
	@TableField("operate_time")
	private Date operateTime;

}
