package com.youshangdache.model.statemachine;

import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.model.enums.OrderStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态迁移表测试（13 状态 × 12 事件的合法性矩阵）
 *
 * <p>这是整个状态机改造最重要的一道防线：迁移规则只有一份（{@link OrderTransitions}），
 * 这个测试把"哪些迁移合法、哪些不合法"完整固化下来。
 * 任何人改动迁移表而没同步改这里，都会立刻失败。</p>
 */
class OrderTransitionsTest {

    /**
     * 真实状态共 13 个：14 个枚举常量减去查询哨兵 ORDER_NOT_EXIST(-100)
     */
    @Test
    @DisplayName("状态机建模 13 个真实状态，且排除查询哨兵")
    void shouldModelThirteenRealStates() {
        Set<OrderStatusEnum> states = OrderTransitions.states();
        assertEquals(13, states.size(), "真实状态应为 13 个");
        assertFalse(states.contains(OrderStatusEnum.ORDER_NOT_EXIST),
                "ORDER_NOT_EXIST(-100) 是查询哨兵，不能作为状态机状态");
        // 枚举总数 = 13 + 1 个哨兵
        assertEquals(14, OrderStatusEnum.values().length);
    }

    @Test
    @DisplayName("初始状态是等待接单，终态是 5 个")
    void shouldDefineInitialAndTerminalStates() {
        assertEquals(OrderStatusEnum.WAITING_ACCEPT, OrderTransitions.initialState());

        Set<OrderStatusEnum> expectedTerminal = EnumSet.of(
                OrderStatusEnum.ORDER_FINISHED,
                OrderStatusEnum.ORDER_CANCELED_BY_USER,
                OrderStatusEnum.ORDER_CANCELED_BY_DRIVER,
                OrderStatusEnum.ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER,
                OrderStatusEnum.ORDER_CLOSED_CASE_ACCIDENT);
        assertEquals(expectedTerminal, OrderTransitions.terminalStates());

        // 终态不允许有任何出边
        for (OrderStatusEnum terminal : OrderTransitions.terminalStates()) {
            assertTrue(OrderTransitions.availableEvents(terminal).isEmpty(),
                    terminal + " 是终态，不应该有可执行的事件");
        }
    }

