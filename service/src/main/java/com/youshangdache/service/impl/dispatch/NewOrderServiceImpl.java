package com.youshangdache.service.impl.dispatch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.youshangdache.common.constant.RedisConstant;
import com.youshangdache.mapper.dispatch.OrderJobMapper;
import com.youshangdache.service.dispatch.NewOrderService;
import com.youshangdache.config.dispatch.XxlJobClient;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {
    @Autowired
    private XxlJobClient xxlJobClient;
    @Autowired
    private OrderJobMapper orderJobMapper;
    @Autowired
    private LocationFeignClient locationFeignClient;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
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
        return stringRedisTemplate.delete(key);
    }

    /**
     * 查询司机的最新订单数据
     * @param driverId 司机id
     * @return
     */
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        if (stringRedisTemplate.opsForList().size(key) > 0) {
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
        //给司机派发订单信息
        nearByDriverVoList.forEach(driver -> {
            //记录司机id，防止重复推送订单信息
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST + newOrderTaskVo.getOrderId();
            boolean isMember = stringRedisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());
            if (!isMember) {
                //记录该订单已放入司机临时容器
                stringRedisTemplate.opsForSet().add(repeatKey, driver.getDriverId().toString());
                //过期时间：15分钟，新订单15分钟没人接单自动取消
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
            // 4、插入数据库
            transactionTemplate.execute(action -> {
                try {
                    return orderJobMapper.insert(finalOrderJob);
                } catch (Exception e) {
                    action.setRollbackOnly();
                    throw new RuntimeException(e);
                }
            });
        }
        return orderJob.getJobId();
    }
}



