package com.youshangdache.controller.coupon;

import com.youshangdache.service.coupon.CouponInfoService;
import com.youshangdache.model.entity.coupon.CouponInfo;
import com.youshangdache.model.form.coupon.UseCouponForm;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.coupon.AvailableCouponVo;
import com.youshangdache.model.vo.coupon.NoReceiveCouponVo;
import com.youshangdache.model.vo.coupon.NoUseCouponVo;
import com.youshangdache.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;


@Tag(name = "优惠券活动接口管理")
@RestController
@RequestMapping(value = "/coupon/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoController {
    @Resource
    private CouponInfoService couponInfoService;

    /**
     * 查询未领取优惠券分页列表
     *
     * @param customerId 用户id
     * @param page       页码
     * @param limit      每页数
     * @return 优惠券分页列表
     */
    @Operation(summary = "查询未领取优惠券分页列表")
    @GetMapping("/findNoReceivePage/{customerId}/{page}/{limit}")
    public PageVo<NoReceiveCouponVo> findNoReceivePage(@PathVariable Long customerId,
                                                               @PathVariable Long page,
                                                               @PathVariable Long limit) {
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        PageVo<NoReceiveCouponVo> pageVo = couponInfoService.findNoReceivePage(pageParam, customerId);
//        pageVo.setPage(page);
//        pageVo.setLimit(limit);
        return pageVo;
    }

    /**
     * 查询未使用优惠券分页列表
     *
     * @param customerId 用户id
     * @param page       页码
     * @param limit      每页数
     * @return 优惠券分页数据
     */
    @Operation(summary = "查询未使用优惠券分页列表")
    @GetMapping("findNoUsePage/{customerId}/{page}/{limit}")
    public PageVo<NoUseCouponVo> findNoUsePage(
            @PathVariable Long customerId,
            @PathVariable Long page,
            @PathVariable Long limit) {
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        PageVo<NoUseCouponVo> pageVo = couponInfoService.findNoUsePage(pageParam, customerId);
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return pageVo;
    }

    @Operation(summary = "查询已使用优惠券分页列表")
    @GetMapping("findUsedPage/{customerId}/{page}/{limit}")
    public PageVo<UsedCouponVo> findUsedPage(
            @PathVariable Long customerId,
            @PathVariable Long page,
            @PathVariable Long limit) {
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        PageVo<UsedCouponVo> pageVo = couponInfoService.findUsedPage(pageParam, customerId);
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return pageVo;
    }

    /**
     * 领取优惠券
     *
     * @param customerId 用户id
     * @param couponId   优惠券id
     * @return true
     */
    @Operation(summary = "领取优惠券")
    @GetMapping("/receive/{customerId}/{couponId}")
    public Boolean receive(@PathVariable Long customerId, @PathVariable Long couponId) {
        return couponInfoService.receive(customerId, couponId);
    }

    /**
     * 获取未使用的最佳优惠券信息
     *
     * @param customerId  用户id
     * @param orderAmount 订单金额
     * @return 可用的优惠券列表
     */
    @Operation(summary = "获取未使用的最佳优惠券信息")
    @GetMapping("/findAvailableCoupon/{customerId}/{orderAmount}")
    public List<AvailableCouponVo> findAvailableCoupon(@PathVariable Long customerId, @PathVariable BigDecimal orderAmount) {
        return couponInfoService.findAvailableCoupon(customerId, orderAmount);
    }

    @Operation(summary = "使用优惠券")
    @PostMapping("/useCoupon")
    public BigDecimal useCoupon(@RequestBody UseCouponForm useCouponForm) {
        return couponInfoService.useCoupon(useCouponForm);
    }

}

