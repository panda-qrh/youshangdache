package com.youshangdache.order.service;

import com.youshangdache.model.entity.order.OrderMonitor;
import com.youshangdache.model.entity.order.OrderMonitorRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderMonitorService extends IService<OrderMonitor> {
    /**
     * 保存订单监控记录数据
     *
     * @param orderMonitorRecord
     * @return true
     */
    Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord);

    Long saveOrderMonitor(OrderMonitor orderMonitor);

    OrderMonitor getOrderMonitor(Long orderId);

    Boolean updateOrderMonitor(OrderMonitor orderMonitor);
}
