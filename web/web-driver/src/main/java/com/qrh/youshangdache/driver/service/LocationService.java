package com.qrh.youshangdache.driver.service;

import com.qrh.youshangdache.model.form.map.OrderServiceLocationForm;
import com.qrh.youshangdache.model.form.map.UpdateDriverLocationForm;
import com.qrh.youshangdache.model.form.map.UpdateOrderLocationForm;

import java.util.List;

public interface LocationService {
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
    void updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);
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

    Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationForms);
}
