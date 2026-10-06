package com.youshangdache.order.statemachine;

import com.youshangdache.model.enums.OrderEventEnum;
import com.youshangdache.model.enums.OrderStatusEnum;
import com.youshangdache.model.statemachine.OrderTransitions;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.config.configurers.StateConfigurer;

/**
 * 订单状态机配置
 *
 * <p>迁移规则不写在这里，全部来自 {@link OrderTransitions}（唯一数据源）。
 * 这里只负责"把迁移表装配成 Spring StateMachine"，
 * 因此规则改动只需要改迁移表一处，配置与测试会自动跟随。</p>
 *
 * <p>用 {@code @EnableStateMachineFactory} 而不是 {@code @EnableStateMachine}：
 * 后者是单例，所有订单会共享同一个机器实例，并发下互相覆盖状态。</p>
 */
@Configuration
@EnableStateMachineFactory
public class OrderStateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderStatusEnum, OrderEventEnum> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderStatusEnum, OrderEventEnum> states) throws Exception {
        StateConfigurer<OrderStatusEnum, OrderEventEnum> configurer = states.withStates()
                .initial(OrderTransitions.initialState())
                .states(OrderTransitions.states());
        // 显式声明终态：进入后不允许再迁移
        // （迁移表里本来也没有从终态出发的迁移，这里是把意图表达清楚）
        for (OrderStatusEnum terminal : OrderTransitions.terminalStates()) {
            configurer.end(terminal);
        }
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatusEnum, OrderEventEnum> transitions) throws Exception {
        StateMachineTransitionConfigurer<OrderStatusEnum, OrderEventEnum> configurer = transitions;
        // 遍历迁移表装配：withExternal() 拿到外部迁移配置器，and() 回到父配置器继续追加
        for (OrderTransitions.Transition transition : OrderTransitions.all()) {
            configurer = configurer.withExternal()
                    .source(transition.from())
                    .target(transition.to())
                    .event(transition.event())
                    .and();
        }
    }
}
