package com.qrh.youshangdache.order.service.impl;

import com.qrh.youshangdache.common.execption.GuiguException;
import com.qrh.youshangdache.common.result.ResultCodeEnum;
import com.qrh.youshangdache.model.entity.order.OrderMonitor;
import com.qrh.youshangdache.model.entity.order.OrderMonitorRecord;
import com.qrh.youshangdache.order.mapper.OrderMonitorMapper;
import com.qrh.youshangdache.order.repository.OrderMonitorRecordRepository;
import com.qrh.youshangdache.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderMonitorServiceImpl extends ServiceImpl<OrderMonitorMapper, OrderMonitor> implements OrderMonitorService {
    @Resource
    private OrderMonitorRecordRepository orderMonitorRecordRepository;
    @Resource
    private OrderMonitorMapper orderMonitorMapper;

    @Override
    public Long saveOrderMonitor(OrderMonitor orderMonitor) {
        orderMonitorMapper.insert(orderMonitor);
        return orderMonitor.getId();
    }

    @Override
    public OrderMonitor getOrderMonitor(Long orderId) {
        return this.getOne(new LambdaQueryWrapper<OrderMonitor>().eq(OrderMonitor::getOrderId, orderId));
    }

    @Override
    public Boolean updateOrderMonitor(OrderMonitor orderMonitor) {
        return this.updateById(orderMonitor);
    }

    /**
     * 保存订单监控记录数据
     *
     * @param orderMonitorRecord
     * @return true
     */
    @Override
    public Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord) {
        if (orderMonitorRecord != null) {
            orderMonitorRecordRepository.save(orderMonitorRecord);
        } else {
            new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

}
