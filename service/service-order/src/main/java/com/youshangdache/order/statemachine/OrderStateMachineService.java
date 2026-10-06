package com.youshangdache.order.statemachine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.model.entity.order.OrderInfo;
import com.youshangdache.model.entity.order.OrderStatusLog;
import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.model.enums.OrderOperatorEnum;
import com.youshangdache.model.enums.OrderStatusEnum;
import com.youshangdache.model.statemachine.OrderTransitions;
import com.youshangdache.order.mapper.OrderInfoMapper;
import com.youshangdache.order.mapper.OrderStatusLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 订单状态机统一入口
 *
 * <p><b>这是 order_info.status 的唯一写入口。</b>所有状态推进都必须经过本类，
 * 不允许业务代码再直接 setStatus。</p>
 *
 * <p>一次迁移由两步保证：</p>
 * <ol>
 *     <li><b>状态机判定合法性</b>——事件与当前状态不匹配时直接拒绝，给出明确的业务错误；</li>
 *     <li><b>数据库条件更新（CAS）</b>——{@code WHERE id = ? AND status = 读取时的状态}，
 *         受影响行数为 0 说明并发被抢先，抛"状态已变更"。</li>
 * </ol>
 *
 * <p>这两步解决的是不同问题，缺一不可：状态机保证"语义正确"，
 * CAS 保证"并发安全"。状态机本身不提供任何并发能力。</p>
 *
 * <p>副作用（写审计日志）只在 CAS 成功之后执行，因此日志里不会出现"未生效"的迁移。</p>
 */
@Slf4j
@Service
public class OrderStateMachineService {

    private final StateMachineFactory<OrderStatusEnum, OrderEventEnum> stateMachineFactory;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;

    public OrderStateMachineService(StateMachineFactory<OrderStatusEnum, OrderEventEnum> stateMachineFactory,
                                    OrderInfoMapper orderInfoMapper,
                                    OrderStatusLogMapper orderStatusLogMapper) {
        this.stateMachineFactory = stateMachineFactory;
        this.orderInfoMapper = orderInfoMapper;
        this.orderStatusLogMapper = orderStatusLogMapper;
    }

    /**
     * 只改状态、不需要额外字段的迁移
     *
     * @param orderId    订单id
     * @param event      事件
     * @param operator   操作人类型
     * @param operatorId 操作人id（系统触发传 null）
     * @param remark     备注
     * @return 迁移后的状态
     */
    public OrderStatusEnum fireEvent(Long orderId, OrderEventEnum event,
                                     OrderOperatorEnum operator, Long operatorId, String remark) {
        OrderInfo patch = new OrderInfo();
        patch.setId(orderId);
        return fireEvent(patch, event, operator, operatorId, remark);
    }

    /**
     * 只改状态、但需要额外更新条件的迁移（例如"只能取消自己的订单"）
     */
    public OrderStatusEnum fireEvent(Long orderId,
                                     OrderEventEnum event,
                                     OrderOperatorEnum operator,
                                     Long operatorId,
                                     String remark,
                                     Consumer<LambdaQueryWrapper<OrderInfo>> extraCondition) {
        OrderInfo patch = new OrderInfo();
        patch.setId(orderId);
        return fireEvent(patch, event, operator, operatorId, remark, extraCondition);
    }

    /**
     * 需要同时写入其它字段的迁移（例如抢单要写 driverId/acceptTime）
     *
     * <p>{@code patch} 里只需要放"本次要更新的字段" + id，状态由本方法根据迁移表决定后写入，
     * 调用方不要自己 setStatus。</p>
     *
     * @param patch      待更新字段（必须包含 id）
     * @param event      事件
     * @param operator   操作人类型
     * @param operatorId 操作人id
     * @param remark     备注
     * @return 迁移后的状态
     */
    public OrderStatusEnum fireEvent(OrderInfo patch,
                                     OrderEventEnum event,
                                     OrderOperatorEnum operator,
                                     Long operatorId,
                                     String remark) {
        return fireEvent(patch, event, operator, operatorId, remark, null);
    }

