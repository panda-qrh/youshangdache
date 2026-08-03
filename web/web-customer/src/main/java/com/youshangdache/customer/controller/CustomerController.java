package com.youshangdache.customer.controller;

import com.youshangdache.common.annotation.Login;
import com.youshangdache.common.result.Result;
import com.youshangdache.common.util.AuthContextHolder;
import com.youshangdache.customer.service.CustomerService;
import com.youshangdache.model.form.customer.UpdateWxPhoneForm;
import com.youshangdache.model.vo.customer.CustomerLoginVo;
import com.youshangdache.model.vo.order.CurrentOrderInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
public class CustomerController {
    @Resource
    private CustomerService customerService;

    /**
     * 小程序登录接口-用户端
     *
     * @param code 微信颁发的授权码
     * @return token
     */
    @Operation(summary = "小程序授权登录")
    @GetMapping(value = "/login/{code}")
    public Result<String> wxLogin(@PathVariable String code) {
        return Result.ok(customerService.login(code));
    }

    /**
     * 获取用户登录后的信息
     *
     * @return 用户登录后的信息
     */
    @Operation(summary = "获取登录后的用户信息")
    @GetMapping("/getCustomerLoginInfo")
    @Login
    public Result<CustomerLoginVo> getCustomerInfo() {
        return Result.ok(customerService.getCustomerLoginInfo(AuthContextHolder.getUserId()));
    }

    /**
     * 绑定用户手机号
     * <p>登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号</p>
     *
     * @param updateWxPhoneForm
     * @return true绑定 | false未绑定
     */
    @Operation(summary = "登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号")
    @GetMapping("/updateWxPhone")
    @Login
    public Result<Void> updateWxPhone(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
        //用于微信公众号个人版不能获取用户的手机号，所以这里直接硬编码写死返回true
        customerService.updateWxPhoneNumber(updateWxPhoneForm);
        return Result.ok();
    }

    /**
     * 乘客如果已经下过单了，而且这个订单在执行中，没有结束，
     * 那么乘客是不可以再下单的，页面会弹出层，进入执行中的订单。
     * @return 当前用户正在进行的订单信息
     */
    @Operation(summary = "乘客端乘客查找当前订单")
    @GetMapping("/searchCustomerCurrentOrder")
    @Login
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(customerService.searchCustomerCurrentOrder(customerId));
    }
}

