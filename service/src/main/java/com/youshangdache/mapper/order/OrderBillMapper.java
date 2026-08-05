package com.youshangdache.mapper.order;

import com.youshangdache.model.entity.order.OrderBill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

@Mapper
public interface OrderBillMapper extends BaseMapper<OrderBill> {

    void updateCouponAmount(Long orderId, BigDecimal couponAmount);
}
