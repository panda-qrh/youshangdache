package com.youshangdache.payment;

import com.youshangdache.model.form.payment.PaymentInfoForm;
import com.youshangdache.model.vo.payment.WxPrepayVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(value = "service")
public interface WxPayFeignClient {

    @PostMapping("/payment/wxPay/createWxPayment")
    WxPrepayVo createWxPayment(@RequestBody PaymentInfoForm paymentInfoForm);

    @GetMapping("/payment/wxPay/queryPayStatus/{orderNo}")
    Boolean queryPayStatus(@PathVariable String orderNo);
}