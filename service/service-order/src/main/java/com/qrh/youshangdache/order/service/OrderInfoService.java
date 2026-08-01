package com.qrh.youshangdache.order.service;

import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.enums.OrderStatusEnum;
import com.qrh.youshangdache.model.form.order.OrderInfoForm;
import com.qrh.youshangdache.model.form.order.StartDriveForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderBillForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderCartForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qrh.youshangdache.model.vo.order.*;

import java.math.BigDecimal;

public interface OrderInfoService extends IService<OrderInfo> {
    /**
     * 保存订单信息
     *
     * @param orderInfoForm 订单信息对象
     * @return 订单id
     */
    Long saveOrderInfo(OrderInfoForm orderInfoForm);

    /**
     * 乘客下完单后，订单状态为1，乘客端小程序会轮询订单状态，当订单状态为2时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     *
     * @param orderId 订单id
     * @return 订单状态代号
     */
    OrderStatusEnum getOrderStatus(Long orderId);
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
    Boolean robNewOrder(Long driverId, Long orderId);

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
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);
    /**
     * 查找司机端当前订单
     *
     * <p>
     * 司机只要有执行中的订单，没有结束，那么司机是不可以接单的，页面会弹出层，进入执行中的订单
     * </p>
     *
     * @return 司机当前正在执行的订单数据
     */
    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);
    /**
     * 司机到达起始点
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return true
     */
    Boolean driverArriveStartLocation(Long orderId, Long driverId);
    /**
     * 更新代驾车辆信息
     *
     * @param updateOrderCartForm
     * @return true
     */
    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);
    /**
     * 开始代驾服务
     *
     * @param startDriveForm
     * @return true
     */
    Boolean startDrive(StartDriveForm startDriveForm);
    /**
     * 根据时间段获取订单数
     *
     * @param startTime 起始时间
     * @param endTime   终止时间
     * @return 订单数量
     */
    Long getOrderNumByTime(String startTime, String endTime);
    /**
     * 结束代驾服务更新订单账单
     *
     * @param updateOrderBillForm 账单
     * @return true
     */
    Boolean endDrive(UpdateOrderBillForm updateOrderBillForm);
    /**
     * 获取司机订单分页列表
     *
     * @param pageParam 分页参数
     * @param  customerId 用户id
     * @return 订单分页
     */
    PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);
    /**
     * 获取司机订单分页列表
     *
     * @param pageParam 分页参数
     * @param driverId 司机id
     * @return 订单分页
     */
    PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);
    /**
     * 根据订单id获取实际账单信息
     *
     * @param orderId 订单id
     * @return 该订单的账单信息
     */
    OrderBillVo getOrderBillInfo(Long orderId);
    /**
     * 根据订单id获取实际分账信息
     *
     * @param orderId 订单id
     * @return 订单分账数据
     */
    OrderProfitsharingVo getOrderProfitsharing(Long orderId);
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
    Boolean sendOrderBillInfo(Long orderId, Long driverId);
    /**
     * 获取订单支付信息
     * @param orderNo 订单编号
     * @param customerId 用户id
     * @return 订单支付信息
     */
    OrderPayVo getOrderPayVo(String orderNo, Long customerId);
    /**
     * 更改订单支付状态
     *
     * @param orderNo 订单编号
     * @return true
     */
    Boolean updateOrderPayStatus(String orderNo);

    OrderRewardVo getOrderRewardFee(String orderNo);

    /**
     * 根据订单Id 取消订单
     *
     * @param orderId 订单id
     */
    void orderCancel(Long orderId);

    Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount);

    /**
     * 系统取消订单
     *
     * @param orderId 订单id
     */
    void systemCancelOrder(Long orderId);

    void updateProfitsharingStatus(String orderNo);


}
