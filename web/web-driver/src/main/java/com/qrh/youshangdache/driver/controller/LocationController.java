package com.qrh.youshangdache.driver.controller;

import com.qrh.youshangdache.common.annotation.Login;
import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.common.util.AuthContextHolder;
import com.qrh.youshangdache.driver.service.LocationService;
import com.qrh.youshangdache.model.form.map.OrderServiceLocationForm;
import com.qrh.youshangdache.model.form.map.UpdateDriverLocationForm;
import com.qrh.youshangdache.model.form.map.UpdateOrderLocationForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "位置API接口管理")
@RestController
@RequestMapping(value="/location")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {

    @Autowired
    private LocationService locationService;
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
    @Operation(summary = "开启接单服务：更新司机经纬度位置")
    @Login
    @PostMapping("/updateDriverLocation")
    public Result<Void> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm) {
        Long driverId = AuthContextHolder.getUserId();
        updateDriverLocationForm.setDriverId(driverId);
        locationService.updateDriverLocation(updateDriverLocationForm);
        return Result.ok();
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
    @Operation(summary = "司机赶往代驾起始点，更新订单地址到缓存")
    @Login
    @PostMapping("/updateOrderLocationToCache")
    public Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm) {
        return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
    }
    @Operation(summary = "批量保存代驾服务订单位置")
    @Login
    @PostMapping("/saveOrderServiceLocation")
    public Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderServiceLocationForms) {
        return Result.ok(locationService.saveOrderServiceLocation(orderServiceLocationForms));
    }
}

