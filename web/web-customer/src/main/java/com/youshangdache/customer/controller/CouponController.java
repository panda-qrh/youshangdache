package com.youshangdache.customer.controller;

import com.youshangdache.common.annotation.Login;
import com.youshangdache.common.result.Result;
import com.youshangdache.common.util.AuthContextHolder;
import com.youshangdache.customer.service.CouponService;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.coupon.AvailableCouponVo;
import com.youshangdache.model.vo.coupon.NoReceiveCouponVo;
import com.youshangdache.model.vo.coupon.NoUseCouponVo;
import com.youshangdache.model.vo.coupon.UsedCouponVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "优惠券活动接口管理")
@RestController
@RequestMapping(value = "/coupon")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponController {
    @Autowired
    private CouponService couponService;

    @Operation(summary = "查询未领取优惠券分页列表")
    @Login
    @GetMapping("findNoReceivePage/{page}/{limit}")
    public Result<PageVo<NoReceiveCouponVo>> findNoReceivePage(@PathVariable Long page, @PathVariable Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo<NoReceiveCouponVo> pageVo = couponService.findNoReceivePage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "查询未使用优惠券分页列表")
    @Login
    @GetMapping("findNoUsePage/{page}/{limit}")
    public Result<PageVo<NoUseCouponVo>> findNoUsePage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo<NoUseCouponVo> pageVo = couponService.findNoUsePage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "查询已使用优惠券分页列表")
    @Login
    @GetMapping("findUsedPage/{page}/{limit}")
    public Result<PageVo<UsedCouponVo>> findUsedPage(
            @PathVariable Long page,
            @PathVariable Long limit) {
        Long customerId = AuthContextHolder.getUserId();
        PageVo<UsedCouponVo> pageVo = couponService.findUsedPage(customerId, page, limit);
        return Result.ok(pageVo);
    }

    @Operation(summary = "领取优惠券")
    @Login
    @GetMapping("/receive/{couponId}")
    public Result<Boolean> receive(@PathVariable Long couponId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(couponService.receive(customerId, couponId));
    }

    @Operation(summary = "获取未使用的最佳优惠券信息")
    @Login
    @GetMapping("/findAvailableCoupon/{orderId}")
    public Result<List<AvailableCouponVo>> findAvailableCoupon(@PathVariable Long orderId ) {
        return Result.ok(couponService.findAvailableCoupon(AuthContextHolder.getUserId(), orderId));
    }
    
    
}

