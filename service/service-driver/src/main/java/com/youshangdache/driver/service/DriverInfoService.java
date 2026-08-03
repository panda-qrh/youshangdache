package com.youshangdache.driver.service;

import com.youshangdache.model.entity.driver.DriverInfo;
import com.youshangdache.model.entity.driver.DriverSet;
import com.youshangdache.model.form.driver.DriverFaceModelForm;
import com.youshangdache.model.form.driver.UpdateDriverAuthInfoForm;
import com.youshangdache.model.vo.driver.DriverAuthInfoVo;
import com.youshangdache.model.vo.driver.DriverInfoVo;
import com.youshangdache.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DriverInfoService extends IService<DriverInfo> {

    /**
     * 司机端-小程序授权登录
     * @param code 微信临时票据
     * @return 用户id
     */
    Long login(String code);
    /**
     * 司机端-获取登录后的司机信息
     * @param driverId 司机id
     * @return 司机登录后的司机基本信息
     */
    DriverLoginVo getDriverLoginInfo(Long driverId);
    /**
     * 获取司机认证信息
     * @param driverId 司机id
     * @return
     */
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    /**
     * 获取司机设置信息
     * @param driverId 司机id
     * @return 司机的设置信息
     */
    DriverSet getDriverSet(Long driverId);
    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @param driverId 司机id
     * @return true当日已进行过人脸识别 | false当日未进行人脸识别
     */
    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);
    /**
     * 更新司机的接单状态
     *
     * <p>
     *     司机完成当日人脸认证后，就默认司机开启接单了
     * </p>
     * @param driverId 司机id
     * @param status 司机当前的接单状态，由未接单改为开始接单
     * @return true更新司机接单状态成功 | 更新司机接单状态失败
     */
    Boolean updateServiceStatus(Long driverId, Integer status);
    /**
     * 获取司机基本信息
     *
     * @param driverId 司机id
     * @return 司机基本信息
     */
    DriverInfoVo getDriverInfoOrder(Long driverId);

    String getDriverOpenId(Long driverId);
}
