package com.youshangdache.model.statemachine;

import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.model.enums.OrderStatusEnum;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 订单状态迁移表（单一数据源）
 *
 * <p>这是整个订单状态机的"唯一事实来源"：</p>
 * <ul>
 *     <li>{@code OrderStateMachineConfig} 遍历本表来装配 Spring StateMachine 的迁移；</li>
 *     <li>单元测试直接针对本表做"13 状态 × 12 事件"的合法性矩阵断言；</li>
 *     <li>业务代码/前端可以通过 {@link #availableEvents(OrderStatusEnum)} 知道当前状态能做什么。</li>
 * </ul>
 *
 * <p>这样做的目的：迁移规则只写一遍。以前状态判断散落在
 * {@code OrderInfoServiceImpl} 的十几个方法里，改一个规则要在多处同步，
 * 且没人能一眼说清"这个状态到底能干什么"。</p>
 *
 * <p>注意：{@link OrderStatusEnum#ORDER_NOT_EXIST}（-100）是"查不到订单"的查询哨兵，
 * 不是真实状态，绝不能作为状态机里的状态或目标。</p>
 */
public final class OrderTransitions {

    /**
     * 一条状态迁移：从 from 状态，收到 event 事件，迁移到 to 状态
     */
    public record Transition(OrderStatusEnum from, OrderEventEnum event, OrderStatusEnum to) {
    }

    /**
     * 全部合法迁移（顺序与业务语义一致，便于阅读）
     */
    private static final List<Transition> ALL = List.of(
            // ===== 正常代驾流程 =====
            new Transition(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.DRIVER_ACCEPT, OrderStatusEnum.ACCEPTED),
            new Transition(OrderStatusEnum.ACCEPTED, OrderEventEnum.DRIVER_ARRIVE, OrderStatusEnum.DRIVER_ARRIVED),
            new Transition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.UPDATE_CAR, OrderStatusEnum.UPDATE_CAR_INFO),
            // 司机到达后可以直接开始代驾（不强制必须录入车辆信息）
            new Transition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.START_DRIVE, OrderStatusEnum.START_SERVICE),
            new Transition(OrderStatusEnum.UPDATE_CAR_INFO, OrderEventEnum.START_DRIVE, OrderStatusEnum.START_SERVICE),
            new Transition(OrderStatusEnum.START_SERVICE, OrderEventEnum.END_DRIVE, OrderStatusEnum.END_SERVICE),
            new Transition(OrderStatusEnum.END_SERVICE, OrderEventEnum.SEND_BILL, OrderStatusEnum.ORDER_UNPAID),
            new Transition(OrderStatusEnum.ORDER_UNPAID, OrderEventEnum.PAY_SUCCESS, OrderStatusEnum.ORDER_PAID),
            // 分账完成后订单才真正结束（原来支付后永远停在"已付款"）
            new Transition(OrderStatusEnum.ORDER_PAID, OrderEventEnum.FINISH_ORDER, OrderStatusEnum.ORDER_FINISHED),

            // ===== 取消 / 关闭 =====
            // 超时无人接单：只有"等待接单"才可能超时
            new Transition(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.CANCEL_TIMEOUT, OrderStatusEnum.ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER),
            // 乘客取消：上车前都可以取消；一旦开始代驾就不再允许自助取消，只能走事故关闭
            new Transition(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.CANCEL_BY_CUSTOMER, OrderStatusEnum.ORDER_CANCELED_BY_USER),
            new Transition(OrderStatusEnum.ACCEPTED, OrderEventEnum.CANCEL_BY_CUSTOMER, OrderStatusEnum.ORDER_CANCELED_BY_USER),
            new Transition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.CANCEL_BY_CUSTOMER, OrderStatusEnum.ORDER_CANCELED_BY_USER),
            // 司机撤单：只能在接到单之后、开始代驾之前
            new Transition(OrderStatusEnum.ACCEPTED, OrderEventEnum.CANCEL_BY_DRIVER, OrderStatusEnum.ORDER_CANCELED_BY_DRIVER),
            new Transition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.CANCEL_BY_DRIVER, OrderStatusEnum.ORDER_CANCELED_BY_DRIVER),
            // 事故关闭：代驾过程中出现事故，由后台介入关闭
            new Transition(OrderStatusEnum.START_SERVICE, OrderEventEnum.CLOSE_BY_CASE_ACCIDENT, OrderStatusEnum.ORDER_CLOSED_CASE_ACCIDENT)
    );

    /**
     * 索引：from 状态 -> (事件 -> 目标状态)
     */
    private static final Map<OrderStatusEnum, Map<OrderEventEnum, OrderStatusEnum>> INDEX = buildIndex();

    /**
     * 终态：进入后不允许再迁移
     */
    private static final Set<OrderStatusEnum> TERMINAL_STATES = Collections.unmodifiableSet(EnumSet.of(
            OrderStatusEnum.ORDER_FINISHED,
            OrderStatusEnum.ORDER_CANCELED_BY_USER,
            OrderStatusEnum.ORDER_CANCELED_BY_DRIVER,
            OrderStatusEnum.ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER,
            OrderStatusEnum.ORDER_CLOSED_CASE_ACCIDENT
    ));

    private OrderTransitions() {
    }

    private static Map<OrderStatusEnum, Map<OrderEventEnum, OrderStatusEnum>> buildIndex() {
        Map<OrderStatusEnum, Map<OrderEventEnum, OrderStatusEnum>> index = new EnumMap<>(OrderStatusEnum.class);
        for (Transition t : ALL) {
            Map<OrderEventEnum, OrderStatusEnum> byEvent =
                    index.computeIfAbsent(t.from(), k -> new EnumMap<>(OrderEventEnum.class));
            OrderStatusEnum previous = byEvent.put(t.event(), t.to());
            if (previous != null) {
                // 同一个"状态 + 事件"出现两个目标状态，状态机语义不成立，属于配置错误
                throw new IllegalStateException(String.format(
                        "订单迁移表存在冲突：%s + %s 同时指向 %s 与 %s", t.from(), t.event(), previous, t.to()));
            }
        }
        return Collections.unmodifiableMap(index);
    }

    /**
     * 全部合法迁移（只读）
     */
    public static List<Transition> all() {
        return ALL;
    }

    /**
     * 状态机需要建模的所有真实状态（13 个，已排除查询哨兵 ORDER_NOT_EXIST）
     */
    public static Set<OrderStatusEnum> states() {
        Set<OrderStatusEnum> states = EnumSet.allOf(OrderStatusEnum.class);
        states.remove(OrderStatusEnum.ORDER_NOT_EXIST);
        return Collections.unmodifiableSet(states);
    }

    /**
     * 初始状态：乘客下单成功后订单所处的状态
     */
    public static OrderStatusEnum initialState() {
        return OrderStatusEnum.WAITING_ACCEPT;
    }

    /**
     * 终态集合
     */
    public static Set<OrderStatusEnum> terminalStates() {
        return TERMINAL_STATES;
    }

    public static boolean isTerminal(OrderStatusEnum status) {
        return TERMINAL_STATES.contains(status);
    }

    /**
     * 计算目标状态
     *
     * @return 迁移合法时返回目标状态，否则返回 {@link Optional#empty()}
     */
    public static Optional<OrderStatusEnum> target(OrderStatusEnum from, OrderEventEnum event) {
        if (from == null || event == null) {
            return Optional.empty();
        }
        Map<OrderEventEnum, OrderStatusEnum> byEvent = INDEX.get(from);
        if (byEvent == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byEvent.get(event));
    }

    /**
     * 判断迁移是否合法
     */
    public static boolean isLegal(OrderStatusEnum from, OrderEventEnum event) {
        return target(from, event).isPresent();
    }

    /**
     * 当前状态下允许的事件（可用于给前端下发"该显示哪些按钮"）
     */
    public static List<OrderEventEnum> availableEvents(OrderStatusEnum from) {
        Map<OrderEventEnum, OrderStatusEnum> byEvent = INDEX.get(from);
        if (byEvent == null || byEvent.isEmpty()) {
            return List.of();
        }
        return List.copyOf(byEvent.keySet());
    }

    /**
     * 当前状态 + 事件 -> 目标状态，供前端展示用（保留迁移表顺序）
     */
    public static Map<OrderEventEnum, OrderStatusEnum> availableTransitions(OrderStatusEnum from) {
        Map<OrderEventEnum, OrderStatusEnum> byEvent = INDEX.get(from);
        if (byEvent == null || byEvent.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(byEvent));
    }
}
