package com.youshangdache.mgr.controller;

import com.youshangdache.common.annotation.Log;
import com.youshangdache.common.result.Result;
import com.youshangdache.mgr.service.OrderInfoService;
import com.youshangdache.model.enums.BusinessTypeEnum;
import com.youshangdache.model.enums.OrderEventEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "订单API接口管理")
@RestController
@RequestMapping(value="/order/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoController {
	
	@Autowired
	private OrderInfoService orderInfoService;

	/**
	 * 事故关闭订单
	 *
	 * <p>代驾过程中发生事故，由后台关闭订单并记录原因。</p>
	 *
	 * @param orderId 订单id
	 * @param remark  关闭原因
	 * @return true
	 */
	@Operation(summary = "事故关闭订单")
	@PreAuthorize("hasAuthority('bnt.orderInfo.update')")
	@Log(title = "订单管理", businessType = BusinessTypeEnum.UPDATE)
	@PostMapping("/closeOrderByCaseAccident/{orderId}")
	public Result<Boolean> closeOrderByCaseAccident(@PathVariable Long orderId,
	                                                @RequestParam(required = false) String remark) {
		return Result.ok(orderInfoService.closeOrderByCaseAccident(orderId, remark));
	}

	/**
	 * 查询订单当前状态下可执行的操作
	 *
	 * @param orderId 订单id
	 * @return 可用事件列表
	 */
	@Operation(summary = "查询订单当前可执行的操作")
	@PreAuthorize("hasAuthority('bnt.orderInfo.list')")
	@GetMapping("/availableEvents/{orderId}")
	public Result<List<OrderEventEnum>> availableEvents(@PathVariable Long orderId) {
		return Result.ok(orderInfoService.availableEvents(orderId));
	}

}
