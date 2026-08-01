package com.qrh.youshangdache.customer.service;

import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.enums.OrderStatusEnum;
import com.qrh.youshangdache.model.form.customer.ExpectOrderForm;
import com.qrh.youshangdache.model.form.customer.SubmitOrderForm;
import com.qrh.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.qrh.youshangdache.model.form.payment.CreateWxPaymentForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.customer.ExpectOrderVo;
import com.qrh.youshangdache.model.vo.driver.DriverInfoVo;
import com.qrh.youshangdache.model.vo.map.DrivingLineVo;
import com.qrh.youshangdache.model.vo.map.OrderLocationVo;
import com.qrh.youshangdache.model.vo.map.OrderServiceLastLocationVo;
import com.qrh.youshangdache.model.vo.order.OrderInfoVo;
import com.qrh.youshangdache.model.vo.payment.WxPrepayVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface OrderService {

    ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm);
    /**
     * 乘客提交打车订单
     * @param submitOrderForm 订单信息对象
     * @return 订单号
     */
    Long submitOrder(SubmitOrderForm submitOrderForm);
    /**
     * 乘客下完单后，订单状态为1（等待接单），乘客端小程序会轮询订单状态，当订单状态为2（司机已接单）时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     * @param orderId 订单id
     * @return 订单状态代号
     */
    OrderStatusEnum getOrderStatus(Long orderId);
    /**
     * 获取执行中的订单
     * @param orderId 订单id
     * @param customerId 用户id
     * @return 执行中的订单的数据
     */
    OrderInfoVo getOrderInfoByOrderId(Long orderId, Long customerId);

    /**
     * 根据订单id获取司机基本信息
     *
     * <p>
     * 乘客端进入司乘同显页面，需要加载司机的基本信息，显示司机的姓名、头像及驾龄等信息
     * </p>
     *
     * @param orderId    订单id
     * @param customerId 用户id
     * @return 司机基本信息
     */
    DriverInfoVo getDriverInfo(Long orderId,Long customerId);

    /**
     * 司机赶往代驾起始点，更新订单经纬度位置
     *
     * <p>
     * 从redis中获取订单的坐标
     * </p>
     *
     * @param orderId 订单id
     * @return 订单的坐标
     */
    OrderLocationVo getCacheOrderLocation(Long orderId );
    /**
     * 计算最佳驾驶路线
     *
     * @param calculateDrivingLineForm
     * @return 路线
     */
    DrivingLineVo calculateDriverLine(CalculateDrivingLineForm calculateDrivingLineForm);

    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);
    /**
     * 获取乘客订单分页列表
     *
     * @param pageParam 分页参数对象
     * @param customerId 用户id
     * @return 订单分页
     */
    PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);

    WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm);

    Boolean queryPayStatus(String orderNo);
}
