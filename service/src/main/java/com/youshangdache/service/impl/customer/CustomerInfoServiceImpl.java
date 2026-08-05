package com.youshangdache.service.impl.customer;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.youshangdache.common.config.thread.ThreadPoolConfig;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.common.util.IpUtil;
import com.youshangdache.common.util.PhoneNumberUtils;
import com.youshangdache.mapper.customer.CustomerInfoMapper;
import com.youshangdache.mapper.customer.CustomerLoginLogMapper;
import com.youshangdache.service.customer.CustomerInfoService;
import com.youshangdache.model.entity.customer.CustomerInfo;
import com.youshangdache.model.entity.customer.CustomerLoginLog;
import com.youshangdache.model.enums.LoginStatusEnum;
import com.youshangdache.model.form.customer.UpdateWxPhoneForm;
import com.youshangdache.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@EnableAsync
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {
    @Resource
    private WxMaService wxMaService;
    @Resource
    private CustomerInfoMapper customerInfoMapper;
    @Resource
    private CustomerLoginLogMapper customerLoginLogMapper;
    @Resource
    private HttpServletRequest request;


    /**
     * 获取乘客的openId
     *
     * @param customerId 乘客id
     * @return 用户的openId
     */
    @Override
    public String getCustomerOpenId(Long customerId) {
        CustomerInfo customerInfo = customerInfoMapper.selectOne(new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getId, customerId));
        return customerInfo.getWxOpenId();
    }

    /**
     * 绑定用户手机号
     * <p>登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号</p>
     *
     * @param updateWxPhoneForm
     * @return true绑定 | false未绑定
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        //根据code获取微信绑定的手机号
        try {
            WxMaPhoneNumberInfo phoneNoInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
            if (phoneNoInfo == null ||
                    StringUtils.isBlank(phoneNoInfo.getPhoneNumber()) ||
                    !PhoneNumberUtils.isValidPhoneNumber(phoneNoInfo.getPhoneNumber())) {
                throw new GuiguException(ResultCodeEnum.UNCORRECTED_PHONE_NUMBER);
            }
            String phoneNumber = phoneNoInfo.getPhoneNumber();
            //更新用户信息
            CustomerInfo customerInfo = customerInfoMapper.selectById(updateWxPhoneForm.getCustomerId());
            if (customerInfo == null) throw new GuiguException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
            customerInfo.setPhone(phoneNumber);
            customerInfoMapper.updateById(customerInfo);
        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    /**
     * 获取乘客信息
     *
     * <p>乘客登录成功，小程序会回调“获取客户登录信息”接口，获取当前乘客基本信息</p>
     *
     * @param customerId 用户id
     * @return 用户登录后的基本信息
     */
    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        //1根据用户id查询用户信息
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);
        if (customerInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //2封装到CustomerInfoVO
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        BeanUtils.copyProperties(customerInfo, customerLoginVo);
        customerLoginVo.setIsBindPhone(StringUtils.isNotBlank(customerInfo.getPhone()));
        //3返回CustomerInfoVO
        return customerLoginVo;
    }

    /**
     * 小程序登录接口-用户端
     * <p>用户第一次登录，则登录并注册，返回用户id；否则直接返回用户ID</p>
     *
     * @param code 微信颁发的授权码
     * @return 用户id
     */
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Long login(String code) {
        //1获取code值，使用微信工具包对象获取微信唯一标识openid
        String openid = null;
        String failMsg = null;
        try {
            //用户在当前小程序的会话信息，从会话信息中获取用户的openid
            openid = wxMaService.getUserService()
                    .getSessionInfo(code)
                    .getOpenid();
        } catch (WxErrorException e) {
            log.warn("微信登录失败，code: {}", code, e);
            failMsg = "微信授权失败";
        }
        if (!StringUtils.isNotBlank(openid)) {
            if (failMsg == null) {
                failMsg = "openid为空";
            }
            recordLoginLog(new CustomerLoginLog(null, IpUtil.getIpAddress(request), LoginStatusEnum.FAIL, failMsg));
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        //2根据openid查询数据库表，判断是否是第一次登录
        CustomerInfo customerInfo = customerInfoMapper.selectOne(
                new LambdaQueryWrapper<CustomerInfo>().eq(CustomerInfo::getWxOpenId, openid)
        );
        boolean isFirstLogin = customerInfo == null;
        //3第一次登录，添加到数据库
        if (isFirstLogin) {
            customerInfo = new CustomerInfo();
            customerInfo.setNickname("用户" + System.currentTimeMillis());
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.setWxOpenId(openid);
            customerInfoMapper.insert(customerInfo);
        }
        //4异步记录登录日志信息
        String msg = isFirstLogin ? "小程序首次登录" : "小程序登录";
        recordLoginLog(new CustomerLoginLog(customerInfo.getId(), IpUtil.getIpAddress(request), LoginStatusEnum.SUCCESS, msg));
        //5返回用户id
        return customerInfo.getId();
    }

    /**
     * 异步记录登录日志<br>
     * <p>
     * 线程池：{@link ThreadPoolConfig#loginLogExecutor()}
     *
     * @param loginLog 待记录的对象
     */
    @Async("loginLogExecutor")
    public void recordLoginLog(CustomerLoginLog loginLog) {
        customerLoginLogMapper.insert(loginLog);
    }
}
