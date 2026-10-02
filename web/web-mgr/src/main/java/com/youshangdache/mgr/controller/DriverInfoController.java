package com.youshangdache.mgr.controller;

import com.youshangdache.common.annotation.Log;
import com.youshangdache.common.result.Result;
import com.youshangdache.mgr.service.DriverInfoService;
import com.youshangdache.model.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoController {
	
	@Autowired
	private DriverInfoService driverInfoService;

	/**
	 * 审核司机认证
	 *
	 * <p>原来该 Controller 是空壳，没有任何审核入口，导致 driver_info.auth_status
	 * 永远无法变成"认证通过"，司机端开启接单时必然被 DRIVER_NOT_AUTH 拦住。</p>
	 *
	 * @param driverId   司机id
	 * @param authStatus 审核结果：1 审核中，2 认证通过，-1 认证未通过
	 * @return true
	 */
	@Operation(summary = "审核司机认证信息")
	@PreAuthorize("hasAuthority('bnt.driverInfo.update')")
	@Log(title = "司机管理", businessType = BusinessTypeEnum.UPDATE)
	@PostMapping("/updateDriverAuthStatus/{driverId}/{authStatus}")
	public Result<Boolean> updateDriverAuthStatus(@PathVariable Long driverId, @PathVariable Integer authStatus) {
		return Result.ok(driverInfoService.updateDriverAuthStatus(driverId, authStatus));
	}

}
