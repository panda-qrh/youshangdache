package com.qrh.youshangdache.order.controller;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.form.order.OrderInfoForm;
import com.qrh.youshangdache.model.form.order.StartDriveForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderBillForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderCartForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.order.*;
import com.qrh.youshangdache.order.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@Tag(name = "订单API接口管理")
@RestController
@RequestMapping(value = "/order/info")
public class OrderInfoController {
    @Resource
    private OrderInfoService orderInfoService;

    /**
     * 保存订单信息
     *
     * @param orderInfoForm 订单信息对象
     * @return 订单id
     */
    @Operation(summary = "保存订单信息")
    @PostMapping("/saveOrderInfo")
    public Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm) {
        return Result.ok(orderInfoService.saveOrderInfo(orderInfoForm));
    }

    /**
     * 乘客下完单后，订单状态为1，乘客端小程序会轮询订单状态，当订单状态为2时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     *
     * @param orderId 订单id
     * @return 订单状态代号
     */
    @Operation(summary = "根据订单id获取订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<OrderStatusEnum> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getOrderStatus(orderId));
    }

    /**
     * 司机抢单
     *
     * <p>
     * 当前司机已经开启接单服务了，实时轮流司机服务器端临时队列，只要有合适的新订单产生，那么就会轮回获取新订单数据，进行语音播放，
     * 如果司机对这个订单感兴趣就可以抢单。注意：同一个新订单会放入满足条件的所有司机的临时队列，谁先抢到就是谁的。
     * </p>
     *
     * @param driverId 司机id
     * @param orderId  订单id
     * @return true抢单成功，否则抛出订单不存在或抢单失败异常
     */
    @Operation(summary = "司机抢单")
    @GetMapping("/robNewOrder/{driverId}/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long driverId, @PathVariable Long orderId) {
        return Result.ok(orderInfoService.robNewOrder(driverId, orderId));
    }

    /**
     * 查找用户当前正在执行的订单
     *
     * <p>
     * 乘客如果已经下过单了，而且这个订单在执行中，没有结束，
     * 那么乘客是不可以再下单的，页面会弹出层，进入执行中的订单。
     * </p>
     *
     * @param customerId 用户id
     * @return 当前用户正在进行的订单信息
     */
    @Operation(summary = "乘客端查找当前订单")
    @GetMapping("/searchCustomerCurrentOrder/{customerId}")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable Long customerId) {
        return Result.ok(orderInfoService.searchCustomerCurrentOrder(customerId));
    }

    /**
     * 查找司机端当前订单
     *
     * <p>
     * 司机只要有执行中的订单，没有结束，那么司机是不可以接单的，页面会弹出层，进入执行中的订单
     * </p>
     *
     * @return 司机当前正在执行的订单数据
     */
    @Operation(summary = "司机端查找当前订单")
    @GetMapping("/searchDriverCurrentOrder/{driverId}")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable Long driverId) {
        return Result.ok(orderInfoService.searchDriverCurrentOrder(driverId));
    }

    /**
     * 获取<strong>执行中</strong>的订单
     *
     * @param orderId 订单id
     * @return 执行中的订单的数据
     */
    @Operation(summary = "根据订单id得到订单信息")
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfo> getOrderInfoByOrderId(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getById(orderId));
    }

    /**
     * 司机到达起始点
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return true
     */
    @Operation(summary = "司机到达起始点")
    @GetMapping("/driverArriveStartLocation/{orderId}/{driverId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId, @PathVariable Long driverId) {
        return Result.ok(orderInfoService.driverArriveStartLocation(orderId, driverId));
    }

    /**
     * 更新代驾车辆信息
     *
     * @param updateOrderCartForm
     * @return true
     */
    @Operation(summary = "更新代驾车辆信息")
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm) {
        return Result.ok(orderInfoService.updateOrderCart(updateOrderCartForm));
    }

    /**
     * 开始代驾服务
     *
     * @param startDriveForm
     * @return true
     */
    @Operation(summary = "开始代驾服务")
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm) {
        return Result.ok(orderInfoService.startDrive(startDriveForm));
    }

    /**
     * 根据时间段获取订单数
     *
     * @param startTime 起始时间
     * @param endTime   终止时间
     * @return 订单数量
     */
    @Operation(summary = "根据时间段获取订单数")
    @GetMapping("/getOrderNumByTime/{startTime}/{endTime}")
    public Result<Long> getOrderNumByTime(@PathVariable String startTime, @PathVariable String endTime) {
        return Result.ok(orderInfoService.getOrderNumByTime(startTime, endTime));
    }

    /**
     * 结束代驾服务，更新订单账单
     *
     * @param updateOrderBillForm 账单
     * @return true
     */
    @Operation(summary = "结束代驾服务更新订单账单")
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody UpdateOrderBillForm updateOrderBillForm) {
        return Result.ok(orderInfoService.endDrive(updateOrderBillForm));
    }

    /**
     * 获取乘客订单分页列表
     *
     * @param customerId 用户id
     * @param limit      页限制
     * @param page       页码
     * @return 订单分页
     */
    @Operation(summary = "获取乘客订单分页列表")
    @GetMapping("/findCustomerOrderPage/{customerId}/{page}/{limit}")
    public Result<PageVo> findCustomerOrderPage(@PathVariable Long customerId,
                                                @PathVariable Long limit,
                                                @PathVariable Long page) {
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        PageVo pageVo = orderInfoService.findCustomerOrderPage(pageParam, customerId);
//        pageVo.setPage(page);
//        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    /**
     * 获取司机订单分页列表
     *
     * @param driverId 司机id
     * @param limit    页限制
     * @param page     页码
     * @return 订单分页
     */
    @Operation(summary = "获取司机订单分页列表")
    @GetMapping("/findDriverOrderPage/{driverId}/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(@PathVariable Long driverId,
                                              @PathVariable Long limit,
                                              @PathVariable Long page) {
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        PageVo pageVo = orderInfoService.findDriverOrderPage(pageParam, driverId);
//        pageVo.setPage(page);
//        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    /**
     * 根据订单id获取实际账单信息
     *
     * @param orderId 订单id
     * @return 该订单的账单信息
     */
    @Operation(summary = "根据订单id获取实际账单信息")
    @GetMapping("/getOrderBillInfo/{orderId}")
    public Result<OrderBillVo> getOrderBillInfo(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getOrderBillInfo(orderId));
    }

    /**
     * 根据订单id获取实际分账信息
     *
     * @param orderId 订单id
     * @return 订单分账数据
     */
    @Operation(summary = "根据订单id获取实际分账信息")
    @GetMapping("/getOrderProfitsharing/{orderId}")
    public Result<OrderProfitsharingVo> getOrderProfitsharing(@PathVariable Long orderId) {
        return Result.ok(orderInfoService.getOrderProfitsharing(orderId));
    }

    /**
     * 发送账单信息
     *
     * <p>
     * 司机端确认账单信息后，点击“发送账单”，乘客端才能切换到未支付账单页面，发送账单其实就是更新订单流程中的一个状态。
     * </p>
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return true
     */
    @Operation(summary = "发送账单信息")
    @GetMapping("/sendOrderBillInfo/{orderId}/{driverId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId, @PathVariable Long driverId) {
        return Result.ok(orderInfoService.sendOrderBillInfo(orderId, driverId));
    }

    /**
     * 获取订单支付信息
     *
     * @param orderNo    订单编号
     * @param customerId 用户id
     * @return 订单支付信息
     */
    @Operation(summary = "获取订单支付信息")
    @GetMapping("/getOrderPayVo/{orderNo}/{customerId}")
    public Result<OrderPayVo> getOrderPayVo(@PathVariable String orderNo, @PathVariable Long customerId) {
        return Result.ok(orderInfoService.getOrderPayVo(orderNo, customerId));
    }

    /**
     * 更改订单支付状态
     *
     * @param orderNo 订单编号
     * @return true
     */
    @Operation(summary = "更改订单支付状态")
    @GetMapping("/updateOrderPayStatus/{orderNo} ")
    public Result<Boolean> updateOrderPayStatus(@PathVariable String orderNo) {
        return Result.ok(orderInfoService.updateOrderPayStatus(orderNo));
    }

    @Operation(summary = "查询订单的系统奖励")
    @GetMapping("/getOrderRewardFee/{orderNo} ")
    public Result<OrderRewardVo> getOrderRewardFee(@PathVariable String orderNo) {
        return Result.ok(orderInfoService.getOrderRewardFee(orderNo));
    }

    @Operation(summary = "更新订单优惠券金额")
    @GetMapping("/updateCouponAmount/{orderId}/{couponAmount}")
    public Result<Boolean> updateCouponAmount(@PathVariable Long orderId, @PathVariable BigDecimal couponAmount) {
        return Result.ok(orderInfoService.updateCouponAmount(orderId, couponAmount));
    }

}

