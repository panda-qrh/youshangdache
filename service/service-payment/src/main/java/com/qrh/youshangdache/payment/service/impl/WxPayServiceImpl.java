package com.qrh.youshangdache.payment.service.impl;

import com.alibaba.fastjson.JSON;

import com.qrh.youshangdache.common.mq.constant.ExchangeConst;
import com.qrh.youshangdache.common.mq.constant.RoutingConst;
import com.qrh.youshangdache.common.constant.SystemConstant;
import com.qrh.youshangdache.common.execption.GuiguException;
import com.qrh.youshangdache.common.result.ResultCodeEnum;
import com.qrh.youshangdache.common.mq.service.RabbitService;
import com.qrh.youshangdache.common.util.RequestUtils;
import com.qrh.youshangdache.driver.client.DriverAccountFeignClient;
import com.qrh.youshangdache.model.entity.payment.PaymentInfo;
import com.qrh.youshangdache.model.enums.TradeTypeEnum;
import com.qrh.youshangdache.model.form.driver.TransferForm;
import com.qrh.youshangdache.model.form.payment.PaymentInfoForm;
import com.qrh.youshangdache.model.form.payment.ProfitsharingForm;
import com.qrh.youshangdache.model.vo.order.OrderProfitsharingVo;
import com.qrh.youshangdache.model.vo.order.OrderRewardVo;
import com.qrh.youshangdache.model.vo.payment.WxPrepayVo;
import com.qrh.youshangdache.order.client.OrderInfoFeignClient;
import com.qrh.youshangdache.payment.config.WxPayV3Properties;
import com.qrh.youshangdache.payment.mapper.PaymentInfoMapper;
import com.qrh.youshangdache.payment.service.WxPayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.partnerpayments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.*;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    @Resource
    private PaymentInfoMapper paymentInfoMapper;
    @Resource
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;
    @Resource
    private WxPayV3Properties wxPayV3Properties;
    @Resource
    private RabbitService rabbitService;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private DriverAccountFeignClient driverAccountFeignClient;

    @Override
    @GlobalTransactional
    public void handlerOrder(String orderNo) {
        orderInfoFeignClient.updateOrderPayStatus(orderNo);

        //处理系统奖励，打入司机账户
        OrderRewardVo orderRewardVo = orderInfoFeignClient.getOrderRewardFee(orderNo).getData();
        if(null != orderRewardVo.getRewardFee() && orderRewardVo.getRewardFee().doubleValue() > 0) {
            TransferForm transferForm = new TransferForm();
            transferForm.setTradeNo(orderNo);
            transferForm.setTradeType(TradeTypeEnum.REWARD.getCode());
            transferForm.setContent(TradeTypeEnum.REWARD.getMessage());
            transferForm.setAmount(orderRewardVo.getRewardFee());
            transferForm.setDriverId(orderRewardVo.getDriverId());
            driverAccountFeignClient.transfer(transferForm);
        }

        //分账处理
        OrderProfitsharingVo orderProfitsharingVo = orderInfoFeignClient.getOrderProfitsharing(orderRewardVo.getOrderId()).getData();
        //封装分账参数对象
        ProfitsharingForm profitsharingForm = new ProfitsharingForm();
        profitsharingForm.setOrderNo(orderNo);
        profitsharingForm.setAmount(orderProfitsharingVo.getDriverIncome());
        profitsharingForm.setDriverId(orderRewardVo.getDriverId());
        //分账有延迟，支付成功后最少2分钟执行分账申请
        rabbitService.sendDelayMessage(ExchangeConst.PROFITSHARING, RoutingConst.PROFITSHARING, JSON.toJSONString(profitsharingForm), SystemConstant.PROFITSHARING_DELAY_TIME);

    }

    @Override
    public Boolean queryPayStatus(String orderNo) {
        //1创建微信对象
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
        //2封装查询支付状态需要参数
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        queryRequest.setSpMchid(wxPayV3Properties.getMerchantId());
        queryRequest.setOutTradeNo(orderNo);
        //3调用微信操作对象里面方法实现查询操作
        Transaction transaction = service.queryOrderByOutTradeNo(queryRequest);
        if (transaction != null && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            this.handlePayment(transaction);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public void wxnotify(HttpServletRequest request) {
        //1.回调通知的验签与解密
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String signature = request.getHeader("Wechatpay-Signature");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String requestBody = RequestUtils.readData(request);

        //2.构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();

        //3.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
        //4.以支付通知回调为例，验签、解密并转换成 Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        if (null != transaction && transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            //5.处理支付业务
            this.handlePayment(transaction);
        }
    }

    public void handlePayment(Transaction transaction) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, transaction.getOutTradeNo()));
        if (paymentInfo.getPaymentStatus() == 1) {
            return;
        }

        //更新支付信息
        paymentInfo.setPaymentStatus(1);
        paymentInfo.setOrderNo(transaction.getOutTradeNo());
        paymentInfo.setTransactionId(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(transaction));
        paymentInfoMapper.updateById(paymentInfo);
        // 表示交易成功！

        // 后续更新订单状态！ 使用消息队列！
        rabbitService.sendMessage(ExchangeConst.ORDER, RoutingConst.PAY_SUCCESS, paymentInfo.getOrderNo());
    }

    @Override
    public WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm) {
        try {
            PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, paymentInfoForm.getOrderNo()));
            if (null == paymentInfo) {
                paymentInfo = new PaymentInfo();
                BeanUtils.copyProperties(paymentInfoForm, paymentInfo);
                paymentInfo.setPaymentStatus(0);
                paymentInfoMapper.insert(paymentInfo);
            }

            // 构建service
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();

            // request.setXxx(val)设置所需参数，具体参数可见Request定义
            PrepayRequest request = new PrepayRequest();
            Amount amount = new Amount();
//            amount.setTotal(paymentInfoForm.getAmount().multiply(new BigDecimal(100)).intValue());
            amount.setTotal(1); //1分钱
            request.setAmount(amount);
            request.setSpAppid(wxPayV3Properties.getAppid());
            request.setSpMchid(wxPayV3Properties.getMerchantId());
            //string[1,127]
            String description = paymentInfo.getContent();
            if (description.length() > 127) {
                description = description.substring(0, 127);
            }
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Properties.getNotifyUrl());
            request.setOutTradeNo(paymentInfo.getOrderNo());

            //获取用户信息
            Payer payer = new Payer();
            payer.setSpOpenid(paymentInfoForm.getCustomerOpenId());
            request.setPayer(payer);

            //是否指定分账，不指定不能分账
            SettleInfo settleInfo = new SettleInfo();
            settleInfo.setProfitSharing(true);
            request.setSettleInfo(settleInfo);

            // 调用下单方法，得到应答
            // response包含了调起支付所需的所有参数，可直接用于前端调起支付
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request, request.getSpAppid());
            WxPrepayVo wxPrepayVo = new WxPrepayVo();
            BeanUtils.copyProperties(response, wxPrepayVo);
            wxPrepayVo.setTimeStamp(response.getTimeStamp());
            return wxPrepayVo;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GuiguException(ResultCodeEnum.WX_CREATE_ERROR);
        }
    }
}
