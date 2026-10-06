-- ============================================================================
-- 订单状态机改造：order_status_log 升级为「状态迁移审计日志」
--
-- 背景：原来的 order_status_log 只记录「变成了什么状态」
--       （order_id / order_status / operate_time），
--       改造后每次迁移都要能回答：从哪个状态、因为什么事件、由谁触发、为什么。
--
-- 执行库：daijia_order（service-order 使用的库）
-- 说明：全部字段可空，对已有历史数据无影响，可安全执行。
-- ============================================================================

ALTER TABLE `order_status_log`
    ADD COLUMN `from_status`   INT          NULL COMMENT '变更前的状态（NULL 表示这是订单的初始状态，例如乘客下单）' AFTER `order_id`,
    ADD COLUMN `event`         INT          NULL COMMENT '触发本次迁移的事件，见 OrderEventEnum'                     AFTER `order_status`,
    ADD COLUMN `operator_id`   BIGINT       NULL COMMENT '操作人id（乘客id/司机id/管理员id，系统触发为空）'          AFTER `event`,
    ADD COLUMN `operator_type` INT          NULL COMMENT '操作人类型：1乘客 2司机 3系统 4后台管理员'                 AFTER `operator_id`,
    ADD COLUMN `remark`        VARCHAR(255) NULL COMMENT '备注（例如事故关闭的原因）'                                AFTER `operator_type`;

-- 排查「订单为什么卡在这个状态」时，最常用的查询是按订单查时间线
CREATE INDEX `idx_order_status_log_order_id` ON `order_status_log` (`order_id`, `operate_time`);

-- ----------------------------------------------------------------------------
-- 验证：可以按下面的 SQL 画出某个订单的状态时间线
-- ----------------------------------------------------------------------------
-- SELECT order_id, from_status, order_status, event, operator_type, operator_id, remark, operate_time
-- FROM order_status_log
-- WHERE order_id = ?
-- ORDER BY operate_time, id;
