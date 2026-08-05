package com.youshangdache.service.map;

import com.youshangdache.model.form.map.OrderServiceLocationForm;
import com.youshangdache.model.form.map.SearchNearByDriverForm;
import com.youshangdache.model.form.map.UpdateDriverLocationForm;
import com.youshangdache.model.form.map.UpdateOrderLocationForm;
import com.youshangdache.model.vo.map.NearByDriverVo;
import com.youshangdache.model.vo.map.OrderLocationVo;
import com.youshangdache.model.vo.map.OrderServiceLastLocationVo;

import java.math.BigDecimal;
import java.util.List;

public interface LocationService {
    /**
     * 开启接单服务：更新司机经纬度位置
     *
     * @param updateDriverLocationForm 更新司机位置对象
     * @return true
     */
    void updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    /**
     * 关闭接单服务：删除司机经纬度位置
     *
     * @param driverId 司机id
     * @return true
     */
    void removeDriverLocation(Long driverId);

    /**
     * 司机端的小程序开启接单服务后，开始实时上传司机的定位信息到redis的GEO缓存，
     * 前面乘客已经下单，现在我们就要查找附近适合接单的司机，如果有对应的司机，那就给司机发送新订单消息。
     *
     * @param searchNearByDriverForm 附近司机
     * @return 附近司机集合
     */
    List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm);

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
    Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);

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
    OrderLocationVo getCacheOrderLocation(Long orderId);

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
    Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationForms);

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
    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);

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
    BigDecimal calculateOrderRealDistance(Long orderId);
}
