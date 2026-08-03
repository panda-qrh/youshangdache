package com.qrh.youshangdache.driver.service.impl;

import com.qrh.youshangdache.common.constant.SystemConstant;
import com.qrh.youshangdache.common.execption.GuiguException;
import com.qrh.youshangdache.common.result.ResultCodeEnum;
import com.qrh.youshangdache.common.util.LocationUtil;
import com.qrh.youshangdache.dispatch.NewOrderFeignClient;
import com.qrh.youshangdache.driver.service.OrderService;
import com.qrh.youshangdache.map.LocationFeignClient;
import com.qrh.youshangdache.map.MapFeignClient;
import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.enums.OrderStatusEnum;
import com.qrh.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.qrh.youshangdache.model.form.order.OrderFeeForm;
import com.qrh.youshangdache.model.form.order.StartDriveForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderBillForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderCartForm;
import com.qrh.youshangdache.model.form.rules.FeeRuleRequestForm;
import com.qrh.youshangdache.model.form.rules.ProfitsharingRuleRequestForm;
import com.qrh.youshangdache.model.form.rules.RewardRuleRequestForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.map.DrivingLineVo;
import com.qrh.youshangdache.model.vo.map.OrderLocationVo;
import com.qrh.youshangdache.model.vo.map.OrderServiceLastLocationVo;
import com.qrh.youshangdache.model.vo.order.*;
import com.qrh.youshangdache.model.vo.rules.FeeRuleResponseVo;
import com.qrh.youshangdache.model.vo.rules.ProfitsharingRuleResponseVo;
import com.qrh.youshangdache.model.vo.rules.RewardRuleResponseVo;
import com.qrh.youshangdache.order.OrderInfoFeignClient;
import com.qrh.youshangdache.rules.FeeRuleFeignClient;
import com.qrh.youshangdache.rules.ProfitsharingRuleFeignClient;
import com.qrh.youshangdache.rules.RewardRuleFeignClient;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private NewOrderFeignClient newOrderFeignClient;
    @Resource
    private MapFeignClient mapFeignClient;
    @Autowired
    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private FeeRuleFeignClient feeRuleFeignClient;
    @Resource
    private RewardRuleFeignClient rewardRuleFeignClient;
    @Resource
    private ProfitsharingRuleFeignClient profitsharingRuleFeignClient;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

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
    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        return orderInfoFeignClient.sendOrderBillInfo(orderId, driverId).getData();
    }

    /**
     * 获取司机订单分页列表
     *
     * @param pageParam 分页参数
     * @param driverId  司机id
     * @return 订单分页
     */
    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        return orderInfoFeignClient.findDriverOrderPage(driverId, pageParam.getPages(), pageParam.getSize()).getData();
    }

    /**
     * 结束代驾服务更新订单账单
     *
     * @param orderFeeForm 订单费用
     * @return true
     */
    @Override
    @SneakyThrows
    public Boolean endDrive(OrderFeeForm orderFeeForm) {
        //1.获取订单信息
        CompletableFuture<OrderInfo> orderInfoCF = CompletableFuture.supplyAsync(
                () -> orderInfoFeignClient.getOrderInfoByOrderId(orderFeeForm.getOrderId()).getData(),
                threadPoolExecutor);

        //2.防止刷单，计算司机的经纬度与代驾的终点经纬度是否在2公里范围内
        CompletableFuture<OrderServiceLastLocationVo> orderServiceLastLocationVoCF = CompletableFuture.supplyAsync(
                () -> locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData(),
                threadPoolExecutor);

        //合并
        CompletableFuture.allOf(orderInfoCF, orderServiceLastLocationVoCF).join();

        //获取数据
        OrderInfo orderInfo = orderInfoCF.get(10, TimeUnit.SECONDS);
        OrderServiceLastLocationVo orderServiceLastLocationVo = orderServiceLastLocationVoCF.get();

        //司机的位置与代驾终点位置的距离
        double distance = LocationUtil.getDistance(
                orderInfo.getEndPointLatitude(),
                orderInfo.getEndPointLongitude(),
                orderServiceLastLocationVo.getLatitude(),
                orderServiceLastLocationVo.getLongitude()
        );
        if (distance > SystemConstant.DRIVER_START_LOCATION_DISTANCE) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTANCE_ERROR);
        }

        //3.计算订单实际里程
        CompletableFuture<BigDecimal> realDistanceCF = CompletableFuture.supplyAsync(
                () -> locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData(),
                threadPoolExecutor);

        //4.计算代驾实际费用
        CompletableFuture<FeeRuleResponseVo> feeRuleResponseVoCF = realDistanceCF.thenApplyAsync((realDistance) -> {
            Integer waitMinute = Math.abs((int) ((orderInfo.getArriveTime().getTime() - orderInfo.getAcceptTime().getTime()) / (1000 * 60)));
            FeeRuleRequestForm feeRuleRequestForm=FeeRuleRequestForm.builder()
                    .distance(realDistance)
                    .startTime(orderInfo.getStartServiceTime())
                    .waitMinute(waitMinute)
                    .build();
            FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
            //订单总金额 需加上 路桥费、停车费、其他费用、乘客好处费
            BigDecimal totalAmount = feeRuleResponseVo.getTotalAmount()
                    .add(orderFeeForm.getTollFee())
                    .add(orderFeeForm.getParkingFee())
                    .add(orderFeeForm.getOtherFee())
                    .add(orderInfo.getFavourFee());
            feeRuleResponseVo.setTotalAmount(totalAmount);
            return feeRuleResponseVo;
        }, threadPoolExecutor);

        //5.计算系统奖励
        //5.1.获取订单数
        CompletableFuture<Long> orderNumCF = CompletableFuture.supplyAsync(() -> {
            String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
            String endTime = new DateTime(orderInfo.getEndServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
            return orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
        }, threadPoolExecutor);

        //5.2.封装参数
        CompletableFuture<RewardRuleResponseVo> rewardRuleResponseVoCF = orderNumCF.thenApplyAsync((orderNum) -> {
            RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
            rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
            rewardRuleRequestForm.setOrderNum(orderNum);
            //5.3.执行
            return rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();
        }, threadPoolExecutor);

        //6.计算分账信息
        CompletableFuture<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoCF = feeRuleResponseVoCF.thenCombineAsync(
                orderNumCF,
                (feeRuleResponseVo, orderNum) -> {
                    ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
                    profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
                    profitsharingRuleRequestForm.setOrderNum(orderNum);
                    return profitsharingRuleFeignClient.calculateProfitSharingFee(profitsharingRuleRequestForm).getData();
                },
                threadPoolExecutor);

        CompletableFuture.allOf(orderServiceLastLocationVoCF,
                realDistanceCF,
                feeRuleResponseVoCF,
                orderNumCF,
                rewardRuleResponseVoCF,
                profitsharingRuleResponseVoCF
        ).get(10, TimeUnit.SECONDS);

        //获取执行结果
        BigDecimal realDistance = realDistanceCF.get();
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoCF.get();
        RewardRuleResponseVo rewardRuleResponseVo = rewardRuleResponseVoCF.get();
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleResponseVoCF.get();

        //7.封装更新订单账单相关实体对象
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());
        updateOrderBillForm.setRealDistance(realDistance);

        BeanUtils.copyProperties(rewardRuleResponseVo, updateOrderBillForm);
        BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);
        BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());

        //8.结束代驾更新账单
        orderInfoFeignClient.endDrive(updateOrderBillForm);
        return true;
    }

    /**
     * 开始代驾服务
     *
     * @param startDriveForm
     * @return true
     */
    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        return orderInfoFeignClient.startDrive(startDriveForm).getData();
    }

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
    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        return orderInfoFeignClient.updateOrderCart(updateOrderCartForm).getData();
    }

    /**
     * 司机到达起始点
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return true
     */
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfoByOrderId(orderId).getData();
        OrderLocationVo orderLocationVo = locationFeignClient.getCacheOrderLocation(orderId).getData();
        double distance = LocationUtil.getDistance(orderInfo.getStartPointLatitude().doubleValue(),
                orderInfo.getStartPointLongitude().doubleValue(),
                orderLocationVo.getLatitude().doubleValue(),
                orderLocationVo.getLongitude().doubleValue());
        if (distance > SystemConstant.DRIVER_START_LOCATION_DISTANCE) {
            throw new GuiguException(ResultCodeEnum.DRIVER_START_LOCATION_DISTANCE_ERROR);
        }
        return orderInfoFeignClient.driverArriveStartLocation(orderId, driverId).getData();
    }

    /**
     * 计算最佳驾驶路线-司乘同显
     *
     * @param calculateDrivingLineForm 起点坐标和终点坐标对象
     * @return 路线
     */
    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapFeignClient.calculateDrivingLine(calculateDrivingLineForm).getData();
    }

    /**
     * 获取执行中的订单
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return 执行中的订单的数据
     */
    @Override
    public OrderInfoVo getOrderInfoByOrderId(Long orderId, Long driverId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfoByOrderId(orderId).getData();
        if (!orderInfo.getCustomerId().equals(driverId)) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        OrderBillVo orderBillVo = null;
        OrderProfitsharingVo orderProfitsharingVo = null;
        if (orderInfo.getStatus().compareTo(OrderStatusEnum.END_SERVICE)>=0) {
            orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
            orderProfitsharingVo = orderInfoFeignClient.getOrderProfitsharing(orderId).getData();
        }
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        BeanUtils.copyProperties(orderInfo, orderInfoVo);
        orderInfoVo.setOrderId(orderId);
        orderInfoVo.setOrderBillVo(orderBillVo);
        orderInfoVo.setOrderProfitsharingVo(orderProfitsharingVo);
        return orderInfoVo;
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
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        return orderInfoFeignClient.robNewOrder(driverId, orderId).getData();
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
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        return orderInfoFeignClient.searchDriverCurrentOrder(driverId).getData();
    }

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
    @Override
    public OrderStatusEnum getOrderStatus(Long orderId) {
        return orderInfoFeignClient.getOrderStatus(orderId).getData();
    }

    /**
     * 查询司机的最新订单数据
     *
     * @param driverId 司机id
     * @return
     */
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        return newOrderFeignClient.findNewOrderQueueData(driverId).getData();
    }

    /**
     * 代驾服务：获取订单服务最后一个位置信息
     *
     * @param orderId 订单id
     * @return 最后一个坐标位置
     */
    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        return locationFeignClient.getOrderServiceLastLocation(orderId).getData();
    }
}
