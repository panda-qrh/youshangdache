package com.qrh.youshangdache.dispatch.client;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.vo.dispatch.NewOrderTaskVo;
import com.qrh.youshangdache.model.vo.order.NewOrderDataVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient(value = "service-dispatch", path = "/dispatch")
public interface NewOrderFeignClient {

    /**
     * 乘客下单后，添加并开始新订单任务调度
     *
     * @param newOrderTaskVo 订单任务对象
     * @return 该任务调度的id
     */
    @PostMapping("/newOrder/addAndStartTask")
    public Result<Long> addAndStartTask(@RequestBody NewOrderTaskVo newOrderTaskVo);

    /**
     * 查询司机的最新订单数据
     *
     * @param driverId 司机id
     * @return
     */
    @PostMapping("/findNewOrderQueueData/{driverId}")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData(@PathVariable Long driverId);

    /**
     * 当司机接单成功后，就需要清空临时队列，释放系统空间
     *
     * @param driverId 司机id
     * @return 成功true，失败false
     */
    @PostMapping("/clearNewOrderQueueData/{driverId}")
    public Result<Boolean> clearNewOrderQueueData(@PathVariable Long driverId);
}