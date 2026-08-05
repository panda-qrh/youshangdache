package com.youshangdache.service.payment;

import com.youshangdache.model.form.payment.PaymentInfoForm;
import com.youshangdache.model.vo.payment.WxPrepayVo;
import jakarta.servlet.http.HttpServletRequest;

public interface WxPayService {


    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);

    void wxnotify(HttpServletRequest request);

    Boolean queryPayStatus(String orderNo);

    void handlerOrder(String orderNo);
}
