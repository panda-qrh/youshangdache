package com.youshangdache.mgr.service;

import com.youshangdache.model.enums.OrderEventEnum;

import java.util.List;

public interface OrderInfoService {

    /**
     * 事故关闭订单（后台介入）
     *
     * @param orderId 订单id
     * @param remark  关闭原因
     * @return true
     */
    Boolean closeOrderByCaseAccident(Long orderId, String remark);

    /**
     * 查询订单当前状态下可执行的操作
     *
     * @param orderId 订单id
     * @return 可用事件列表
     */
    List<OrderEventEnum> availableEvents(Long orderId);
}
