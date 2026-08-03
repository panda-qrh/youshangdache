package com.youshangdache.customer.service;

import com.youshangdache.model.entity.customer.CustomerInfo;
import com.youshangdache.model.form.customer.UpdateWxPhoneForm;
import com.youshangdache.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CustomerInfoService extends IService<CustomerInfo> {

    /**
     * 小程序登录接口-用户端
     * <p>用户第一次登录，则登录并注册，返回用户id；否则直接返回用户ID</p>
     *
     * @param code 微信颁发的授权码
     * @return 用户id
     */
    Long login(String code);

    /**
     * 获取用户的登录信息
     *
     * @param customerId 用户id
     * @return 用户登录后的基本信息
     */
    CustomerLoginVo getCustomerInfo(Long customerId);

    /**
     * 绑定用户手机号
     * <p>登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号</p>
     *
     * @param updateWxPhoneForm
     * @return true绑定 | false未绑定
     */
    void updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);

    String getCustomerOpenId(Long customerId);
}
