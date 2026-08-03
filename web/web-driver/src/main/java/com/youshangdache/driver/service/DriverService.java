package com.youshangdache.driver.service;

import com.youshangdache.model.form.driver.DriverFaceModelForm;
import com.youshangdache.model.form.driver.UpdateDriverAuthInfoForm;
import com.youshangdache.model.vo.driver.DriverAuthInfoVo;
import com.youshangdache.model.vo.driver.DriverLoginVo;

public interface DriverService {

    /**
     * 司机端-小程序授权登录
     * @param code 微信临时票据
     * @return token
     */
    String login(String code);

    /**
     * 司机端-获取登录后的司机信息
     * @param driverId 司机id
     * @return 司机登录后的司机基本信息
     */
    DriverLoginVo getDriverLoginInfo(Long driverId);

    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);
    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @return true当日已进行过人脸识别 | false当日未进行人脸识别
     */
    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);
    /**
     * 更新司机的接单状态为开启接单状态
     *<p>
     *     司机完成了当日人脸认证后，开启接单，然后删除司机在redis中的位置，及清空司机的临时订单列表数据
     *</p>
     * @param driverId 司机id
     * @return true
     */
    Boolean startService(Long driverId);
    /**
     * 司机抢成功单，就要关闭接单服务。
     *
     * @param driverId 司机id
     * @return true
     */
    Boolean stopService(Long driverId);
}
