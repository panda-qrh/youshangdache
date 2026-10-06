package com.youshangdache.mgr.service.impl;

import com.youshangdache.mgr.service.OrderInfoService;
import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.order.OrderInfoFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl implements OrderInfoService {

	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;

	@Override
	public Boolean closeOrderByCaseAccident(Long orderId, String remark) {
		return orderInfoFeignClient.closeOrderByCaseAccident(orderId, remark);
	}

	@Override
	public List<OrderEventEnum> availableEvents(Long orderId) {
		return orderInfoFeignClient.availableEvents(orderId);
	}
}
