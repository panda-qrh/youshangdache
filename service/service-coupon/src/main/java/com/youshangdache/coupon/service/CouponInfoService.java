package com.youshangdache.coupon.service;

import com.youshangdache.model.entity.coupon.CouponInfo;
import com.youshangdache.model.form.coupon.UseCouponForm;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.coupon.AvailableCouponVo;
import com.youshangdache.model.vo.coupon.NoReceiveCouponVo;
import com.youshangdache.model.vo.coupon.NoUseCouponVo;
import com.youshangdache.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface CouponInfoService extends IService<CouponInfo> {

    /**
     * 查询未领取优惠券分页列表
     *
     * @param customerId 用户id
     * @param pageParam  分页参数
     * @return 优惠券分页列表
     */
    PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId);
    /**
     * 查询未使用优惠券分页列表
     *
     * @param customerId 用户id
     * @param pageParam       分页参数
     * @return 优惠券分页数据
     */
    PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId);

    PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId);
    /**
     * 领取优惠券
     *
     * @param customerId 用户id
     * @param couponId   优惠券id
     * @return true
     */
    Boolean receive(Long customerId, Long couponId);
    /**
     * 获取未使用的最佳优惠券信息
     *
     * @param customerId  用户id
     * @param orderAmount 订单金额
     * @return 可用的优惠券列表
     */
    List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount);

    BigDecimal useCoupon(UseCouponForm useCouponForm);

}
