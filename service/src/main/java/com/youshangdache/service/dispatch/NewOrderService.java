package com.youshangdache.service.dispatch;

import com.youshangdache.model.vo.dispatch.NewOrderTaskVo;
import com.youshangdache.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface NewOrderService {
    /**
     * 乘客下单后，添加并开始新订单任务调度
     *
     * @param newOrderTaskVo 订单任务对象
     * @return 该任务调度的id
     */
    Long addAndStartTask(NewOrderTaskVo newOrderTaskVo);

    /**
     * 调度任务开始执行
     *
     * @param jobId 调度任务id
     * @return 成功true，失败false
     */
    Boolean executeTask(Long jobId);

    /**
     * 当司机接单成功后，就需要清空临时队列，释放系统空间
     *
     * @param driverId 司机id
     * @return 成功true，失败false
     */
    Boolean clearNewOrderQueueData(Long driverId);

    /**
     * 查询司机的最新订单数据
     *
     * @param driverId 司机id
     * @return
     */
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);
}