    @Test
    @DisplayName("13 个状态 × 12 个事件 的合法性矩阵")
    void shouldVerifyFullLegalMatrix() {
        // 逐条列出期望的合法迁移：(状态, 事件) -> 目标状态
        assertTransition(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.DRIVER_ACCEPT, OrderStatusEnum.ACCEPTED);
        assertTransition(OrderStatusEnum.ACCEPTED, OrderEventEnum.DRIVER_ARRIVE, OrderStatusEnum.DRIVER_ARRIVED);
        assertTransition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.UPDATE_CAR, OrderStatusEnum.UPDATE_CAR_INFO);
        assertTransition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.START_DRIVE, OrderStatusEnum.START_SERVICE);
        assertTransition(OrderStatusEnum.UPDATE_CAR_INFO, OrderEventEnum.START_DRIVE, OrderStatusEnum.START_SERVICE);
        assertTransition(OrderStatusEnum.START_SERVICE, OrderEventEnum.END_DRIVE, OrderStatusEnum.END_SERVICE);
        assertTransition(OrderStatusEnum.END_SERVICE, OrderEventEnum.SEND_BILL, OrderStatusEnum.ORDER_UNPAID);
        assertTransition(OrderStatusEnum.ORDER_UNPAID, OrderEventEnum.PAY_SUCCESS, OrderStatusEnum.ORDER_PAID);
        assertTransition(OrderStatusEnum.ORDER_PAID, OrderEventEnum.FINISH_ORDER, OrderStatusEnum.ORDER_FINISHED);
        assertTransition(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.CANCEL_TIMEOUT,
                OrderStatusEnum.ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER);
        assertTransition(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.CANCEL_BY_CUSTOMER,
                OrderStatusEnum.ORDER_CANCELED_BY_USER);
        assertTransition(OrderStatusEnum.ACCEPTED, OrderEventEnum.CANCEL_BY_CUSTOMER,
                OrderStatusEnum.ORDER_CANCELED_BY_USER);
        assertTransition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.CANCEL_BY_CUSTOMER,
                OrderStatusEnum.ORDER_CANCELED_BY_USER);
        assertTransition(OrderStatusEnum.ACCEPTED, OrderEventEnum.CANCEL_BY_DRIVER,
                OrderStatusEnum.ORDER_CANCELED_BY_DRIVER);
        assertTransition(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.CANCEL_BY_DRIVER,
                OrderStatusEnum.ORDER_CANCELED_BY_DRIVER);
        assertTransition(OrderStatusEnum.START_SERVICE, OrderEventEnum.CLOSE_BY_CASE_ACCIDENT,
                OrderStatusEnum.ORDER_CLOSED_CASE_ACCIDENT);

        // 反向断言：迁移表里每一条都必须在上面的期望清单里，总数必须一致，防止漏测新增迁移
        assertEquals(16, OrderTransitions.all().size(),
                "迁移总数为 16 条；如果这里失败，说明新增/删除了迁移但没同步测试");
        Set<String> declared = new HashSet<>();
        for (OrderTransitions.Transition t : OrderTransitions.all()) {
            declared.add(t.from() + "->" + t.event() + "->" + t.to());
        }
        assertEquals(16, declared.size(), "迁移表存在重复条目");
    }

    @Test
    @DisplayName("关键非法迁移必须被拒绝（防止状态乱跳）")
    void shouldRejectIllegalTransitions() {
        // 已支付不能被发账单打回未付款
        assertIllegal(OrderStatusEnum.ORDER_PAID, OrderEventEnum.SEND_BILL);
        // 不能用"完成订单"把未付款订单直接结束
        assertIllegal(OrderStatusEnum.ORDER_UNPAID, OrderEventEnum.FINISH_ORDER);
        // 未接单不能直接开始代驾
        assertIllegal(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.START_DRIVE);
        // 未付款不能直接结束代驾
        assertIllegal(OrderStatusEnum.ORDER_UNPAID, OrderEventEnum.END_DRIVE);
        // 已取消的订单不能再被接单
        assertIllegal(OrderStatusEnum.ORDER_CANCELED_BY_USER, OrderEventEnum.DRIVER_ACCEPT);
        // 开始代驾之后乘客不能自助取消
        assertIllegal(OrderStatusEnum.START_SERVICE, OrderEventEnum.CANCEL_BY_CUSTOMER);
        // 开始代驾之后司机不能撤单
        assertIllegal(OrderStatusEnum.START_SERVICE, OrderEventEnum.CANCEL_BY_DRIVER);
        // 只有等待接单才可能"超时无人接单"
        assertIllegal(OrderStatusEnum.ACCEPTED, OrderEventEnum.CANCEL_TIMEOUT);
        // 未开始代驾谈不上事故关闭
        assertIllegal(OrderStatusEnum.ACCEPTED, OrderEventEnum.CLOSE_BY_CASE_ACCIDENT);
        // 终态一律不可再迁移
        for (OrderStatusEnum terminal : OrderTransitions.terminalStates()) {
            for (OrderEventEnum event : OrderEventEnum.values()) {
                assertIllegal(terminal, event);
            }
        }
    }

    @Test
    @DisplayName("availableEvents 返回当前状态允许的合法事件")
    void shouldListAvailableEvents() {
        List<OrderEventEnum> waiting = OrderTransitions.availableEvents(OrderStatusEnum.WAITING_ACCEPT);
        assertTrue(waiting.contains(OrderEventEnum.DRIVER_ACCEPT));
        assertTrue(waiting.contains(OrderEventEnum.CANCEL_BY_CUSTOMER));
        assertTrue(waiting.contains(OrderEventEnum.CANCEL_TIMEOUT));
        assertEquals(3, waiting.size());

        // 开始代驾状态只允许"结束代驾"和"事故关闭"
        List<OrderEventEnum> driving = OrderTransitions.availableEvents(OrderStatusEnum.START_SERVICE);
        assertTrue(driving.contains(OrderEventEnum.END_DRIVE));
        assertTrue(driving.contains(OrderEventEnum.CLOSE_BY_CASE_ACCIDENT));
        assertEquals(2, driving.size());
    }

    private void assertTransition(OrderStatusEnum from, OrderEventEnum event, OrderStatusEnum expectedTo) {
        Optional<OrderStatusEnum> actual = OrderTransitions.target(from, event);
        assertTrue(actual.isPresent(), from + " + " + event + " 应该是合法迁移");
        assertEquals(expectedTo, actual.get(), from + " + " + event + " 的目标状态不对");
        assertTrue(OrderTransitions.isLegal(from, event));
    }

    private void assertIllegal(OrderStatusEnum from, OrderEventEnum event) {
        assertFalse(OrderTransitions.isLegal(from, event),
                from + " + " + event + " 应该是非法迁移");
    }
}
