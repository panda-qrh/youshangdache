package com.qrh.youshangdache.order.client;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.entity.order.OrderMonitor;
import com.qrh.youshangdache.model.entity.order.OrderMonitorRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(value = "service-order",path = "/order/monitor")
public interface OrderMonitorFeignClient {

    /**
     * 根据订单id获取订单监控信息
     */
    @GetMapping("/getOrderMonitor/{orderId}")
    Result<OrderMonitor> getOrderMonitor(@PathVariable Long orderId);

    /**
     * 更新订单监控信息
     *
     * @param OrderMonitor
     * @return
     */
    @PostMapping("/updateOrderMonitor")
    Result<Boolean> updateOrderMonitor(@RequestBody OrderMonitor OrderMonitor);

    /**
     * 保存订单监控记录数据
     *
     * @param orderMonitorRecord
     * @return true
     */
    @PostMapping("/saveOrderMonitorRecord")
    Result<Boolean> saveMonitorRecord(@RequestBody OrderMonitorRecord orderMonitorRecord);
}