    /**
     * 需要同时写入其它字段、并且需要额外更新条件的迁移
     *
     * <p>典型场景："只能更新自己的订单"——司机归属校验必须作为 UPDATE 的 WHERE 条件参与原子判断，
     * 不能只在 Java 里 if 一下，否则并发下仍可能改到别人的订单。</p>
     *
     * @param patch          待更新字段（必须包含 id）
     * @param event          事件
     * @param operator       操作人类型
     * @param operatorId     操作人id
     * @param remark         备注
     * @param extraCondition 额外的更新条件（可为 null）
     * @return 迁移后的状态
     */
    public OrderStatusEnum fireEvent(OrderInfo patch,
                                     OrderEventEnum event,
                                     OrderOperatorEnum operator,
                                     Long operatorId,
                                     String remark,
                                     Consumer<LambdaQueryWrapper<OrderInfo>> extraCondition) {
        if (patch == null || patch.getId() == null) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        String orderId = patch.getOrderNo();

        OrderInfo current = orderInfoMapper.selectById(orderId);
        if (current == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        OrderStatusEnum from = current.getStatus();

        // 第一步：状态机判定合法性与目标状态
        OrderStatusEnum to = resolveTarget(from, event);

        // 第二步：CAS 更新（同时写入调用方要求的其它字段 + 额外条件）
        patch.setStatus(to);
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderId)
                .eq(OrderInfo::getStatus, from);
        if (extraCondition != null) {
            extraCondition.accept(wrapper);
        }
        int rows = orderInfoMapper.update(patch, wrapper);
        if (rows == 0) {
            // 读到的状态（或归属）与更新时不符，说明期间被其它请求改掉了
            log.warn("订单 {} 状态迁移并发冲突：期望从 {} 迁移到 {}，条件更新受影响行数为 0", orderId, from, to);
            throw new GuiguException(ResultCodeEnum.ORDER_CONCURRENT_MODIFY);
        }

        // 第三步：更新成功后才写审计日志
        recordStatusLog(orderId, from, to, event, operator, operatorId, remark);
        log.info("订单 {} 状态迁移：{} --[{}]--> {}", orderId, from.getMessage(), event.getMessage(), to.getMessage());
        return to;
    }

    /**
     * 只用状态机校验并计算目标状态，不落库
     *
     * @param from  当前状态
     * @param event 事件
     * @return 目标状态
     * @throws GuiguException 迁移非法时抛出 {@link ResultCodeEnum#ORDER_STATUS_ILLEGAL_TRANSITION}
     */
    public OrderStatusEnum resolveTarget(OrderStatusEnum from, OrderEventEnum event) {
        if (from == null || event == null) {
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }
        // 之所以既走状态机、又校验迁移表：状态机负责"装配正确性"，迁移表负责"可测试性"，
        // 两者的答案必须一致，不一致说明配置漏了迁移，属于严重问题，直接暴露出来。
        Optional<OrderStatusEnum> expected = OrderTransitions.target(from, event);
        if (expected.isEmpty()) {
            log.warn("订单状态迁移非法：当前状态 {} 不允许事件 {}", from, event);
            throw new GuiguException(ResultCodeEnum.ORDER_STATUS_ILLEGAL_TRANSITION);
        }

        StateMachine<OrderStatusEnum, OrderEventEnum> stateMachine = stateMachineFactory.getStateMachine();
        try {
            //每个请求新建机器实例（不要按 orderId 缓存，否则实例会无限增长）
            stateMachine.start();
            // 注意顺序：必须先 start() 再 resetStateMachine()。
            // 反过来的话，start() 会把状态重新拉回初始状态（WAITING_ACCEPT），
            // 导致所有校验都从初始状态开始 —— 那样状态机就等于失效了。
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(access ->
                            access.resetStateMachine(new DefaultStateMachineContext<>(from, null, null, null))
                    );

            boolean accepted = stateMachine.sendEvent(MessageBuilder.withPayload(event).build());
            if (!accepted) {
                log.error("状态机拒绝了迁移表允许的迁移，疑似状态机配置与迁移表不一致：{} + {}", from, event);
                throw new GuiguException(ResultCodeEnum.ORDER_STATUS_ILLEGAL_TRANSITION);
            }
            OrderStatusEnum actual = stateMachine.getState().getId();
            if (actual != expected.get()) {
                log.error("状态机计算结果与迁移表不一致：{} + {} -> 状态机={} 迁移表={}",from, event, actual, expected.get());
                throw new GuiguException(ResultCodeEnum.ORDER_STATUS_ILLEGAL_TRANSITION);
            }
            return actual;
        } finally {
            stateMachine.stop();
        }
    }

    /**
     * 当前状态下允许执行的事件（可用于给前端下发按钮，避免前端硬编码状态判断）
     */
    public List<OrderEventEnum> availableEvents(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        return OrderTransitions.availableEvents(orderInfo.getStatus());
    }

    /**
     * 当前状态 + 事件 -> 目标状态
     */
    public Map<OrderEventEnum, OrderStatusEnum> availableTransitions(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo == null) {
            throw new GuiguException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        return OrderTransitions.availableTransitions(orderInfo.getStatus());
    }

    private void recordStatusLog(String orderId, OrderStatusEnum from, OrderStatusEnum to,
                                 OrderEventEnum event, OrderOperatorEnum operator,
                                 Long operatorId, String remark) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(orderId);
        statusLog.setFromStatus(from);
        statusLog.setOrderStatus(to);
        statusLog.setEvent(event);
        statusLog.setOperatorType(operator == null ? OrderOperatorEnum.SYSTEM : operator);
        statusLog.setOperatorId(operatorId);
        statusLog.setRemark(remark);
        statusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(statusLog);
    }
}
