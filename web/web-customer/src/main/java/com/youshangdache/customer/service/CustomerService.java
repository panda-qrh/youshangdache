package com.youshangdache.customer.service;

import com.youshangdache.model.form.customer.UpdateWxPhoneForm;
import com.youshangdache.model.vo.customer.CustomerLoginVo;
import com.youshangdache.model.vo.order.CurrentOrderInfoVo;

public interface CustomerService {

    /**
     * 小程序登录接口-用户端
     *
     * @param code 微信颁发的授权码
     * @return token
     */
    String login(String code);

    /**
     * 获取用户登录后的信息
     *
     * @param customerId 用户id
     * @return 用户登录后的相关数据
     */
    CustomerLoginVo getCustomerLoginInfo(Long customerId);

    /**
     * 绑定用户手机号
     * <p>登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号</p>
     *
     * @param updateWxPhoneForm
     * @return true绑定 | false未绑定
     */
    void updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);

    /**
     * 乘客如果已经下过单了，而且这个订单在执行中，没有结束，
     * 那么乘客是不可以再下单的，页面会弹出层，进入执行中的订单。
     * @param customerId 用户id
     * @return 当前用户正在进行的订单信息
     */
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);
}
