package com.qrh.youshangdache.mgr.service.impl;

import com.qrh.youshangdache.mgr.service.OrderInfoService;
import com.qrh.youshangdache.order.feign.OrderInfoFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl implements OrderInfoService {

	@Autowired
	private OrderInfoFeignClient orderInfoFeignClient;



}
