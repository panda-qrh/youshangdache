package com.youshangdache.map.service.impl;

import com.alibaba.fastjson.JSON;
import com.youshangdache.common.constant.OrderDistanceConstant;
import com.youshangdache.common.constant.RedisConstant;
import com.youshangdache.common.constant.SystemConstant;
import com.youshangdache.common.util.LocationUtil;
import com.youshangdache.driver.DriverInfoFeignClient;
import com.youshangdache.map.repository.OrderServiceLocationRepository;
import com.youshangdache.map.service.LocationService;
import com.youshangdache.model.entity.driver.DriverSet;
import com.youshangdache.model.entity.map.OrderServiceLocation;
import com.youshangdache.model.form.map.OrderServiceLocationForm;
import com.youshangdache.model.form.map.SearchNearByDriverForm;
import com.youshangdache.model.form.map.UpdateDriverLocationForm;
import com.youshangdache.model.form.map.UpdateOrderLocationForm;
import com.youshangdache.model.vo.map.NearByDriverVo;
import com.youshangdache.model.vo.map.OrderLocationVo;
import com.youshangdache.model.vo.map.OrderServiceLastLocationVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;
    @Resource
    private OrderServiceLocationRepository orderServiceLocationRepository;
    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 代驾服务：计算订单实际里程
     *
     * <p>
     * 把MongoDB中该订单的GPS定位坐标都给取出来，以时间排序，连接成连线，这个线的距离就是时间里程
     * </p>
     *
     * @param orderId 订单id
     * @return 订单实际的公里数
     */
    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {
        // 根据订单id获取代驾订单位置信息，根据创建时间升序排序
        List<OrderServiceLocation> list = orderServiceLocationRepository.findByOrderIdOrderByCreateTimeAsc(orderId);
        //返回查询订单位置信息list集合
        //把list集合遍历，得到每个位置信息,计算两个地点的位置
        double realDistance = 0;
        if (!list.isEmpty()) {
            for (int i = 0, size = list.size() - 1; i < size; i++) {
                OrderServiceLocation location1 = list.get(i);
                OrderServiceLocation location2 = list.get(i + 1);
                double distance = LocationUtil.getDistance(
                        location1.getLatitude().doubleValue(),
                        location1.getLongitude().doubleValue(),
                        location2.getLatitude().doubleValue(),
                        location2.getLongitude().doubleValue()
                );
                realDistance += distance;
            }
        }
        //LocationUtil.getDistance 返回的单位是米，而计费规则与 expectDistance 用的都是公里，这里统一换算成公里
        if (realDistance <= 0) {
            //没有采集到轨迹点时返回 0 公里，由调用方按 0 里程计价（不能再拿预估金额冒充里程）
            log.warn("订单 {} 没有可用的代驾轨迹点，实际里程按 0 公里计算", orderId);
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(realDistance)
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
    }

    /**
     * 代驾服务：获取订单服务最后一个位置信息
     *
     * <p>
     * 司机开始代驾后，乘客端获取司机的动向，定时获取上面更新的最后一个位置信息。
     * </p>
     *
     * @param orderId 订单id
     * @return 最后一个坐标位置
     */
    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        OrderServiceLocation orderServiceLocation = mongoTemplate.findOne(
                Query.query(Criteria.where("orderId").is(orderId))
                        .with(Sort.by(Sort.Order.desc("createTime")))
                        .limit(1),
                OrderServiceLocation.class
        );
        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        if (orderServiceLocation != null) {
            BeanUtils.copyProperties(orderServiceLocation, orderServiceLastLocationVo);
        }
        return orderServiceLastLocationVo;
    }

    /**
     * 批量保存代驾服务订单位置
     *
     * <p>
     * 司机开始代驾后，为了减少请求次数，司机端会实时收集变更的GPS定位信息，定时批量上传到后台服务器
     * </p>
     *
     * @param orderServiceLocationForms
     * @return true
     */
    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationForms) {
        List<OrderServiceLocation> list = new ArrayList<>();
        orderServiceLocationForms.forEach(orderServiceLocationForm -> {
            OrderServiceLocation location = new OrderServiceLocation();
            BeanUtils.copyProperties(orderServiceLocationForm, location);
            location.setId(ObjectId.get().toString());
            location.setCreateTime(new Date());
            list.add(location);
        });
        orderServiceLocationRepository.saveAll(list);

        return true;
    }

    /**
     * 司机赶往代驾起始点，更新订单经纬度位置
     *
     * <p>
     * 从redis中获取订单的坐标
     * </p>
     *
     * @param orderId 订单id
     * @return 订单的坐标
     */
    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        return JSON.parseObject(stringRedisTemplate.opsForValue().get(RedisConstant.UPDATE_ORDER_LOCATION + orderId),
                OrderLocationVo.class);
    }

    /**
     * 司机赶往代驾起始点，更新订单地址到缓存
     *
     * <p>
     * 司机赶往代驾点，实时更新司机的经纬度位置到Redis缓存，乘客端可以看见司机的动向，司机端更新，乘客端获取
     * </p>
     *
     * @param updateOrderLocationForm 订单的坐标，即用户下单时的坐标
     * @return true
     */
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        //订单坐标是代驾过程中的临时数据，必须设置过期时间，否则会一直占用 Redis 内存
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(orderLocationVo),
                RedisConstant.UPDATE_ORDER_LOCATION_EXPIRES_TIME, TimeUnit.MINUTES);

        return true;
    }

    /**
     * 司机端的小程序开启接单服务后，开始实时上传司机的定位信息到redis的GEO缓存，
     * 前面乘客已经下单，现在我们就要查找附近适合接单的司机，如果有对应的司机，那就给司机发送新订单消息。
     *
     * @param searchNearByDriverForm 附近司机
     * @return 附近司机集合
     */
    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        //搜索经纬度中5公里以内的司机
        //注意：Spring Data Redis 的 Point(x, y) 中 x 是经度、y 是纬度，写入与检索必须保持一致
        Circle circle = new Circle(
                new Point(searchNearByDriverForm.getLongitude().doubleValue(), searchNearByDriverForm.getLatitude().doubleValue()),
                new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, RedisGeoCommands.DistanceUnit.KILOMETERS)
        );
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                //按距离由近到远排序并限制数量，保证优先用最近的司机、且不会一次性推送给海量司机
                .sortAscending()
                .limit(SystemConstant.NEARBY_DRIVER_LIMIT);
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);
        //3.返回计算后的信息
        List<NearByDriverVo> list = new ArrayList();
        if (results == null || results.getContent().isEmpty()) {
            //没有附近司机时返回空集合，避免调用方直接 forEach 造成 NPE
            return list;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : results.getContent()) {
            Long driverId = Long.parseLong(item.getContent().getName());
            BigDecimal currentDistance = new BigDecimal(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);
            DriverSet driverSet = driverInfoFeignClient.getDriverSettingInfo(driverId);
            //司机没有配置接单设置时，按"不限制"处理，避免空指针
            if (driverSet == null) {
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);
                continue;
            }
            if (driverSet.getAcceptDistance() != null
                    && !driverSet.getAcceptDistance().equals(BigDecimal.ZERO)
                    && driverSet.getAcceptDistance().compareTo(currentDistance) < 0) {
                //超出司机设置的最大接单距离
                continue;
            }
            if (driverSet.getOrderDistance() != null
                    && driverSet.getOrderDistance().doubleValue() != OrderDistanceConstant.ORDER_DISTANCE_NO_LIMITATION
                    && driverSet.getOrderDistance().compareTo(searchNearByDriverForm.getMileageDistance()) < 0) {
                //订单里程超出司机设置的最大接单里程
                continue;
            }
            NearByDriverVo nearByDriverVo = new NearByDriverVo();
            nearByDriverVo.setDriverId(driverId);
            nearByDriverVo.setDistance(currentDistance);
            list.add(nearByDriverVo);
        }
        return list;
    }

    /**
     * 开启接单服务：更新司机经纬度位置
     *
     * <p>
     * 将司机的定位坐标存储在redis中<br>
     * 乘客下单后寻找5公里范围内开启接单服务的司机，通过Redis GEO进行计算
     * </p>
     *
     * @param updateDriverLocationForm 更新司机位置对象
     * @return true
     */
    @Override
    public void updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(), updateDriverLocationForm.getLatitude().doubleValue());
        stringRedisTemplate.opsForGeo()
                .add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString());
    }

    /**
     * 接单结束，关闭接单服务：删除司机经纬度位置
     *
     * <p>
     * 将司机的定位坐标从redis中删除
     * </p>
     *
     * @param driverId 司机id
     * @return true
     */
    @Override
    public void removeDriverLocation(Long driverId) {
        stringRedisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
    }
}
