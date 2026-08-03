package com.youshangdache.model.entity.system;

import com.youshangdache.model.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youshangdache.model.enums.LoginStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "SysLoginLog")
@TableName("sys_login_log")
public class SysLoginLog extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "用户账号")
	@TableField("username")
	private String username;

	@Schema(description = "登录IP地址")
	@TableField("ipaddr")
	private String ipaddr;

	@Schema(description = "登录状态（0登录成功 1登录失败）")
	@TableField("status")
	private LoginStatusEnum status;

	@Schema(description = "提示信息")
	@TableField("msg")
	private String msg;

	@Schema(description = "访问时间")
	@TableField("access_time")
	private Date accessTime;

}