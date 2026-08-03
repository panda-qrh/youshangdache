package com.qrh.youshangdache.customer.service.impl;

import com.qrh.youshangdache.coupon.feign.CouponFeignClient;
import com.qrh.youshangdache.customer.service.CouponService;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.coupon.AvailableCouponVo;
import com.qrh.youshangdache.model.vo.coupon.NoReceiveCouponVo;
import com.qrh.youshangdache.model.vo.coupon.NoUseCouponVo;
import com.qrh.youshangdache.model.vo.coupon.UsedCouponVo;
import com.qrh.youshangdache.model.vo.order.OrderBillVo;
import com.qrh.youshangdache.order.feign.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CouponServiceImpl implements CouponService {
    @Resource
    private CouponFeignClient couponFeignClient;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;


    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, Long orderId) {
        OrderBillVo orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
        return couponFeignClient.findAvailableCoupon(customerId,orderBillVo.getPayAmount()).getData();
    }

    @Override
    public Boolean receive(Long customerId, Long couponId) {
        return couponFeignClient.receive(customerId, couponId).getData();
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Long customerId, Long page, Long limit) {
        return couponFeignClient.findUsedPage(customerId, page, limit).getData();
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Long customerId, Long page, Long limit) {
        return couponFeignClient.findNoUsePage(customerId, page, limit).getData();
    }

    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Long customerId, Long page, Long limit) {
        return couponFeignClient.findNoReceivePage(customerId, page, limit).getData();
    }
}
