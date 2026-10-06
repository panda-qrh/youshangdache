package com.youshangdache.dispatch.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.youshangdache.common.constant.RedisConstant;
import com.youshangdache.dispatch.mapper.OrderJobMapper;
import com.youshangdache.dispatch.service.NewOrderService;
import com.youshangdache.dispatch.xxl.client.XxlJobClient;
import com.youshangdache.map.LocationFeignClient;
import com.youshangdache.model.entity.dispatch.OrderJob;
import com.youshangdache.model.enums.OrderStatusEnum;
import com.youshangdache.model.form.map.SearchNearByDriverForm;
import com.youshangdache.model.vo.dispatch.NewOrderTaskVo;
import com.youshangdache.model.vo.map.NearByDriverVo;
import com.youshangdache.model.vo.order.NewOrderDataVo;
import com.youshangdache.order.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NewOrderServiceImpl implements NewOrderService {
    @Resource
    private XxlJobClient xxlJobClient;
    @Resource
    private OrderJobMapper orderJobMapper;
    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;


    /**
     * 当司机接单成功后，就需要清空临时队列，释放系统空间
     *
     * @param driverId 司机id
     * @return true|false
     */
    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        stringRedisTemplate.delete(key);
        //删除不存在的 key 时 delete 会返回 false，直接返回它会让调用方误以为清理失败
        return true;
    }

    /**
     * 查询司机的最新订单数据
     *
     * @param driverId 司机id
     * @return
     */
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        if (Objects.requireNonNull(stringRedisTemplate.opsForList().size(key)) > 0) {
            String content = stringRedisTemplate.opsForList().leftPop(key);
            NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
            list.add(newOrderDataVo);
        }
        return list;
    }

    /**
     * 调度任务开始执行
     *
     * @param jobId 调度任务id
     * @return 成功true，失败false
     */
    @Override
    public Boolean executeTask(Long jobId) {
        //获取任务参数
        OrderJob orderJob = orderJobMapper.selectOne(new LambdaQueryWrapper<OrderJob>().eq(OrderJob::getJobId, jobId));
        if (null == orderJob) {
            return true;
        }
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(orderJob.getParameter(), NewOrderTaskVo.class);

        //查询订单状态，如果该订单还在接单状态，继续执行；如果不在接单状态，则停止定时调度
        OrderStatusEnum orderStatus = orderInfoFeignClient.getOrderStatus(newOrderTaskVo.getOrderId());
        if (orderStatus != OrderStatusEnum.WAITING_ACCEPT) {
            xxlJobClient.stopJob(jobId);
            log.info("停止任务调度: {}", JSON.toJSONString(newOrderTaskVo));
            return true;
        }

        //搜索附近满足条件的司机
        SearchNearByDriverForm nearByDrivers = new SearchNearByDriverForm();
        nearByDrivers.setLongitude(newOrderTaskVo.getStartPointLongitude());
        nearByDrivers.setLatitude(newOrderTaskVo.getStartPointLatitude());
        nearByDrivers.setMileageDistance(newOrderTaskVo.getExpectDistance());
        List<NearByDriverVo> nearByDriverVoList = locationFeignClient.searchNearByDriver(nearByDrivers);
        //附近司机可能为空（服务返回空集合或异常时返回 null），必须先判空，
        //否则 forEach 会直接抛 NPE，导致每分钟一次的调度任务持续失败。
        if (nearByDriverVoList == null || nearByDriverVoList.isEmpty()) {
            log.info("订单 {} 附近没有满足条件的司机，本次不派单", newOrderTaskVo.getOrderId());
            return true;
        }
        //给司机派发订单信息
        nearByDriverVoList.forEach(driver -> {
            //记录司机id，防止重复推送订单信息
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST + newOrderTaskVo.getOrderId();
            boolean isMember = stringRedisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());
            if (!isMember) {
                //记录该订单已放入司机临时容器
                stringRedisTemplate.opsForSet().add(repeatKey, driver.getDriverId().toString());
                //过期时间：16分钟（比"15分钟无人接单自动取消"稍长，保证订单取消前不会重复推给同一个司机）
                stringRedisTemplate.expire(repeatKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES);

                NewOrderDataVo newOrderDataVo = NewOrderDataVo.builder()
                        .orderId(newOrderTaskVo.getOrderId())
                        .startLocation(newOrderTaskVo.getStartLocation())
                        .endLocation(newOrderTaskVo.getEndLocation())
                        .expectAmount(newOrderTaskVo.getExpectAmount())
                        .expectDistance(newOrderTaskVo.getExpectDistance())
                        .expectTime(newOrderTaskVo.getExpectTime())
                        .favourFee(newOrderTaskVo.getFavourFee())
                        .distance(driver.getDistance())
                        .createTime(newOrderTaskVo.getCreateTime())
                        .build();

                //将消息保存到司机的临时队列里面，司机接单了会定时轮询到他的临时队列获取订单消息
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driver.getDriverId();
                stringRedisTemplate.opsForList().leftPush(key, JSONObject.toJSONString(newOrderDataVo));
                //过期时间：1分钟，1分钟未消费，自动过期
                //注：司机端开启接单，前端每5秒（远小于1分钟）拉取1次“司机临时队列”里面的新订单消息
                stringRedisTemplate.expire(key, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
                log.info("该新订单信息已放入司机临时队列: {}", JSON.toJSONString(newOrderDataVo));
            }
        });
        return true;
    }


    /**
     * 乘客下单后，添加并开始新订单任务调度
     *
     * @param newOrderTaskVo 订单任务对象
     * @return 该任务调度的id
     */
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        // 1、先查数据库中有没有这个任务
        OrderJob orderJob = orderJobMapper.selectOne(new LambdaQueryWrapper<OrderJob>().eq(OrderJob::getOrderId, newOrderTaskVo.getOrderId()));
        // 2、没有则创建任务
        if (null == orderJob) {
            //  每1分钟执行一次，处理任务的bean为：newOrderTaskHandler
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler",
                    "",
                    "0 0/1 * * * ?",
                    "新订单任务,订单id：" + newOrderTaskVo.getOrderId()
            );

            // 3、记录订单与任务的关联信息
            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));

            final OrderJob finalOrderJob = orderJob;
            try {
                // 4、插入数据库
                transactionTemplate.execute(action -> {
                    try {
                        return orderJobMapper.insert(finalOrderJob);
                    } catch (Exception e) {
                        action.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                //XXL-Job 任务是在事务之外通过 HTTP 先创建出来的，
                //如果 order_job 落库失败就会留下一个永远没人管理的"孤儿任务"，
                //这里必须把刚创建的任务删掉，保证两边状态一致。
                log.error("订单 {} 的调度任务关联落库失败，回滚已创建的 XXL-Job 任务 jobId={}",
                        newOrderTaskVo.getOrderId(), jobId, e);
                try {
                    xxlJobClient.removeJob(jobId);
                } catch (Exception ex) {
                    log.error("删除孤儿调度任务失败，jobId={}", jobId, ex);
                }
                throw e;
            }
        }
        return orderJob.getJobId();
    }
}



