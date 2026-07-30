package com.qrh.youshangdache.model.entity.customer;

import com.qrh.youshangdache.model.entity.base.BaseEntity;
import com.qrh.youshangdache.model.enums.LoginStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户（乘客）登录日志
 */
@Data
@Schema(description = "CustomerLoginLog")
@AllArgsConstructor
@NoArgsConstructor
public class CustomerLoginLog extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "客户id")
	private Long customerId;

	@Schema(description = "登录IP地址")
	private String ipaddr;

	@Schema(description = "登录状态（0成功 1失败）")
	private LoginStatusEnum status;

	@Schema(description = "提示信息")
	private String msg;

}