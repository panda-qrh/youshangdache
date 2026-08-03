package com.qrh.youshangdache.payment.controller;

import com.qrh.youshangdache.model.form.payment.PaymentInfoForm;
import com.qrh.youshangdache.model.vo.payment.WxPrepayVo;
import com.qrh.youshangdache.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Tag(name = "微信支付接口")
@RestController
@RequestMapping("payment/wxPay")
@Slf4j
public class WxPayController {
    @Resource
    private WxPayService wxPayService;

    @Operation(summary = "创建微信支付")
    @PostMapping("/createJsapi")
    public WxPrepayVo createWxPayment(@RequestBody PaymentInfoForm paymentInfoForm) {
        return wxPayService.createWxPayment(paymentInfoForm);
    }
    @Operation(summary = "支付状态查询")
    @GetMapping("/queryPayStatus/{orderNo}")
    public Boolean queryPayStatus(@PathVariable String orderNo) {
        return wxPayService.queryPayStatus(orderNo);
    }

    @Operation(summary = "微信支付异步通知接口")
    @PostMapping("/notify")
    public Map<String,Object> notify(HttpServletRequest request) {
        try {
            wxPayService.wxnotify(request);

            //返回成功
            Map<String,Object> result = new HashMap<>();
            result.put("code", "SUCCESS");
            result.put("message", "成功");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //返回失败
        Map<String,Object> result = new HashMap<>();
        result.put("code", "FAIL");
        result.put("message", "失败");
        return result;
    }
}
