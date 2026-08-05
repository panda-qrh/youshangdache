package com.youshangdache.coupon;

import com.youshangdache.model.form.coupon.UseCouponForm;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.coupon.AvailableCouponVo;
import com.youshangdache.model.vo.coupon.NoReceiveCouponVo;
import com.youshangdache.model.vo.coupon.NoUseCouponVo;
import com.youshangdache.model.vo.coupon.UsedCouponVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;


@FeignClient(value = "service")
public interface CouponFeignClient {
    /**
     * 领取优惠券
     *
     * @param customerId
     * @param couponId
     * @return
     */
    @GetMapping("/coupon/info/receive/{customerId}/{couponId}")
    Boolean receive(@PathVariable("customerId") Long customerId, @PathVariable("couponId") Long couponId);

    /**
     * 查询已使用优惠券分页列表
     *
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/coupon/info/findUsedPage/{customerId}/{page}/{limit}")
    PageVo<UsedCouponVo> findUsedPage(
            @PathVariable("customerId") Long customerId,
            @PathVariable("page") Long page,
            @PathVariable("limit") Long limit);

    /**
     * 查询未使用优惠券分页列表
     *
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/coupon/info/findNoUsePage/{customerId}/{page}/{limit}")
    PageVo<NoUseCouponVo> findNoUsePage(
            @PathVariable("customerId") Long customerId,
            @PathVariable("page") Long page,
            @PathVariable("limit") Long limit);

    /**
     * 查询未领取优惠券分页列表
     *
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/coupon/info/findNoReceivePage/{customerId}/{page}/{limit}")
    PageVo<NoReceiveCouponVo> findNoReceivePage(
            @PathVariable("customerId") Long customerId,
            @PathVariable("page") Long page,
            @PathVariable("limit") Long limit);

    @GetMapping("/coupon/info/findAvailableCoupon/{customerId}/{orderAmount}")
    List<AvailableCouponVo> findAvailableCoupon(@PathVariable Long customerId, @PathVariable BigDecimal orderAmount);

    @PostMapping("/useCoupon")
    BigDecimal useCoupon(@RequestBody UseCouponForm useCouponForm);
}