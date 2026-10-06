package com.youshangdache.order.statemachine;

import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.model.enums.OrderStatusEnum;
import com.youshangdache.model.statemachine.OrderTransitions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单状态机装配测试
 *
 * <p>这个测试直接驱动真实的 {@link OrderStateMachineConfig}（不启动 Spring 容器、不连数据库），
 * 验证"迁移表 → 状态机"的装配结果与迁移表完全一致。</p>
 *
 * <p>因为顺序问题很容易踩坑（reset 与 start 的先后），这里把"复位到指定状态再发事件"的
 * 完整过程都跑了一遍，确认 {@code OrderStateMachineService} 使用的调用顺序是正确的。</p>
 */
class OrderStateMachineConfigTest {

    private StateMachineFactory<OrderStatusEnum, OrderEventEnum> factory;

    @BeforeEach
    void setUp() throws Exception {
        OrderStateMachineConfig config = new OrderStateMachineConfig();
        StateMachineBuilder.Builder<OrderStatusEnum, OrderEventEnum> builder = StateMachineBuilder.builder();
        // 依次执行配置类里的三段配置，等价于 Spring 容器里的装配过程
        config.configure(builder.configureConfiguration());
        config.configure(builder.configureStates());
        config.configure(builder.configureTransitions());
        factory = builder.createFactory();
    }

    @Test
    @DisplayName("迁移表里每一条迁移，状态机都必须接受并落到正确的目标状态")
    void shouldAcceptEveryLegalTransitionWithCorrectTarget() {
        for (OrderTransitions.Transition transition : OrderTransitions.all()) {
            Optional<OrderStatusEnum> actual = sendEvent(transition.from(), transition.event());
            assertTrue(actual.isPresent(),
                    String.format("状态机应接受迁移：%s + %s", transition.from(), transition.event()));
            assertEquals(transition.to(), actual.get(),
                    String.format("迁移 %s + %s 的目标状态不对", transition.from(), transition.event()));
        }
    }

    @Test
    @DisplayName("13 状态 × 12 事件：迁移表之外的所有组合都必须被状态机拒绝")
    void shouldRejectEveryIllegalCombination() {
        int checked = 0;
        for (OrderStatusEnum from : OrderTransitions.states()) {
            for (OrderEventEnum event : OrderEventEnum.values()) {
                checked++;
                boolean legalInTable = OrderTransitions.isLegal(from, event);
                boolean accepted = sendEvent(from, event).isPresent();
                assertEquals(
                        legalInTable,
                        accepted,
                        String.format("状态机与迁移表不一致：%s + %s（迁移表认为%s，状态机%s）", from, event, legalInTable ? "合法" : "非法", accepted ? "接受" : "拒绝")
                );
            }
        }
        assertEquals(13 * 12, checked, "应当覆盖 13 个状态 × 12 个事件");
    }

    @Test
    @DisplayName("关键状态迁移链路可以完整走通：下单 -> 抢单 -> 到达 -> 开始 -> 结束 -> 发账单 -> 支付 -> 完成")
    void shouldWalkThroughHappyPath() {
        assertEquals(OrderStatusEnum.ACCEPTED, sendEvent(OrderStatusEnum.WAITING_ACCEPT, OrderEventEnum.DRIVER_ACCEPT).orElseThrow());
        assertEquals(OrderStatusEnum.DRIVER_ARRIVED, sendEvent(OrderStatusEnum.ACCEPTED, OrderEventEnum.DRIVER_ARRIVE).orElseThrow());
        assertEquals(OrderStatusEnum.UPDATE_CAR_INFO, sendEvent(OrderStatusEnum.DRIVER_ARRIVED, OrderEventEnum.UPDATE_CAR).orElseThrow());
        assertEquals(OrderStatusEnum.START_SERVICE, sendEvent(OrderStatusEnum.UPDATE_CAR_INFO, OrderEventEnum.START_DRIVE).orElseThrow());
        assertEquals(OrderStatusEnum.END_SERVICE, sendEvent(OrderStatusEnum.START_SERVICE, OrderEventEnum.END_DRIVE).orElseThrow());
        assertEquals(OrderStatusEnum.ORDER_UNPAID, sendEvent(OrderStatusEnum.END_SERVICE, OrderEventEnum.SEND_BILL).orElseThrow());
        assertEquals(OrderStatusEnum.ORDER_PAID, sendEvent(OrderStatusEnum.ORDER_UNPAID, OrderEventEnum.PAY_SUCCESS).orElseThrow());
        assertEquals(OrderStatusEnum.ORDER_FINISHED, sendEvent(OrderStatusEnum.ORDER_PAID, OrderEventEnum.FINISH_ORDER).orElseThrow());
    }

    @Test
    @DisplayName("终态无法再迁移：已结束的订单收到任何事件都被拒绝")
    void shouldNotLeaveTerminalState() {
        for (OrderStatusEnum terminal : OrderTransitions.terminalStates()) {
            for (OrderEventEnum event : OrderEventEnum.values()) {
                assertFalse(sendEvent(terminal, event).isPresent(),
                        terminal + " 是终态，不应接受事件 " + event);
            }
        }
    }

    @Test
    @DisplayName("复位到指定状态后，状态机确实停在该状态（验证 reset 与 start 的调用顺序）")
    void shouldResetToGivenState() {
        StateMachine<OrderStatusEnum, OrderEventEnum> machine = newMachineAt(OrderStatusEnum.START_SERVICE);
        assertEquals(OrderStatusEnum.START_SERVICE, machine.getState().getId(),
                "复位后状态机应当停在指定状态，否则所有校验都会从初始状态开始，等于失效");
        machine.stop();
    }

    /**
     * 把状态机复位到指定状态后发送事件
     *
     * <p>调用顺序与 {@code OrderStateMachineService.resolveTarget} 保持一致：
     * start() -> resetStateMachine() -> sendEvent()。
     * 注意不能先 reset 再 start，否则 start 会把状态拉回初始状态。</p>
     *
     * @return 事件被接受时返回目标状态，否则返回空
     */
    private Optional<OrderStatusEnum> sendEvent(OrderStatusEnum from, OrderEventEnum event) {
        StateMachine<OrderStatusEnum, OrderEventEnum> machine = newMachineAt(from);
        try {
            boolean accepted = machine.sendEvent(MessageBuilder.withPayload(event).build());
            return accepted ? Optional.of(machine.getState().getId()) : Optional.empty();
        } finally {
            machine.stop();
        }
    }

    private StateMachine<OrderStatusEnum, OrderEventEnum> newMachineAt(OrderStatusEnum from) {
        StateMachine<OrderStatusEnum, OrderEventEnum> machine = factory.getStateMachine();
        machine.start();
        machine.getStateMachineAccessor().doWithAllRegions(access ->
                access.resetStateMachine(new DefaultStateMachineContext<>(from, null, null, null)));
        return machine;
    }
}
