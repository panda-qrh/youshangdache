package com.qrh.youshangdache.map.controller;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.map.service.LocationService;
import com.qrh.youshangdache.model.form.map.OrderServiceLocationForm;
import com.qrh.youshangdache.model.form.map.SearchNearByDriverForm;
import com.qrh.youshangdache.model.form.map.UpdateDriverLocationForm;
import com.qrh.youshangdache.model.form.map.UpdateOrderLocationForm;
import com.qrh.youshangdache.model.vo.map.NearByDriverVo;
import com.qrh.youshangdache.model.vo.map.OrderLocationVo;
import com.qrh.youshangdache.model.vo.map.OrderServiceLastLocationVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Tag(name = "位置API接口管理")
@RestController
@RequestMapping("/map/location")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationController {
    @Resource
    private LocationService locationService;

    /**
     * 开启接单服务：更新司机经纬度位置
     *
     * @param updateDriverLocationForm 更新司机位置对象
     * @return true
     */
    @Operation(summary = "开启接单服务：更新司机经纬度位置")
    @PostMapping("/updateDriverLocation")
    public Result<Void> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm) {
        locationService.updateDriverLocation(updateDriverLocationForm);
        return Result.ok();
    }

    /**
     * 关闭接单服务：删除司机经纬度位置
     *
     * @param driverId 司机id
     * @return true
     */
    @Operation(summary = "关闭接单服务：删除司机经纬度位置")
    @DeleteMapping("/removeDriverLocation/{driverId}")
    public Result<Void> removeDriverLocation(@PathVariable Long driverId) {
        locationService.removeDriverLocation(driverId);
        return Result.ok();
    }

    /**
     * 司机端的小程序开启接单服务后，开始实时上传司机的定位信息到redis的GEO缓存，
     * 前面乘客已经下单，现在我们就要查找附近适合接单的司机，如果有对应的司机，那就给司机发送新订单消息。
     *
     * @param searchNearByDriverForm 附近司机
     * @return 附近司机集合
     */
    @Operation(summary = "搜索附近满足条件的司机")
    @PostMapping("/searchNearByDriver")
    public Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody SearchNearByDriverForm searchNearByDriverForm) {
        return Result.ok(locationService.searchNearByDriver(searchNearByDriverForm));
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
    @PostMapping("/updateOrderLocationToCache")
    public Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm) {
        return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
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
    @Operation(summary = "司机赶往代驾起始点，更新订单经纬度位置")
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getCacheOrderLocation(@PathVariable Long orderId) {
        return Result.ok(locationService.getCacheOrderLocation(orderId));
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
    @Operation(summary = "批量保存代驾服务订单位置")
    @PostMapping("/saveOrderServiceLocation")
    public Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderServiceLocationForms) {
        return Result.ok(locationService.saveOrderServiceLocation(orderServiceLocationForms));
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
    @Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
        return Result.ok(locationService.getOrderServiceLastLocation(orderId));
    }

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
    @Operation(summary = "代驾服务：计算订单实际里程")
    @GetMapping("/calculateOrderRealDistance/{orderId}")
    public Result<BigDecimal> calculateOrderRealDistance(@PathVariable Long orderId) {
        return Result.ok(locationService.calculateOrderRealDistance(orderId));
    }
}

