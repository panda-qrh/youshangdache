package com.youshangdache.service.impl.order;

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
import com.youshangdache.model.enums.OrderStatusEnum;
import com.youshangdache.model.enums.ProfitsharingStatusEnum;
import com.youshangdache.model.form.order.OrderInfoForm;
import com.youshangdache.model.form.order.StartDriveForm;
import com.youshangdache.model.form.order.UpdateOrderBillForm;
import com.youshangdache.model.form.order.UpdateOrderCartForm;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.order.*;
import com.youshangdache.mapper.order.OrderBillMapper;
import com.youshangdache.mapper.order.OrderInfoMapper;
import com.youshangdache.mapper.order.OrderProfitsharingMapper;
import com.youshangdache.mapper.order.OrderStatusLogMapper;
import com.youshangdache.service.order.OrderInfoService;
import com.youshangdache.service.order.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
     * 修改分账信息的状态
     *
     * @param orderNo 订单编号
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateProfitsharingStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo).select(OrderInfo::getId));
        OrderProfitsharing updateOrderProfitsharing = new OrderProfitsharing();
        updateOrderProfitsharing.setStatus(ProfitsharingStatusEnum.SHARED);
        orderProfitsharingMapper.update(updateOrderProfitsharing, new LambdaQueryWrapper<OrderProfitsharing>()
                .eq(OrderProfitsharing::getOrderId, orderInfo.getId()));
    }

    /**
     * 系统取消订单
     *
     * @param orderId 订单id
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void systemCancelOrder(Long orderId) {
        OrderStatusEnum orderStatus = this.getOrderStatus(orderId);
        if (orderStatus == OrderStatusEnum.WAITING_ACCEPT) {
            //取消订单
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setId(orderId);
            orderInfo.setStatus(OrderStatusEnum.ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER);
            int row = orderInfoMapper.updateById(orderInfo);
            if (row == 1) {
                //记录日志
                this.log(orderInfo.getId(), orderInfo.getStatus());
                //删除redis订单标识
                stringRedisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            } else {
                throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
            }
        }
    }

    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        orderBillMapper.updateCouponAmount(orderId, couponAmount);
        return true;
    }

    @Override
    public void orderCancel(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo.getStatus() == OrderStatusEnum.WAITING_ACCEPT) {
            orderInfo.setStatus(OrderStatusEnum.ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER);
            int i = orderInfoMapper.updateById(orderInfo);
            if (i > 0) {
                //删除接单标识
                stringRedisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        }
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
    public Boolean updateOrderPayStatus(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));

        //订单存在且已付款
        if (orderInfo == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        if (orderInfo.getStatus() == OrderStatusEnum.ORDER_PAID) {
            return true;
        }
        //订单存在，但订单未支付
        OrderInfo orderInfo1 = new OrderInfo();
        orderInfo1.setStatus(OrderStatusEnum.ORDER_PAID);
        orderInfo1.setPayTime(new Date());
        int update = orderInfoMapper.update(orderInfo1, new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (update > 0) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
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
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getDriverId, driverId);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatusEnum.ORDER_UNPAID);
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows > 0) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

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
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, form.getOrderId())
                .eq(OrderInfo::getDriverId, form.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatusEnum.END_SERVICE);
        orderInfo.setRealAmount(form.getTotalAmount());
        orderInfo.setFavourFee(form.getFavourFee());
        orderInfo.setRealDistance(form.getRealDistance());
        orderInfo.setEndServiceTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows == 0) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

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
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, startDriveForm.getOrderId())
                .eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatusEnum.START_SERVICE);
        updateOrderInfo.setStartServiceTime(new Date());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if (row >= 1) {
            //记录日志
            this.log(startDriveForm.getOrderId(), OrderStatusEnum.START_SERVICE);
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

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
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, updateOrderCartForm.getOrderId())
                .eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm, orderInfo);
        orderInfo.setStatus(OrderStatusEnum.UPDATE_CAR_INFO);
        int rows = orderInfoMapper.update(orderInfo, queryWrapper);
        if (rows > 0) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
    }

    /**
     * 司机到达起始点
     *
     * @param orderId  订单id
     * @param driverId 司机id
     * @return
     */
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        //更新订单状态，到达时间
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getDriverId, driverId);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatusEnum.DRIVER_ARRIVED);
        orderInfo.setArriveTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, queryWrapper);
        if (rows > 0) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
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
        OrderStatusEnum[] statusArray = {
                OrderStatusEnum.ACCEPTED,
                OrderStatusEnum.DRIVER_ARRIVED,
                OrderStatusEnum.UPDATE_CAR_INFO,
                OrderStatusEnum.START_SERVICE,
                OrderStatusEnum.END_SERVICE
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
        // 判断定是否存在
        if (!stringRedisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK + orderId)) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        //创建锁 order:accept:mark:{orderId}
        RLock lock = redissonClient.getLock(RedisConstant.ORDER_ACCEPT_MARK + orderId);
        try {
            if (!stringRedisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK + orderId)) {
                throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
            }
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME, RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (flag) {
                OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getId, orderId));
                orderInfo.setStatus(OrderStatusEnum.ACCEPTED);
                orderInfo.setDriverId(driverId);
                orderInfo.setAcceptTime(new Date());
                int row = orderInfoMapper.updateById(orderInfo);
                if (row < 1) {
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.ORDER_SNAP_UP_FAILED);
                }
                //司机抢单成功，说明用户的订单已被司机接单，那就不需要再等待接单了，删除redis中的标记
                stringRedisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderId);
                return true;
            }

        } catch (InterruptedException e) {
            throw new GuiguException(ResultCodeEnum.ORDER_SNAP_UP_FAILED);
        } finally {
            if (lock.isLocked()) {
                lock.lock();
            }
        }
        return false;
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
        //生成订单之后，发送到延迟队列
        this.sendDelayMessage(orderInfo.getId());
        //记录日志
        this.log(orderInfo.getId(), orderInfo.getStatus());
        //接单标识，标识不存在了说明不在等待接单状态了
        stringRedisTemplate.opsForValue()
                .set(RedisConstant.ORDER_ACCEPT_MARK + orderInfo.getId(),
                        String.valueOf(OrderStatusEnum.ACCEPTED.getCode()),
                        RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME,
                        TimeUnit.MINUTES);

        //发送延迟消息，取消订单
        rabbitService.sendDelayMessage(ExchangeConst.CANCEL_ORDER,
                RoutingConst.CANCEL_ORDER,
                orderInfo.getId().toString(),
                SystemConstant.CANCEL_ORDER_DELAY_TIME);
        return orderInfo.getId();
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
     * 记录乘客下单的日志
     *
     * @param orderId 订单id
     * @param status  订单状态
     */
    private void log(Long orderId, OrderStatusEnum status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
