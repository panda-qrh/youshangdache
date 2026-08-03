package com.qrh.youshangdache.driver.controller;

import com.qrh.youshangdache.common.login.Login;
import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.common.util.AuthContextHolder;
import com.qrh.youshangdache.driver.service.OrderService;
import com.qrh.youshangdache.model.entity.order.OrderInfo;
import com.qrh.youshangdache.model.enums.OrderStatusEnum;
import com.qrh.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.qrh.youshangdache.model.form.order.OrderFeeForm;
import com.qrh.youshangdache.model.form.order.StartDriveForm;
import com.qrh.youshangdache.model.form.order.UpdateOrderCartForm;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.model.vo.map.DrivingLineVo;
import com.qrh.youshangdache.model.vo.map.OrderServiceLastLocationVo;
import com.qrh.youshangdache.model.vo.order.CurrentOrderInfoVo;
import com.qrh.youshangdache.model.vo.order.NewOrderDataVo;
import com.qrh.youshangdache.model.vo.order.OrderInfoVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 查询订单状态
     *
     * <p>
     * 乘客下完单后，订单状态为1（等待接单），乘客端小程序会轮询订单状态，当订单状态为2（司机已接单）时，说明已经有司机接单了，那么页面进行跳转，进行下一步操作
     * </p>
     *
     * @param orderId 订单id
     * @return 订单状态代号
     */
    @Operation(summary = "查询订单状态")
    @Login
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<OrderStatusEnum> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    /**
     * 查询司机的最新订单数据
     *
     * @param driverId 司机id
     * @return
     */
    @Operation(summary = "查询司机的最新订单数据")
    @Login
    @PostMapping("/findNewOrderQueueData/{driverId}")
    public Result<List<NewOrderDataVo>> findNewOrderQueueData(@PathVariable Long driverId) {
        return Result.ok(orderService.findNewOrderQueueData(driverId));
    }

    /**
     * 查找司机端当前订单
     *
     * <p>
     * 司机只要有执行中的订单，没有结束，那么司机是不可以接单的，页面会弹出层，进入执行中的订单
     * </p>
     *
     * @return 司机当前正在执行的订单数据
     */
    @Operation(summary = "司机端查找当前订单")
    @Login
    @GetMapping("/searchDriverCurrentOrder ")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
        return Result.ok(orderService.searchDriverCurrentOrder(AuthContextHolder.getUserId()));
    }

    @Operation(summary = "司机抢单")
    @Login
    @GetMapping("/robNewOrder/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.robNewOrder(driverId, orderId));
    }

    /**
     * 获取执行中的订单
     *
     * @param orderId 订单id
     * @return 执行中的订单的数据
     */
    @Operation(summary = "根据订单id得到订单信息")
    @Login
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfoByOrderId(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderInfoByOrderId(orderId, AuthContextHolder.getUserId()));
    }

    /**
     * 计算最佳驾驶路线-司乘同显
     *
     * @param calculateDrivingLineForm 起点坐标和终点坐标对象
     * @return 路线
     */
    @Operation(summary = "计算最佳驾驶路线")
    @Login
    @GetMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    /**
     * 司机到达起始点
     *
     * @param orderId 订单id
     * @return true
     */
    @Operation(summary = "司机到达起始点")
    @Login
    @GetMapping("/driverArriveStartLocation/{orderId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId) {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(orderService.driverArriveStartLocation(orderId, driverId));
    }

    /**
     * 更新代驾车辆信息
     *
     * <p>
     * 司机到达代驾起始点，联系了乘客，见到了代驾车辆，要拍照与录入车辆信息
     * </p>
     *
     * @param updateOrderCartForm
     * @return true
     */
    @Operation(summary = "更新代驾车辆信息")
    @Login
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm) {
        updateOrderCartForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(orderService.updateOrderCart(updateOrderCartForm));
    }

    /**
     * 开始代驾服务
     *
     * @param startDriveForm
     * @return true
     */
    @Operation(summary = "开始代驾服务")
    @Login
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm) {
        startDriveForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(orderService.startDrive(startDriveForm));
    }

    /**
     * 代驾服务：获取订单服务最后一个位置信息
     *
     * @param orderId 订单id
     * @return 最后一个坐标位置
     */
    @Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
    @Login
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderServiceLastLocation(orderId));
    }

    /**
     * 结束代驾服务更新订单账单
     *
     * @param orderFeeForm 订单费用
     * @return true
     */
    @Operation(summary = "结束代驾服务更新订单账单")
    @Login
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody OrderFeeForm orderFeeForm) {
        orderFeeForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(orderService.endDrive(orderFeeForm));
    }

    /**
     * 获取司机订单分页列表
     *
     * @param limit 页限制
     * @param page  页码
     * @return 订单分页
     */
    @Operation(summary = "获取司机订单分页列表")
    @Login
    @GetMapping("/findDriverOrderPage/{page}/{limit}")
    public Result<PageVo> findDriverOrderPage(@PathVariable Long limit,
                                              @PathVariable Long page) {
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        PageVo pageVo = orderService.findDriverOrderPage(pageParam, AuthContextHolder.getUserId());
        pageVo.setPage(page);
        pageVo.setLimit(limit);
        return Result.ok(pageVo);
    }

    /**
     * 发送账单信息
     *
     * <p>
     * 司机端确认账单信息后，点击“发送账单”，乘客端才能切换到未支付账单页面，发送账单其实就是更新订单流程中的一个状态。
     * </p>
     *
     * @param orderId  订单id
     * @return true
     */
    @Operation(summary = "发送账单信息")
    @Login
    @GetMapping("/sendOrderBillInfo/{orderId}")
    public Result<Boolean> sendOrderBillInfo(@PathVariable Long orderId) {
        return Result.ok(orderService.sendOrderBillInfo(orderId, AuthContextHolder.getUserId()));
    }

}

