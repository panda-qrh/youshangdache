package com.youshangdache.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youshangdache.common.mq.constant.ExchangeConst;
import com.youshangdache.common.constant.RedisConstant;
import com.youshangdache.common.mq.constant.RoutingConst;
import com.youshangdache.common.constant.SystemConstant;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.common.mq.service.RabbitService;
import com.youshangdache.model.entity.order.*;
import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.model.enums.OrderOperatorEnum;
import com.youshangdache.model.enums.OrderStatusEnum;
import com.youshangdache.model.enums.ProfitsharingStatusEnum;
import com.youshangdache.model.form.order.OrderInfoForm;
import com.youshangdache.model.form.order.StartDriveForm;
import com.youshangdache.model.form.order.UpdateOrderBillForm;
import com.youshangdache.model.form.order.UpdateOrderCartForm;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.order.*;
import com.youshangdache.order.mapper.OrderBillMapper;
import com.youshangdache.order.mapper.OrderInfoMapper;
import com.youshangdache.order.mapper.OrderProfitsharingMapper;
import com.youshangdache.order.mapper.OrderStatusLogMapper;
import com.youshangdache.order.service.OrderInfoService;
import com.youshangdache.order.service.OrderMonitorService;
import com.youshangdache.order.statemachine.OrderStateMachineService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private OrderInfoMapper orderInfoMapper;
    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OrderMonitorService orderMonitorService;
    @Resource
    private OrderBillMapper orderBillMapper;
    @Resource
    private OrderProfitsharingMapper orderProfitsharingMapper;
    @Resource
    private RabbitService rabbitService;
    /**
     * 订单状态机统一入口：order_info.status 的唯一写入口
     */
    @Resource
    private OrderStateMachineService orderStateMachineService;

    /**
     * 修改分账信息的状态
     *
     * <p>分账成功意味着这一单的钱已经结算完，此时把订单推进到终态"订单已结束"
     * （原来支付完成后订单永远停在"已付款"，ORDER_FINISHED 是个死状态）。</p>
     *
     * @param orderNo 订单编号
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateProfitsharingStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo).select(OrderInfo::getId));
        if (orderInfo == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        OrderProfitsharing updateOrderProfitsharing = new OrderProfitsharing();
        updateOrderProfitsharing.setStatus(ProfitsharingStatusEnum.SHARED);
        orderProfitsharingMapper.update(updateOrderProfitsharing, new LambdaQueryWrapper<OrderProfitsharing>()
                .eq(OrderProfitsharing::getOrderId, orderInfo.getId()));

        // 分账完成 -> 订单结束（幂等：既不是"已付款"也不是重复消息时，状态机会拒绝并在这里被忽略）
        try {
            orderStateMachineService.fireEvent(orderInfo.getId(), OrderEventEnum.FINISH_ORDER,
                    OrderOperatorEnum.SYSTEM, null, "分账完成，订单结束");
        } catch (GuiguException e) {
            if (e.getCode() == ResultCodeEnum.ORDER_STATUS_ILLEGAL_TRANSITION.getCode()
                    || e.getCode() == ResultCodeEnum.ORDER_CONCURRENT_MODIFY.getCode()) {
                log.info("订单 {} 当前状态无需推进到已结束，忽略本次 FINISH_ORDER", orderNo);
            } else {
                throw e;
            }
        }
    }

    /**
     * 系统取消订单（超时无人接单）
     *
     * @param orderId 订单id
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void systemCancelOrder(Long orderId) {
        OrderStatusEnum orderStatus = this.getOrderStatus(orderId);
        if (orderStatus != OrderStatusEnum.WAITING_ACCEPT) {
            //已经不在等待接单了（可能刚被抢单），直接跳过，属于正常竞态
            log.info("订单 {} 当前状态为 {}，跳过超时取消", orderId, orderStatus);
            return;
        }
        try {
            orderStateMachineService.fireEvent(orderId, OrderEventEnum.CANCEL_TIMEOUT,
                    OrderOperatorEnum.SYSTEM, null, "超过15分钟无人接单，系统自动取消");
        } catch (GuiguException e) {
            //与"司机抢单"竞争失败：状态已被改成已接单，本次取消失败是正常结果
            if (e.getCode() == ResultCodeEnum.ORDER_CONCURRENT_MODIFY.getCode()
                    || e.getCode() == ResultCodeEnum.ORDER_STATUS_ILLEGAL_TRANSITION.getCode()) {
                log.info("订单 {} 已被接单，超时取消不再生效", orderId);
                return;
            }
            throw e;
        }
        //删除redis接单标识（key 里必须带上 orderId，否则删的是一个不存在的 key）
        stringRedisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderId);
    }

    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        orderBillMapper.updateCouponAmount(orderId, couponAmount);
        return true;
    }

    /**
     * 超时取消订单（Redisson 延迟队列入口）
     *
     * <p>与 {@link #systemCancelOrder(Long)} 是同一条业务的两个触发通道（MQ 延迟消息 / Redisson 延迟队列），
     * 这里直接复用同一套逻辑，避免两处实现逐渐不一致。</p>
     */
    @Override
    public void orderCancel(Long orderId) {
        this.systemCancelOrder(orderId);
    }

    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderNo)
                .select(OrderInfo::getId, OrderInfo::getDriverId));
        OrderBill orderBill = orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>()
                .eq(OrderBill::getOrderId, orderInfo.getId())
                .select(OrderBill::getRewardFee));
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }

    /**
     * 更改订单支付状态
     *
     * @param orderNo 订单编号
     * @return true
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean updateOrderPayStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));

        //订单存在且已付款（回调可能重复到达，这里做幂等）
        if (orderInfo == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if (orderInfo.getStatus() == OrderStatusEnum.ORDER_PAID) {
            return true;
        }
        //只有"未付款"状态的订单才能置为已付款：交给状态机判断，非法状态会被拒绝
        OrderInfo patch = new OrderInfo();
        patch.setId(orderInfo.getId());
        patch.setPayTime(new Date());
        orderStateMachineService.fireEvent(
                patch,
                OrderEventEnum.PAY_SUCCESS,
                OrderOperatorEnum.SYSTEM,
                null,
                "微信支付成功回调");
        return true;
    }

    /**
     * 获取订单支付信息
     *
     * @param orderNo    订单编号
     * @param customerId 用户id
     * @return 订单支付信息
     */
    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo, customerId);
        if (orderPayVo != null) {
            String content = orderPayVo.getStartLocation() + " 到 " + orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
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
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        //先做归属校验给出明确错误；SQL 里的 driverId 条件作为并发下的原子兜底
        OrderInfo current = orderInfoMapper.selectById(orderId);
        if (current == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if (!driverId.equals(current.getDriverId())) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        OrderInfo patch = new OrderInfo();
        patch.setId(orderId);
        orderStateMachineService.fireEvent(
                patch,
                OrderEventEnum.SEND_BILL,
                OrderOperatorEnum.DRIVER,
                driverId,
                "司机确认账单并发送给乘客",
                wrapper -> wrapper.eq(OrderInfo::getDriverId, driverId));
        return true;
    }

    /**
     * 根据订单id获取实际分账信息
     *
     * @param orderId 订单id
     * @return 订单分账数据
     */
    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<OrderProfitsharing>().eq(OrderProfitsharing::getOrderId, orderId);
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);
        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing, orderProfitsharingVo);
        return orderProfitsharingVo;
    }

    /**
     * 根据订单id获取实际账单信息
     *
     * @param orderId 订单id
     * @return 该订单的账单信息
     */
    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        LambdaQueryWrapper<OrderBill> wrapper = new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, orderId);
        OrderBill orderBill = orderBillMapper.selectOne(wrapper);
        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill, orderBillVo);
        return orderBillVo;
    }

    /**
     * 获取司机订单分页列表
     *
     * @param pageParam
     * @param driverId  司机id
     * @return 分页数据
     */
    @Override
    public PageVo<?> findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectDriverOrderPage(pageParam, driverId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getSize());
    }

    /**
     * 获取乘客订单分页列表
     *
     * @param pageParam  分页参数
     * @param customerId 用户id
     * @return 订单数据
     */
    @Override
    public PageVo<?> findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectCustomerOrderPage(pageParam, customerId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getSize());
    }

    /**
     * 结束代驾服务更新订单账单
     *
     * @param form 账单
     * @return true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean endDrive(UpdateOrderBillForm form) {
        OrderInfo patch = new OrderInfo();
        patch.setId(form.getOrderId());
        patch.setRealAmount(form.getTotalAmount());
        patch.setFavourFee(form.getFavourFee());
        patch.setRealDistance(form.getRealDistance());
        patch.setEndServiceTime(new Date());
        //状态机校验"开始代驾 -> 结束代驾"；CAS 同时保证重复调用不会重复生成账单
        orderStateMachineService.fireEvent(
                patch,
                OrderEventEnum.END_DRIVE,
                OrderOperatorEnum.DRIVER,
                form.getDriverId(),
                "司机结束代驾",
                wrapper -> wrapper.eq(OrderInfo::getDriverId, form.getDriverId()));

        OrderBill orderBill = new OrderBill();
        BeanUtils.copyProperties(form, orderBill);
        orderBill.setPayAmount(form.getTotalAmount());
        orderBill.setDistanceFee(safeMultiply(form.getExceedDistance(), form.getExceedDistancePrice()));
        orderBill.setWaitFee(safeMultiply(form.getExceedWaitMinute(), form.getExceedWaitMinutePrice()));
        orderBill.setLongDistanceFee(safeMultiply(form.getExceedLongDistance(), form.getExceedLongDistancePrice()));
        orderBill.setRewardFee(form.getRewardAmount());
        orderBillMapper.insert(orderBill);

        OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
        BeanUtils.copyProperties(form, orderProfitsharing);
        orderProfitsharing.setRuleId(form.getProfitsharingRuleId());
        orderProfitsharing.setStatus(ProfitsharingStatusEnum.NOT_SHARED);
        orderProfitsharingMapper.insert(orderProfitsharing);

        return true;
    }

    /**
     * 根据时间段获取订单数
     *
     * @param startTime 起始时间
     * @param endTime   终止时间
     * @return 订单数量
     */
    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .ge(OrderInfo::getStartServiceTime, startTime)
                .le(OrderInfo::getEndServiceTime, endTime);
        return orderInfoMapper.selectCount(wrapper);
    }

    /**
     * 开始代驾服务
     *
     * @param startDriveForm
     * @return true
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        //开始代驾允许从"司机已到达"或"已更新车辆信息"流转过来，合法性由状态机判断
        OrderInfo patch = new OrderInfo();
        patch.setId(startDriveForm.getOrderId());
        patch.setStartServiceTime(new Date());
        //只能更新自己的订单
        orderStateMachineService.fireEvent(
                patch,
                OrderEventEnum.START_DRIVE,
                OrderOperatorEnum.DRIVER,
                startDriveForm.getDriverId(),
                "司机开始代驾",
                wrapper -> wrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId()));

        //初始化订单监控统计数据
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);
        return true;
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
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        OrderInfo patch = new OrderInfo();
        patch.setId(updateOrderCartForm.getOrderId());
        patch.setCarLicense(updateOrderCartForm.getCarLicense());
        patch.setCarType(updateOrderCartForm.getCarType());
        patch.setCarFrontUrl(updateOrderCartForm.getCarFrontUrl());
        patch.setCarBackUrl(updateOrderCartForm.getCarBackUrl());
        //合法性：只允许"司机已到达 -> 更新代驾车辆信息"
        orderStateMachineService.fireEvent(
                patch,
                OrderEventEnum.UPDATE_CAR,
                OrderOperatorEnum.DRIVER,
                updateOrderCartForm.getDriverId(),
                "司机录入代驾车辆信息",
                wrapper -> wrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId()));
        return true;
    }

    /**
     * 司机到达起始点
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        OrderInfo patch = new OrderInfo();
        patch.setId(orderId);
        patch.setArriveTime(new Date());
        //合法性：只允许"已接单 -> 司机已到达"
        orderStateMachineService.fireEvent(
                patch,
                OrderEventEnum.DRIVER_ARRIVE,
                OrderOperatorEnum.DRIVER,
                driverId,
                "司机到达上车点",
                wrapper -> wrapper.eq(OrderInfo::getDriverId, driverId));
        return true;
    }

    /**
     * 查询该司机是否有已经进行或未支付的订单信息
     *
     * <p>
     * 订单信息的状态为：已接单、司机已到达、更新代驾车辆信息、开始服务、结束服务、待付款都视为订单未完成，该司机不能在接单
     * </p>
     *
     * @param driverId 司机id
     * @return 当司机当前正在执行但未完成的订单数据
     */
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        //注意：司机侧同样要把"未付款"算作未完成订单，否则司机手上有未支付订单还能继续接单
        OrderStatusEnum[] statusArray = {
                OrderStatusEnum.ACCEPTED,
                OrderStatusEnum.DRIVER_ARRIVED,
                OrderStatusEnum.UPDATE_CAR_INFO,
                OrderStatusEnum.START_SERVICE,
                OrderStatusEnum.END_SERVICE,
                OrderStatusEnum.ORDER_UNPAID
        };
        return queryCurrentOrder(OrderInfo::getDriverId, driverId, statusArray);
    }

    /**
     * 查询该乘客是否有已经进行或未支付的订单信息
     *
     * <p>
     * 订单信息的状态为：已接单、司机已到达、更新代驾车辆信息、开始服务、结束服务、待付款都视为订单未完成，该乘客不能再叫车
     * </p>
     *
     * @param customerId 乘客id
     * @return 当前订单信息
     */
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        OrderStatusEnum[] statusArray = {
                OrderStatusEnum.ACCEPTED,
                OrderStatusEnum.DRIVER_ARRIVED,
                OrderStatusEnum.UPDATE_CAR_INFO,
                OrderStatusEnum.START_SERVICE,
                OrderStatusEnum.END_SERVICE,
                OrderStatusEnum.ORDER_UNPAID
        };
        return queryCurrentOrder(OrderInfo::getCustomerId, customerId, statusArray);
    }

    /**
     * 查询用户当前是否有未完成的订单
     *
     * @param idField     订单关联用户ID的字段
     * @param userId      用户ID
     * @param statusArray 视为未完成的状态列表
     * @return 当前订单信息
     */
    private CurrentOrderInfoVo queryCurrentOrder(SFunction<OrderInfo, ?> idField, Long userId, OrderStatusEnum[] statusArray) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(idField, userId)
                .in(OrderInfo::getStatus, statusArray)
                .orderByDesc(OrderInfo::getId)
                .last(" limit 1"));
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
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
    @Transactional(rollbackFor = {Exception.class})
    public Boolean robNewOrder(Long driverId, Long orderId) {
        String acceptMarkKey = RedisConstant.ORDER_ACCEPT_MARK + orderId;
        // 判断订单是否存在且还在等待接单（接单标识不存在说明已经不在等待接单状态了）
        if (!stringRedisTemplate.hasKey(acceptMarkKey)) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        //抢单锁必须使用独立的 key，不能和业务接单标识共用一个 key：
        //否则临界区内删除接单标识会连带把 Redisson 的锁记录删掉，并发请求会立刻拿到锁，导致同一单被多个司机抢到。
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
        try {
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME, RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!flag) {
                //没有抢到锁，说明别的司机正在接这一单
                throw new GuiguException(ResultCodeEnum.ORDER_SNAP_UP_FAILED);
            }
            //二次校验：持有锁之后再确认一次订单是否仍在等待接单
            if (!stringRedisTemplate.hasKey(acceptMarkKey)) {
                throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
            }

            //状态机判定"等待接单 + 抢单 -> 已接单"，CAS 作为锁之外的最后一道防线
            OrderInfo patch = new OrderInfo();
            patch.setOrderNo(orderId.toString());
            patch.setDriverId(driverId);
            patch.setAcceptTime(new Date());
            try {
                orderStateMachineService.fireEvent(patch, OrderEventEnum.DRIVER_ACCEPT,OrderOperatorEnum.DRIVER, driverId, "司机抢单");
            } catch (GuiguException e) {
                //非法迁移或并发冲突，对抢单来说都统一表达为"抢单失败"
                if (e.getCode() == ResultCodeEnum.ORDER_CONCURRENT_MODIFY.getCode()
                        || e.getCode() == ResultCodeEnum.ORDER_STATUS_ILLEGAL_TRANSITION.getCode()) {
                    throw new GuiguException(ResultCodeEnum.ORDER_SNAP_UP_FAILED);
                }
                throw e;
            }

            //司机抢单成功，说明用户的订单已被司机接单，那就不需要再等待接单了，删除redis中的接单标识
            stringRedisTemplate.delete(acceptMarkKey);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GuiguException(ResultCodeEnum.ORDER_SNAP_UP_FAILED);
        } finally {
            //只能释放当前线程持有的锁，否则会抛 IllegalMonitorStateException 或误释放别人的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 乘客下完单后，订单状态为{@link OrderStatusEnum#WAITING_ACCEPT}，乘客端小程序会轮询订单状态，当订单状态为{@link OrderStatusEnum#ACCEPTED}时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     *
     * @param orderId 订单id
     * @return 订单状态代号
     */
    @Override
    public OrderStatusEnum getOrderStatus(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .select(OrderInfo::getStatus));
        if (null == orderInfo) {
            return OrderStatusEnum.ORDER_NOT_EXIST;
        }
        return orderInfo.getStatus();
    }

    /**
     * 保存订单信息
     *
     * @param orderInfoForm 订单信息对象
     * @return 订单id
     */
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm, orderInfo);
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        orderInfo.setStatus(OrderStatusEnum.WAITING_ACCEPT);
        orderInfo.setOrderNo(orderNo);
        orderInfoMapper.insert(orderInfo);
        //记录日志
        this.log(orderInfo.getOrderNo(), orderInfo.getStatus());

        //延迟取消订单、接单标识都属于"消息/缓存"动作，一旦 DB 事务回滚它们无法撤回，
        //因此统一放到事务提交之后再执行，避免出现"订单不存在但取消消息还在飞"的不一致。
        final Long orderId = orderInfo.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    afterOrderSaved(orderId);
                }
            });
        } else {
            this.afterOrderSaved(orderId);
        }
        return orderId;
    }

    /**
     * 订单落库成功后的后续动作：写入接单标识 + 投递两条延迟取消消息
     *
     * <p>必须在事务提交后调用，保证"订单已存在"与"取消消息已投递"两者一致。</p>
     *
     * @param orderId 订单id
     */
    private void afterOrderSaved(Long orderId) {
        try {
            //接单标识，标识不存在了说明不在等待接单状态了
            stringRedisTemplate.opsForValue()
                    .set(RedisConstant.ORDER_ACCEPT_MARK + orderId,
                            String.valueOf(OrderStatusEnum.ACCEPTED.getCode()),
                            RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME,
                            TimeUnit.MINUTES);
            //Redisson 延迟队列：兜底取消
            this.sendDelayMessage(orderId);
            //RabbitMQ 延迟消息：取消订单
            rabbitService.sendDelayMessage(ExchangeConst.CANCEL_ORDER,
                    RoutingConst.CANCEL_ORDER,
                    orderId.toString(),
                    SystemConstant.CANCEL_ORDER_DELAY_TIME);
        } catch (Exception e) {
            //订单已经落库，后续动作失败不能影响下单结果，记录日志即可（由定时任务/延迟队列兜底取消）
            log.error("订单 {} 创建后的延迟取消消息投递失败", orderId, e);
        }
    }

    /**
     * 生成延迟订单,用redisson实现
     *
     * <p>
     * 使用redisson的延迟队列实现延迟订单发送。
     * 创建延迟队列，并设置延迟队列的过期时间（15min）
     * </p>
     *
     * @param orderId 订单id
     */
    private void sendDelayMessage(Long orderId) {
        try {
            //创建队列
            RBlockingQueue<Object> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");
            //把创建队列放到延迟队列里面
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
            //设置过期时间
            delayedQueue.offer(orderId.toString(), RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.DELAY_MESSAGE_FAIL);
        }
    }

    /**
     * 安全乘法，任一参数为null时返回ZERO
     */
    private BigDecimal safeMultiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return BigDecimal.ZERO;
        }
        return a.multiply(b);
    }

    private BigDecimal safeMultiply(Integer intVal, BigDecimal b) {
        if (intVal == null || b == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(intVal).multiply(b);
    }

    /**
     * 记录乘客下单的状态日志
     *
     * <p>下单是把订单"创建"为等待接单，属于初始状态落库而不是状态迁移，
     * 所以不经过状态机，但仍要留下一条审计记录（fromStatus 为空表示这是订单的起点）。</p>
     *
     * @param orderId 订单id
     * @param status  订单状态
     */
    private void log(String orderId, OrderStatusEnum status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setFromStatus(null);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperatorType(OrderOperatorEnum.CUSTOMER);
        orderStatusLog.setRemark("乘客下单");
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }

    /**
     * 乘客取消订单
     *
     * <p>上车前（等待接单/已接单/司机已到达）允许乘客自助取消；
     * 开始代驾之后不再允许，需要走后台的事故关闭。</p>
     *
     * @param orderId    订单id
     * @param customerId 乘客id
     * @return true
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean cancelOrderByCustomer(Long orderId, Long customerId) {
        OrderInfo current = orderInfoMapper.selectById(orderId);
        if (current == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        //归属校验：只能取消自己的订单
        if (!customerId.equals(current.getCustomerId())) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        orderStateMachineService.fireEvent(
                orderId,
                OrderEventEnum.CANCEL_BY_CUSTOMER,
                OrderOperatorEnum.CUSTOMER,
                customerId,
                "乘客主动取消订单",
                wrapper -> wrapper.eq(OrderInfo::getCustomerId, customerId)
        );
        //取消成功后清掉接单标识，避免延误的派单消息继续把这个订单推给司机
        stringRedisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderId);
        return true;
    }

    /**
     * 司机撤单
     *
     * <p>只能在接到单之后、开始代驾之前撤单。</p>
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return true
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean cancelOrderByDriver(Long orderId, Long driverId) {
        OrderInfo current = orderInfoMapper.selectById(orderId);
        if (current == null || !driverId.equals(current.getDriverId())) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }

        orderStateMachineService.fireEvent(
                orderId,
                OrderEventEnum.CANCEL_BY_DRIVER,
                OrderOperatorEnum.DRIVER,
                driverId,
                "司机主动撤单",
                wrapper -> wrapper.eq(OrderInfo::getDriverId, driverId)
        );
        return true;
    }

    /**
     * 事故关闭订单（后台介入）
     *
     * <p>代驾过程中发生事故时由后台关闭订单，需要留下原因。</p>
     *
     * @param orderId 订单id
     * @param remark  关闭原因
     * @return true
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Boolean closeOrderByCaseAccident(Long orderId, String remark) {
        orderStateMachineService.fireEvent(
                orderId,
                OrderEventEnum.CLOSE_BY_CASE_ACCIDENT,
                OrderOperatorEnum.ADMIN,
                null,
                StringUtils.hasText(remark) ? remark : "代驾过程中发生事故，后台关闭订单");
        return true;
    }

    /**
     * 当前状态下允许执行的事件
     *
     * @param orderId 订单id
     * @return 事件列表
     */
    @Override
    public List<OrderEventEnum> availableEvents(Long orderId) {
        return orderStateMachineService.availableEvents(orderId);
    }
}
