package com.youshangdache.driver.service.impl;

import com.youshangdache.common.constant.RedisConstant;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.dispatch.NewOrderFeignClient;
import com.youshangdache.driver.DriverInfoFeignClient;
import com.youshangdache.driver.service.DriverService;
import com.youshangdache.map.LocationFeignClient;
import com.youshangdache.model.enums.AuthStatusEnum;
import com.youshangdache.model.enums.DriverServiceStatusEnum;
import com.youshangdache.model.form.driver.DriverFaceModelForm;
import com.youshangdache.model.form.driver.UpdateDriverAuthInfoForm;
import com.youshangdache.model.vo.driver.DriverAuthInfoVo;
import com.youshangdache.model.vo.driver.DriverLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DriverServiceImpl implements DriverService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;
    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private NewOrderFeignClient newOrderFeignClient;

    /**
     * 司机接单成功后，关闭接单功能
     *
     * @param driverId 司机id
     * @return true
     */
    @Override
    public Boolean stopService(Long driverId) {
        //更新司机的接单状态
        driverInfoFeignClient.updateServiceStatus(driverId, DriverServiceStatusEnum.DRIVER_NOT_SERVICE.getCode());
        //删除司机的位置信息
        locationFeignClient.removeDriverLocation(driverId);
        //清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }

    /**
     * 更新司机的接单状态为开启接单状态
     * <p>
     * 司机完成了当日人脸认证后，开启接单，然后删除司机在redis中的位置，及清空司机的临时订单列表数据
     * </p>
     *
     * @param driverId 司机id
     * @return true
     */
    @Override
    public Boolean startService(Long driverId) {
        //判断是否完成了验证
        DriverLoginVo driverLoginVo = driverInfoFeignClient.getDriverLoginInfo(driverId);
        if (driverLoginVo.getAuthStatus() != AuthStatusEnum.AUTHENTICATION_PASSED) {
            throw new GuiguException(ResultCodeEnum.DRIVER_NOT_AUTH);
        }
        //判断当日是否人脸识别
        Boolean isFaceRecognition = driverInfoFeignClient.isFaceRecognition(driverId);
        if (!isFaceRecognition) {
            throw new GuiguException(ResultCodeEnum.DRIVER_NOT_FACIAL_RECOGNITION);
        }
        //更新司机服务状态
        driverInfoFeignClient.updateServiceStatus(driverId, DriverServiceStatusEnum.DRIVER_START_SERVICE.getCode());
        //删除redis的司机的位置信息
        locationFeignClient.removeDriverLocation(driverId);
        //清空司机临时订单数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }


    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm);
    }

    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @return true当日已进行过人脸识别 | false当日未进行人脸识别
     */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return driverInfoFeignClient.isFaceRecognition(driverId);
    }

    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm);
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        return driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm);
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        return driverInfoFeignClient.getDriverAuthInfo(driverId);
    }

    /**
     * 司机端-获取登录后的司机信息
     *
     * @param driverId 司机id
     * @return 司机登录后的司机基本信息
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        return driverInfoFeignClient.getDriverLoginInfo(driverId);
    }

    /**
     * 司机端-小程序授权登录
     *
     * @param code 微信临时票据
     * @return token
     */
    @Override
    public String login(String code) {
        //token字符串
        String token = UUID.randomUUID().toString().replace("-", "");
        //放到redis，设置过期时间
        stringRedisTemplate.opsForValue()
                .set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                        driverInfoFeignClient.login(code).toString(),
                        RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                        TimeUnit.SECONDS);
        return token;
    }


}
