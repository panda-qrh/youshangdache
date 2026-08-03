package com.qrh.youshangdache.driver.service;

import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.enums.OrderStatusEnum;
import com.qrh.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.qrh.youshangdache.model.form.order.OrderFeeForm;
import com.qrh.youshangdache.model.form.order.StartDriveForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderCartForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.map.DrivingLineVo;
import com.qrh.youshangdache.model.vo.map.OrderServiceLastLocationVo;
import com.qrh.youshangdache.model.vo.order.CurrentOrderInfoVo;
import com.qrh.youshangdache.model.vo.order.NewOrderDataVo;
import com.qrh.youshangdache.model.vo.order.OrderInfoVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface OrderService {

    /**
     * 查询订单状态
     *
     * <p>
     * 乘客下完单后，订单状态为1（等待接单），乘客端小程序会轮询订单状态，当订单状态为2（司机已接单）时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     * </p>
     *
     * @param orderId 订单id
     * @return 订单状态代号
     */
    OrderStatusEnum getOrderStatus(Long orderId);

    /**
     * 查询司机的最新订单数据
     *
     * @param driverId 司机id
     * @return
     */
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

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
     * 获取执行中的订单
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return 执行中的订单的数据
     */
    OrderInfoVo getOrderInfoByOrderId(Long orderId, Long driverId);

    /**
     * 计算最佳驾驶路线-司乘同显
     *
     * @param calculateDrivingLineForm 起点坐标和终点坐标对象
     * @return 路线
     */
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

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
     * <p>
     * 司机到达代驾起始点，联系了乘客，见到了代驾车辆，要拍照与录入车辆信息
     * </p>
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
     * 结束代驾服务更新订单账单
     *
     * @param orderFeeForm 订单费用
     * @return true
     */
    Boolean endDrive(OrderFeeForm orderFeeForm);

    /**
     * 获取司机订单分页列表
     *
     * @param pageParam 分页参数
     * @param driverId  司机id
     * @return 订单分页
     */
    PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);

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
     * 代驾服务：获取订单服务最后一个位置信息
     *
     * @param orderId 订单id
     * @return 最后一个坐标位置
     */
    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);
}
