package com.youshangdache.driver.controller;

import com.youshangdache.common.annotation.Login;
import com.youshangdache.common.result.Result;
import com.youshangdache.common.util.AuthContextHolder;
import com.youshangdache.driver.service.DriverService;
import com.youshangdache.model.form.driver.DriverFaceModelForm;
import com.youshangdache.model.form.driver.UpdateDriverAuthInfoForm;
import com.youshangdache.model.vo.driver.DriverAuthInfoVo;
import com.youshangdache.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value = "/driver")
public class DriverController {
    @Resource
    private DriverService driverService;

    /**
     * 司机端-小程序授权登录
     *
     * @param code 微信临时票据
     * @return token
     */
    @Operation(summary = "司机端-小程序授权登录")
    @GetMapping(value = "/login/{code}")
    public Result<String> login(@PathVariable String code) {
        return Result.ok(driverService.login(code));
    }

    /**
     * 司机端-获取登录后的司机信息
     *
     * @return 司机登录后的司机基本信息
     */
    @Operation(summary = "司机端-获取登录后的司机信息")
    @GetMapping(value = "/getDriverLoginInfo")
    @Login
    public Result<DriverLoginVo> getDriverLoginInfo() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.getDriverLoginInfo(driverId));
    }

    @Operation(summary = "获取司机认证信息")
    @Login
    @GetMapping("/getDriverAuthInfo")
    public Result<DriverAuthInfoVo> getDriverAuthInfo() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.getDriverAuthInfo(driverId));
    }

    @Operation(summary = "更新司机认证信息")
    @Login
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        updateDriverAuthInfoForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.updateDriverAuthInfo(updateDriverAuthInfoForm));
    }

    @Operation(summary = "创建司机人脸模型")
    @Login
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.creatDriverFaceModel(driverFaceModelForm));
    }

    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @return true当日已进行过人脸识别 | false当日未进行人脸识别
     */
    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @Login
    @PostMapping("/isFaceRecognition")
    public Result<Boolean> isFaceRecognition() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @Login
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    /**
     * 更新司机的接单状态为开启接单状态
     *
     * <p>
     * 司机完成了当日人脸认证后，开启接单，然后删除司机在redis中的位置，及清空司机的临时订单列表数据
     * </p>
     *
     * @return true
     */
    @Operation(summary = "开始接单服务")
    @Login
    @PostMapping("/startService")
    public Result<Boolean> startService() {
        return Result.ok(driverService.startService(AuthContextHolder.getUserId()));
    }
    /**
     * 司机抢成功单，就要关闭接单服务。
     *
     * @return true
     */
    @Operation(summary = "停止接单服务")
    @Login
    @PostMapping("/stopService")
    public Result<Boolean> stopService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.stopService(driverId));
    }
}

