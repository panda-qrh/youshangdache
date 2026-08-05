package com.youshangdache.controller.customer;

import com.youshangdache.service.customer.CustomerInfoService;
import com.youshangdache.model.form.customer.UpdateWxPhoneForm;
import com.youshangdache.model.vo.customer.CustomerLoginVo;
import com.youshangdache.service.customer.CustomerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customer/info")
public class CustomerInfoController {

	@Autowired
	private CustomerInfoService customerInfoService;
	/**
	 * 小程序登录接口-用户端
	 * <p>用户第一次登录，则登录并注册，返回用户id；否则直接返回用户ID</p>
	 *
	 * @param code 微信颁发的授权码
	 * @return 用户id
	 */
	@Operation(summary = "用户端微信小程序登录接口")
	@GetMapping("/login/{code}")
	public Long login(@PathVariable String code){
		return customerInfoService.login(code);
	}

	/**
	 * 获取用户的登录信息
	 *
	 * @param customerId 用户id
	 * @return 用户登录后的基本信息
	 */
	@Operation(summary = "获取客户基本信息")
	@GetMapping("/getCustomerLoginInfo/{customerId}")
	public CustomerLoginVo getCustomerInfo(@PathVariable Long customerId) {
		return customerInfoService.getCustomerInfo(customerId);
	}
	/**
	 * 绑定用户手机号
	 * <p>登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号</p>
	 *
	 * @param updateWxPhoneForm
	 * @return true绑定 | false未绑定
	 */
	@Operation(summary = "更新客户微信手机号码")
	@GetMapping("/updateWxPhoneNumber")
	public void updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm) {
		 customerInfoService.updateWxPhoneNumber(updateWxPhoneForm);
	}
	@Operation(summary = "获取客户的openId")
	@GetMapping("/getCustomerOpenId/{customerId}")
	public String getCustomerOpenId(@PathVariable Long customerId) {
		return customerInfoService.getCustomerOpenId(customerId);
	}

}

