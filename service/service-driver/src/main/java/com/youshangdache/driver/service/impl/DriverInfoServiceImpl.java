package com.youshangdache.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.youshangdache.common.config.thread.ThreadPoolConfig;
import com.youshangdache.common.constant.SystemConstant;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.common.util.IpUtil;
import com.youshangdache.driver.config.TencentCloudProperties;
import com.youshangdache.driver.mapper.*;
import com.youshangdache.driver.service.CosService;
import com.youshangdache.driver.service.DriverInfoService;
import com.youshangdache.model.entity.driver.*;
import com.youshangdache.model.enums.AccountStatusEnum;
import com.youshangdache.model.enums.DriverServiceStatusEnum;
import com.youshangdache.model.enums.LoginStatusEnum;
import com.youshangdache.model.form.driver.DriverFaceModelForm;
import com.youshangdache.model.form.driver.UpdateDriverAuthInfoForm;
import com.youshangdache.model.vo.driver.DriverAuthInfoVo;
import com.youshangdache.model.vo.driver.DriverInfoVo;
import com.youshangdache.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {

    @Resource
    private DriverInfoMapper driverInfoMapper;
    @Resource
    private WxMaService wxMaService;
    @Resource
    private DriverSetMapper driverSetMapper;
    @Resource
    private DriverAccountMapper driverAccountMapper;
    @Resource
    private DriverLoginLogMapper driverLoginLogMapper;
    @Resource
    private CosService cosService;
    @Resource
    private TencentCloudProperties tencentCloudProperties;
    @Resource
    private DriverFaceRecognitionMapper driverFaceRecognitionMapper;
    @Resource
    private HttpServletRequest request;

    /**
     * 获取司机openId
     *
     * @param driverId 司机id
     * @return openId
     */
    @Override
    public String getDriverOpenId(Long driverId) {
        LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getId, driverId);
        DriverInfo driverInfo = driverInfoMapper.selectOne(wrapper);
        if (driverInfo == null) throw new GuiguException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        return driverInfo.getWxOpenId();
    }

    /**
     * 获取司机基本信息
     *
     * @param driverId 司机id
     * @return 司机基本信息
     */
    @Override
    public DriverInfoVo getDriverInfoOrder(Long driverId) {
        //司机基本信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        if (driverInfo == null) throw new GuiguException(ResultCodeEnum.ACCOUNT_NOT_EXIST);

        DriverInfoVo driverInfoVo = new DriverInfoVo();
        BeanUtils.copyProperties(driverInfo, driverInfoVo);

        //计算驾龄
        Date licenseIssueDate = driverInfo.getDriverLicenseIssueDate();
        LocalDate now = LocalDate.now();
        int year = Math.abs(
                Period.between(
                        licenseIssueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        now
                ).getYears());
        driverInfoVo.setDriverLicenseAge(year);
        return driverInfoVo;
    }

    /**
     * 更新司机的接单状态
     *
     * <p>
     * 司机完成当日人脸认证后，就默认司机开启接单了
     * </p>
     *
     * @param driverId 司机id
     * @param status   司机当前的接单状态，由未接单改为开始接单
     * @return true更新司机接单状态成功 | 更新司机接单状态失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        LambdaQueryWrapper<DriverSet> queryWrapper = new LambdaQueryWrapper<DriverSet>().eq(DriverSet::getDriverId, driverId);
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(DriverServiceStatusEnum.of(status));
        return driverSetMapper.update(driverSet, queryWrapper) > 0;
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        try {
            VerifyFaceResponse resp = getVerifyFaceResponse(driverFaceModelForm);
            if (resp.getIsMatch()) {
                //照片比对成功,静态活体检测
                Boolean isSuccess = this.detectLiveFace(driverFaceModelForm.getImageBase64());
                if (isSuccess) {
                    DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
                    driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
                    driverFaceRecognition.setFaceDate(new Date());
                    driverFaceRecognitionMapper.insert(driverFaceRecognition);
                    return true;
                }
            }

        } catch (TencentCloudSDKException e) {
            log.error("e: ", e);
        }
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    private VerifyFaceResponse getVerifyFaceResponse(DriverFaceModelForm driverFaceModelForm) throws TencentCloudSDKException {
        Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("iai.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        IaiClient client = new IaiClient(cred,
                tencentCloudProperties.getRegion(),
                clientProfile);
        VerifyFaceRequest request = new VerifyFaceRequest();
        request.setImage(driverFaceModelForm.getImageBase64());
        request.setPersonId(driverFaceModelForm.getDriverId().toString());
        return client.VerifyFace(request);
    }

    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @param driverId 司机id
     * @return true当日已进行过人脸识别 | false当日未进行人脸识别
     */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        LambdaQueryWrapper<DriverFaceRecognition> queryWrapper = new LambdaQueryWrapper<DriverFaceRecognition>()
                .eq(DriverFaceRecognition::getDriverId, driverId)
                .eq(DriverFaceRecognition::getFaceDate, new DateTime().toString("yyyy-MM-dd"));
        Long count = driverFaceRecognitionMapper.selectCount(queryWrapper);
        return count != 0;
    }

    /**
     * 获取司机设置信息
     *
     * @param driverId 司机id
     * @return 司机的设置信息
     */
    @Override
    public DriverSet getDriverSet(Long driverId) {
        return driverSetMapper.selectOne(new LambdaQueryWrapper<DriverSet>().eq(DriverSet::getDriverId, driverId));
    }

    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        DriverInfo driverInfo = this.getById(driverFaceModelForm.getDriverId());
        try {
            Credential cred = new Credential(tencentCloudProperties.getSecretId(), tencentCloudProperties.getSecretKey());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(), clientProfile);
            CreatePersonRequest req = new CreatePersonRequest();
            req.setGroupId(tencentCloudProperties.getPersionGroupId());
            req.setPersonId(String.valueOf(driverInfo.getId()));
            req.setGender(Long.parseLong(driverInfo.getGender()));
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setPersonName(driverInfo.getName());
            req.setImage(driverFaceModelForm.getImageBase64());
            CreatePersonResponse resp = client.CreatePerson(req);
            System.out.println(CreatePersonResponse.toJsonString(resp));
            if (StringUtils.hasText(resp.getFaceId())) {
                driverInfo.setFaceModelId(resp.getFaceId());
                this.updateById(driverInfo);
            }
        } catch (TencentCloudSDKException e) {
            log.error("e: ", e);
            return false;
        }
        return true;
    }

    /**
     * 更新司机认证信息
     *
     * @param updateDriverAuthInfoForm
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(updateDriverAuthInfoForm.getDriverId());
        BeanUtils.copyProperties(updateDriverAuthInfoForm, driverInfo);
        return this.updateById(driverInfo);
    }

    /**
     * 获取司机认证信息
     *
     * @param driverId 司机id
     * @return
     */
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        if (driverInfo == null) throw new GuiguException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        BeanUtils.copyProperties(driverInfo, driverAuthInfoVo);

        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));

        return driverAuthInfoVo;
    }

    /**
     * 司机端-获取登录后的司机信息
     *
     * @param driverId 司机id
     * @return 司机登录后的司机基本信息
     */
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        //根据司机id获取司机信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        if (driverInfo == null) throw new GuiguException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        DriverLoginVo driverLoginVo = new DriverLoginVo();
        BeanUtils.copyProperties(driverInfo, driverLoginVo);
        driverLoginVo.setIsArchiveFace(StringUtils.hasText(driverInfo.getFaceModelId()));//是否建立人脸识别
        return driverLoginVo;
    }

    /**
     * 司机端-小程序授权登录
     *
     * @param code 微信临时票据
     * @return 司机id
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Long login(String code) {
        String openId = null;
        String failMsg = null;
        try {
            //根据code+小程序id+秘钥请求微信接口，返回openid
            openId = wxMaService.getUserService()
                    .getSessionInfo(code)
                    .getOpenid();
        } catch (WxErrorException e) {
            log.warn("微信登录失败，code: {}", code, e);
            failMsg = "微信授权失败";
        }
        if (!StringUtils.hasText(openId)) {
            if (failMsg == null) {
                failMsg = "openId为空";
            }
            recordLoginLog(new DriverLoginLog(null, IpUtil.getIpAddress(request), LoginStatusEnum.FAIL, failMsg));
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        //根据openid查询是否第一次登录
        DriverInfo driverInfo = driverInfoMapper.selectOne(
                new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getWxOpenId, openId)
        );
        //如果是第一次登录，driverInfo应该为null
        boolean isFirstLogin = driverInfo == null;
        if (isFirstLogin) {
            //添加司机基本信息
            driverInfo = initialDriverInfoWithSimple(openId);
            //初始化司机配置
            initialDriverSet(driverInfo.getId());
            //设置司机账户信息
            initialDriverAccountInfo(driverInfo.getId());
        }
        //异步记录司机登录信息
        String msg = isFirstLogin ? "小程序首次登录" : "小程序登录";
        recordLoginLog(new DriverLoginLog(driverInfo.getId(), IpUtil.getIpAddress(request), LoginStatusEnum.SUCCESS, msg));
        //返回司机的id
        return driverInfo.getId();
    }

    /**
     * 异步记录登录日志<br>
     * <p>
     * 线程池：{@link ThreadPoolConfig#loginLogExecutor()}
     *
     * @param loginLog 待记录的对象
     */
    @Async("loginLogExecutor")
    public void recordLoginLog(DriverLoginLog loginLog) {
        driverLoginLogMapper.insert(loginLog);
    }

    /**
     * 初始化司机的账户信息
     *
     * @param driverId 司机的id
     */
    private void initialDriverAccountInfo(Long driverId) {
        DriverAccount driverAccount = new DriverAccount();
        driverAccount.setDriverId(driverId);
        driverAccount.setTotalAmount(BigDecimal.ZERO);
        driverAccount.setLockAmount(BigDecimal.ZERO);
        driverAccount.setAvailableAmount(BigDecimal.ZERO);
        driverAccount.setTotalIncomeAmount(BigDecimal.ZERO);
        driverAccount.setTotalPayAmount(BigDecimal.ZERO);
        driverAccountMapper.insert(driverAccount);
    }

    /**
     * 初始化司机的设置
     *
     * @param driverId 司机id
     */
    private void initialDriverSet(Long driverId) {
        DriverSet driverSet = new DriverSet();
        driverSet.setDriverId(driverId);
        driverSet.setServiceStatus(DriverServiceStatusEnum.DRIVER_NOT_SERVICE);
        driverSet.setOrderDistance(BigDecimal.ZERO);
        driverSet.setAcceptDistance(new BigDecimal(String.valueOf(SystemConstant.ACCEPT_DISTANCE)));
        driverSet.setIsAutoAccept(Boolean.FALSE);
        driverSetMapper.insert(driverSet);
    }

    /**
     * 简单初始化司机的信息（信息完不完整）
     *
     * @param openId 微信openId
     */
    @NotNull
    private DriverInfo initialDriverInfoWithSimple(String openId) {
        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setNickname("用户" + System.currentTimeMillis());
        driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
        driverInfo.setWxOpenId(openId);
        driverInfo.setStatus(AccountStatusEnum.NORMAL);
        driverInfoMapper.insert(driverInfo);
        return driverInfo;
    }


    private Boolean detectLiveFace(String img) {
        try {
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),tencentCloudProperties.getSecretKey());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            IaiClient client = new IaiClient(cred,
                    tencentCloudProperties.getRegion(),
                    clientProfile);
            DetectLiveFaceRequest req = new DetectLiveFaceRequest();
            req.setImage(img);
            DetectLiveFaceResponse resp = client.DetectLiveFace(req);
            if (resp.getIsLiveness()) {
                return true;
            }
        } catch (TencentCloudSDKException e) {
           log.error("e ",e);
        }
        return false;
    }
}