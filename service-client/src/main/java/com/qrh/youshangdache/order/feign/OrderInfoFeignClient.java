package com.qrh.youshangdache.order.feign;


import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.enums.OrderStatusEnum;
import com.qrh.youshangdache.model.form.order.OrderInfoForm;
import com.qrh.youshangdache.model.form.order.StartDriveForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderBillForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderCartForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.order.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;


@FeignClient(value = "service-order", path = "/order/info")
public interface OrderInfoFeignClient {
    /**
     * 保存订单信息
     *
     * @param orderInfoForm 订单信息对象
     * @return 订单id
     */
    @PostMapping("/saveOrderInfo")
    Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm);

    /**
     * 乘客下完单后，订单状态为1（等待接单），乘客端小程序会轮询订单状态，当订单状态为2（司机已接单）时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     *
     * @param orderId 订单id
     * @return 订单状态代号
     */
    @GetMapping("/getOrderStatus/{orderId}")
    Result<OrderStatusEnum> getOrderStatus(@PathVariable("orderId") Long orderId);

    /**
     * 乘客如果已经下过单了，而且这个订单在执行中，没有结束，
     * 那么乘客是不可以再下单的，页面会弹出层，进入执行中的订单。
     *
     * @param customerId 用户id
     * @return 当前用户正在进行的订单信息
     */
    @GetMapping("/searchCustomerCurrentOrder/{customerId}")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable Long customerId);

    /**
     * 查找司机端当前订单
     *
     * <p>
     * 司机只要有执行中的订单，没有结束，那么司机是不可以接单的，页面会弹出层，进入执行中的订单
     * </p>
     *
     * @return 司机当前正在执行的订单数据
     */
    @GetMapping("/searchDriverCurrentOrder/{driverId}")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable Long driverId);

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
    @GetMapping("/robNewOrder/{driverId}/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long driverId, @PathVariable Long orderId);

    /**
     * 获取执行中的订单
     *
     * @param orderId 订单id
     * @return 执行中的订单的数据
     */
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfo> getOrderInfoByOrderId(@PathVariable Long orderId);

    /**
     * 司机到达起始位置
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return true
     */
    @GetMapping("/driverArriveStartLocation/{orderId}/{driverId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId, @PathVariable Long driverId);

    /**
     * 更新代驾车辆信息
     *
     * <p>
     * 司机到达代驾起始点，联系了乘客，见到了代驾车辆，要拍照与录入车辆信息
     * </p>
     *
     * @param updateOrderCartForm
     * @return true
     */
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm);
    /**
     * 开始代驾服务
     *
     * @param startDriveForm
     * @return true
     */
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm);
    /**
     * 根据时间段获取订单数
     *
     * @param startTime 起始时间
     * @param endTime   终止时间
     * @return 订单数量
     */
    @GetMapping("/getOrderNumByTime/{startTime}/{endTime}")
    public Result<Long> getOrderNumByTime(@PathVariable String startTime, @PathVariable String endTime);
    /**
     * 结束代驾服务，更新订单账单
     *
     * @param updateOrderBillForm 账单
     * @return true
     */
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody UpdateOrderBillForm updateOrderBillForm);
    /**
     * 获取乘客订单分页列表
     *
     * @param customerId 用户id
     * @param limit 页限制
     * @param page 页码
     * @return 订单分页
     */
    @GetMapping("/findCustomerOrderPage/{customerId}/{page}/{limit}")
    public Result<PageVo> findCustomerOrderPage(@PathVariable Long customerId,
                                                @PathVariable Long limit,
                                                @PathVariable Long page);
    /**
     * 获取司机订单分页列表
     *
     * @param driverId 司机id
     * @param limit 页限制
     * @param page 页码
     * @return 订单分页
     */
    @GetMapping("/findDriverOrderPage/{driverId}/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(@PathVariable Long driverId,
                                              @PathVariable Long limit,
                                              @PathVariable Long page);

    /**
     * 根据订单id获取实际账单信息
     *
     * @param orderId 订单id
     * @return 该订单的账单信息
     */
    @GetMapping("/getOrderBillInfo/{orderId}")
    public Result<OrderBillVo> getOrderBillInfo(@PathVariable Long orderId);
    /**
     * 根据订单id获取实际分账信息
     *
     * @param orderId 订单id
     * @return 订单分账数据
     */
    @GetMapping("/getOrderProfitsharing/{orderId}")
    public Result<OrderProfitsharingVo> getOrderProfitsharing(@PathVariable Long orderId);
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
    @GetMapping("/sendOrderBillInfo/{orderId}/{driverId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId, @PathVariable Long driverId);
    /**
     * 获取订单支付信息
     * @param orderNo 订单编号
     * @param customerId 用户id
     * @return 订单支付信息
     */
    @GetMapping("/getOrderPayVo/{orderNo}/{customerId}")
    public Result<OrderPayVo> getOrderPayVo(@PathVariable String orderNo, @PathVariable Long customerId);
    /**
     * 更改订单支付状态
     *
     * @param orderNo 订单编号
     * @return true
     */
    @GetMapping("/updateOrderPayStatus/{orderNo} ")
    public Result<Boolean> updateOrderPayStatus(@PathVariable String orderNo);

    @GetMapping("/getOrderRewardFee/{orderNo} ")
    public Result<OrderRewardVo> getOrderRewardFee(@PathVariable String orderNo);

    @GetMapping("/updateCouponAmount/{orderId}/{couponAmount}")
    public Result<Boolean> updateCouponAmount(@PathVariable Long orderId, @PathVariable BigDecimal couponAmount);


}