package com.youshangdache.order;

import com.youshangdache.model.entity.order.OrderMonitor;
import com.youshangdache.model.entity.order.OrderMonitorRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(value = "service", path = "/order/monitor")
public interface OrderMonitorFeignClient {

    /**
     * 根据订单id获取订单监控信息
     */
    @GetMapping("/getOrderMonitor/{orderId}")
    OrderMonitor getOrderMonitor(@PathVariable Long orderId);

    /**
     * 更新订单监控信息
     *
     * @param OrderMonitor
     * @return
     */
    @PostMapping("/updateOrderMonitor")
    Boolean updateOrderMonitor(@RequestBody OrderMonitor OrderMonitor);

    /**
     * 保存订单监控记录数据
     *
     * @param orderMonitorRecord
     * @return true
     */
    @PostMapping("/saveOrderMonitorRecord")
    Boolean saveMonitorRecord(@RequestBody OrderMonitorRecord orderMonitorRecord);
}