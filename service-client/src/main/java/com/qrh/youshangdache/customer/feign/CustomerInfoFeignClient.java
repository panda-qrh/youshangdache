package com.qrh.youshangdache.customer.feign;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.form.customer.UpdateWxPhoneForm;
import com.qrh.youshangdache.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-customer")
public interface CustomerInfoFeignClient {
    /**
     * 小程序登录接口-用户端
     * <p>用户第一次登录，则登录并注册，返回用户id；否则直接返回用户ID</p>
     *
     * @param code 微信颁发的授权码
     * @return 用户id
     */
    @GetMapping("/customer/info/login/{code}")
    public Result<Long> login(@PathVariable String code);

    /**
     * 获取用户的登录信息
     *
     * @param customerId 用户id
     * @return 用户登录后的基本信息
     */
    @GetMapping("/customer/info/getCustomerLoginInfo/{customerId}")
    public Result<CustomerLoginVo> getCustomerInfo(@PathVariable Long customerId);

    /**
     * 绑定用户手机号
     * <p>登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号</p>
     *
     * @param updateWxPhoneForm
     * @return true绑定 | false未绑定
     */
    @GetMapping("/customer/info/updateWxPhoneNumber")
    public Result<Void> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm);

    @GetMapping("/customer/info/getCustomerOpenId/{customerId}")
    public Result<String> getCustomerOpenId(@PathVariable Long customerId);
}