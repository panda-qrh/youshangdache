package com.youshangdache.controller.order;

import com.youshangdache.model.entity.order.OrderMonitor;
import com.youshangdache.model.entity.order.OrderMonitorRecord;
import com.youshangdache.service.order.OrderMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/monitor")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderMonitorController {
    @Resource
    private OrderMonitorService orderMonitorService;


    @Operation(summary = "根据订单id获取订单监控信息")
    @GetMapping("/getOrderMonitor/{orderId}")
    public OrderMonitor getOrderMonitor(@PathVariable Long orderId) {
        return orderMonitorService.getOrderMonitor(orderId);
    }

    @Operation(summary = "更新订单监控信息")
    @PostMapping("/updateOrderMonitor")
    public Boolean updateOrderMonitor(@RequestBody OrderMonitor OrderMonitor) {
        return orderMonitorService.updateOrderMonitor(OrderMonitor);
    }

    /**
     * 保存订单监控记录数据
     *
     * @param orderMonitorRecord
     * @return true
     */
    @Operation(summary = "保存订单监控记录数据")
    @PostMapping("/saveOrderMonitorRecord")
    public Boolean saveMonitorRecord(@RequestBody OrderMonitorRecord orderMonitorRecord) {
        return orderMonitorService.saveOrderMonitorRecord(orderMonitorRecord);
    }
}

