package com.youshangdache.driver;

import com.youshangdache.model.entity.driver.DriverSet;
import com.youshangdache.model.form.driver.DriverFaceModelForm;
import com.youshangdache.model.form.driver.UpdateDriverAuthInfoForm;
import com.youshangdache.model.vo.driver.DriverAuthInfoVo;
import com.youshangdache.model.vo.driver.DriverInfoVo;
import com.youshangdache.model.vo.driver.DriverLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service", path = "/driver/info")
public interface DriverInfoFeignClient {

    /**
     * 司机端-小程序授权登录
     *
     * @param code 微信临时票据
     * @return userID
     */
    @GetMapping(value = "/login/{code}")
    public Long login(@PathVariable String code);

    /**
     * 司机端-获取登录后的司机信息
     *
     * @param driverId 司机id
     * @return 司机登录后的司机基本信息
     */
    @GetMapping(value = "/getDriverLoginInfo/{driverId}")
    public DriverLoginVo getDriverLoginInfo(@PathVariable Long driverId);

    @GetMapping("/getDriverAuthInfo/{driverId}")
    DriverAuthInfoVo getDriverAuthInfo(@PathVariable("driverId") Long driverId);

    @PostMapping("/updateDriverAuthInfo")
    Boolean UpdateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    @PostMapping("/creatDriverFaceModel")
    Boolean creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm);

    /**
     * 获取司机设置信息
     *
     * @param driverId 司机id
     * @return 司机的设置信息
     */
    @PostMapping("/getDriverSet/{driverId}")
    public DriverSet getDriverSettingInfo(@PathVariable Long driverId);

    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @param driverId 司机id
     * @return true当日已进行过人脸识别 | false当日未进行人脸识别
     */
    @PostMapping("/isFaceRecognition/{driverId}")
    public Boolean isFaceRecognition(@PathVariable Long driverId);

    @PostMapping("/verifyDriverFace")
    public Boolean verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm);
    /**
     * 更新司机的接单状态为开启接单状态
     *<p>
     *     司机完成了当日人脸认证后，开启接单，然后删除司机在redis中的位置，及清空司机的临时订单列表数据
     *</p>
     * @param driverId 司机id
     * @param status 司机接单状态
     * @return true更新司机接单状态成功 | 更新司机接单状态失败
     */
    @PostMapping("/updateServiceStatus/{driverId}/{status}")
    public Boolean updateServiceStatus(@PathVariable Long driverId, @PathVariable Integer status);

    /**
     * 获取司机基本信息
     * @param driverId 司机id
     * @return 司机基本信息
     */
    @PostMapping("/getDriverInfo/{driverId}")
    public DriverInfoVo getDriverInfo(@PathVariable Long driverId);

    @GetMapping("/getDriverOpenId/{driverId}")
    public String getDriverOpenId(@PathVariable Long driverId);

}