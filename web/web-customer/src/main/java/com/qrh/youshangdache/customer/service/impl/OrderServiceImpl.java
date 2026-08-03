package com.qrh.youshangdache.customer.service.impl;

import com.qrh.youshangdache.common.execption.GuiguException;
import com.qrh.youshangdache.common.result.ResultCodeEnum;
import com.qrh.youshangdache.coupon.CouponFeignClient;
import com.qrh.youshangdache.customer.CustomerInfoFeignClient;
import com.qrh.youshangdache.customer.service.OrderService;
import com.qrh.youshangdache.dispatch.NewOrderFeignClient;
import com.qrh.youshangdache.driver.DriverInfoFeignClient;
import com.qrh.youshangdache.map.LocationFeignClient;
import com.qrh.youshangdache.map.MapFeignClient;
import com.qrh.youshangdache.map.WxPayFeignClient;
import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.enums.OrderStatusEnum;
import com.qrh.youshangdache.model.enums.PayWayEnum;
import com.qrh.youshangdache.model.form.coupon.UseCouponForm;
import com.qrh.youshangdache.model.form.customer.ExpectOrderForm;
import com.qrh.youshangdache.model.form.customer.SubmitOrderForm;
import com.qrh.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.qrh.youshangdache.model.form.order.OrderInfoForm;
import com.qrh.youshangdache.model.form.payment.CreateWxPaymentForm;
import com.qrh.youshangdache.model.form.payment.PaymentInfoForm;
import com.qrh.youshangdache.model.form.rules.FeeRuleRequestForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.customer.ExpectOrderVo;
import com.qrh.youshangdache.model.vo.dispatch.NewOrderTaskVo;
import com.qrh.youshangdache.model.vo.driver.DriverInfoVo;
import com.qrh.youshangdache.model.vo.map.DrivingLineVo;
import com.qrh.youshangdache.model.vo.map.OrderLocationVo;
import com.qrh.youshangdache.model.vo.map.OrderServiceLastLocationVo;
import com.qrh.youshangdache.model.vo.order.OrderBillVo;
import com.qrh.youshangdache.model.vo.order.OrderInfoVo;
import com.qrh.youshangdache.model.vo.order.OrderPayVo;
import com.qrh.youshangdache.model.vo.payment.WxPrepayVo;
import com.qrh.youshangdache.model.vo.rules.FeeRuleResponseVo;
import com.qrh.youshangdache.order.OrderInfoFeignClient;
import com.qrh.youshangdache.rules.FeeRuleFeignClient;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {
    @Resource
    private MapFeignClient mapFeignClient;
    @Resource
    private FeeRuleFeignClient feeRuleFeignClient;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private NewOrderFeignClient newOrderFeignClient;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;
    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;
    @Resource
    private WxPayFeignClient wxPayFeignClient;
    @Resource
    private CouponFeignClient couponFeignClient;


    @Override
    public Boolean queryPayStatus(String orderNo) {
        return wxPayFeignClient.queryPayStatus(orderNo).getData();
    }

    @Override
    public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
        //1.获取订单支付相关信息
        OrderPayVo orderPayVo = orderInfoFeignClient.getOrderPayVo(createWxPaymentForm.getOrderNo(), createWxPaymentForm.getCustomerId()).getData();
        //判断是否在未支付状态
        if (orderPayVo.getStatus().equals(OrderStatusEnum.ORDER_UNPAID)) {
            throw new GuiguException(ResultCodeEnum.EXIST_UNPAID_ORDER);
        }

        //2.获取乘客微信openId
        String customerOpenId = customerInfoFeignClient.getCustomerOpenId(orderPayVo.getCustomerId()).getData();

        //3.获取司机微信openId
        String driverOpenId = driverInfoFeignClient.getDriverOpenId(orderPayVo.getDriverId()).getData();

        BigDecimal couponAmount = null;
        if (null == orderPayVo.getCouponAmount() &&
                null != createWxPaymentForm.getCustomerCouponId() &&
                createWxPaymentForm.getCustomerCouponId() != 0) {
            UseCouponForm useCouponForm = new UseCouponForm();
            useCouponForm.setOrderId(orderPayVo.getOrderId());
            useCouponForm.setCustomerCouponId(createWxPaymentForm.getCustomerCouponId());
            useCouponForm.setOrderAmount(orderPayVo.getPayAmount());
            useCouponForm.setCustomerId(createWxPaymentForm.getCustomerId());
            couponAmount = couponFeignClient.useCoupon(useCouponForm).getData();
        }
        //更新订单支付金额
        BigDecimal payAmount = orderPayVo.getPayAmount();
        if (couponAmount != null) {
            Boolean aBoolean = orderInfoFeignClient.updateCouponAmount(orderPayVo.getOrderId(), couponAmount).getData();
            //当前支付金额
            payAmount = payAmount.subtract(couponAmount);
        }

        //4.封装微信下单对象，微信支付只关注以下订单属性
        PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
        paymentInfoForm.setCustomerOpenId(customerOpenId);
        paymentInfoForm.setDriverOpenId(driverOpenId);
        paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());
        paymentInfoForm.setAmount(payAmount);
        paymentInfoForm.setContent(orderPayVo.getContent());
        paymentInfoForm.setPayWay(PayWayEnum.WECHAT);
        return wxPayFeignClient.createWxPayment(paymentInfoForm).getData();
    }

    /**
     * 获取乘客订单分页列表
     *
     * @param pageParam  分页参数对象
     * @param customerId 用户id
     * @return 订单分页
     */
    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        return orderInfoFeignClient.findCustomerOrderPage(customerId, pageParam.getPages(), pageParam.getSize()).getData();
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        return locationFeignClient.getOrderServiceLastLocation(orderId).getData();
    }

    /**
     * 计算最佳驾驶路线
     *
     * @param calculateDrivingLineForm
     * @return 路线
     */
    @Override
    public DrivingLineVo calculateDriverLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }

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
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        return locationFeignClient.getCacheOrderLocation(orderId).getData();
    }

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
    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfoByOrderId(orderId).getData();
        if (!orderInfo.getCustomerId().equals(customerId)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverInfoFeignClient.getDriverInfo(orderInfo.getDriverId()).getData();
    }

    /**
     * 获取执行中的订单
     *
     * @param orderId    订单id
     * @param customerId 用户id
     * @return 执行中的订单的数据
     */
    @Override
    public OrderInfoVo getOrderInfoByOrderId(Long orderId, Long customerId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfoByOrderId(orderId).getData();
        if (!orderInfo.getCustomerId().equals(customerId)) throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);

        DriverInfoVo driverInfoVo = null;
        Long driverId = orderInfo.getDriverId();
        if (driverId != null) {
            driverInfoVo = driverInfoFeignClient.getDriverInfo(driverId).getData();
        }
        OrderBillVo orderBillVo = null;
        if (orderInfo.getStatus().compareTo(OrderStatusEnum.ORDER_UNPAID) >= 0) {
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
        }
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        BeanUtils.copyProperties(orderInfo, orderInfoVo);
        orderInfoVo.setOrderId(orderId);
        orderInfoVo.setDriverInfoVo(driverInfoVo);
        orderInfoVo.setOrderBillVo(orderBillVo);
        return orderInfoVo;
    }

    /**
     * 乘客下完单后，订单状态为1（等待接单），乘客端小程序会轮询订单状态，当订单状态为2（司机已接单）时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     *
     * @param orderId 订单id
     * @return 订单状态代号
     */
    @Override
    public OrderStatusEnum getOrderStatus(Long orderId) {
        return orderInfoFeignClient.getOrderStatus(orderId).getData();
    }

    /**
     * 预估订单费用
     *
     * @param expectOrderForm
     * @return
     */
    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
        //计算驾驶线路
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(expectOrderForm, calculateDrivingLineForm);
        DrivingLineVo drivingLineVo = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();

        //计算订单费用
        FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
        calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
        calculateOrderFeeForm.setStartTime(new Date());
        calculateOrderFeeForm.setWaitMinute(0);
        FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm).getData();

        //预估订单实体
        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        expectOrderVo.setDrivingLineVo(drivingLineVo);
        expectOrderVo.setFeeRuleResponseVo(feeRuleResponseVo);
        return expectOrderVo;
    }

    /**
     * 乘客提交打车订单
     *
     * <p>
     * 乘客提交打车订单后，开始计算预估费用，将数据保存到数据库，然后开启任务调度
     * </p>
     *
     * @param submitOrderForm 订单信息对象
     * @return 订单id
     */
    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {
        //1.重新计算驾驶线路
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(submitOrderForm, calculateDrivingLineForm);
        DrivingLineVo drivingLineVo = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();

        //2.重新计算订单费用
        FeeRuleRequestForm calculateOrderFeeForm = new FeeRuleRequestForm();
        calculateOrderFeeForm.setDistance(drivingLineVo.getDistance());
        calculateOrderFeeForm.setStartTime(new Date());
        calculateOrderFeeForm.setWaitMinute(0);
        FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(calculateOrderFeeForm).getData();

        //3.封装订单信息对象
        OrderInfoForm orderInfoForm = new OrderInfoForm();
        //订单位置信息
        BeanUtils.copyProperties(submitOrderForm, orderInfoForm);
        //预估里程和费用
        orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
        orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());

        //4.保存订单信息
        Long orderId = orderInfoFeignClient.saveOrderInfo(orderInfoForm).getData();

        //   启动任务调度
        NewOrderTaskVo newOrderTaskVo = NewOrderTaskVo.builder()
                .orderId(orderId)
                .startLocation(orderInfoForm.getStartLocation())
                .startPointLongitude(orderInfoForm.getEndPointLongitude())
                .startPointLatitude(orderInfoForm.getStartPointLatitude())
                .endLocation(orderInfoForm.getEndLocation())
                .endPointLongitude(orderInfoForm.getEndPointLongitude())
                .endPointLatitude(orderInfoForm.getEndPointLatitude())
                .expectAmount(orderInfoForm.getExpectAmount())
                .expectDistance(orderInfoForm.getExpectDistance())
                .expectTime(drivingLineVo.getDuration())
                .favourFee(orderInfoForm.getFavourFee())
                .createTime(new Date())
                .build();

        Long jobId = newOrderFeignClient.addAndStartTask(newOrderTaskVo).getData();

        return orderId;
    }
}
