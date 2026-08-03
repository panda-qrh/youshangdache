package com.youshangdache.model.entity.dispatch;

import com.youshangdache.model.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单任务
 */
@Data
@Schema(description = "订单任务关联表")
@TableName("order_job")
public class OrderJob extends BaseEntity {

    private static final long serialVersionUID = 1L;
    /**
     * 订单id
     */
    @Schema(description = "订单id")
    @TableField("order_id")
    private Long orderId;
    /**
     * 任务id
     */
    @Schema(description = "任务id")
    @TableField("job_id")
    private Long jobId;
    /**
     * 参数
     */
    @Schema(description = "参数")
    @TableField("parameter")
    private String parameter;

}