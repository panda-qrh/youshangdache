package com.qrh.youshangdache.driver.service.impl;

import com.qrh.youshangdache.common.execption.GuiguException;
import com.qrh.youshangdache.common.result.ResultCodeEnum;
import com.qrh.youshangdache.driver.DriverInfoFeignClient;
import com.qrh.youshangdache.driver.service.LocationService;
import com.qrh.youshangdache.map.LocationFeignClient;
import com.qrh.youshangdache.model.entity.driver.DriverSet;
import com.qrh.youshangdache.model.enums.DriverServiceStatusEnum;
import com.qrh.youshangdache.model.form.map.OrderServiceLocationForm;
import com.qrh.youshangdache.model.form.map.UpdateDriverLocationForm;
import com.qrh.youshangdache.model.form.map.UpdateOrderLocationForm;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {
    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderServiceLocationForms) {
        return locationFeignClient.saveOrderServiceLocation(orderServiceLocationForms).getData();
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
        return locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm).getData();
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
        Long driverId = updateDriverLocationForm.getDriverId();
        DriverSet driverSet = driverInfoFeignClient.getDriverSettingInfo(driverId).getData();
        if (driverSet.getServiceStatus()==DriverServiceStatusEnum.DRIVER_START_SERVICE) {
            locationFeignClient.updateDriverLocation(updateDriverLocationForm);
        } else {
            throw new GuiguException(ResultCodeEnum.DRIVER_NOT_START_SERVICE);
        }
    }
}
