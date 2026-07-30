package com.qrh.youshangdache.customer.service.impl;

import com.qrh.youshangdache.common.constant.RedisConstant;
import com.qrh.youshangdache.common.execption.GuiguException;
import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.common.result.ResultCodeEnum;
import com.qrh.youshangdache.customer.client.CustomerInfoFeignClient;
import com.qrh.youshangdache.customer.service.CustomerService;
import com.qrh.youshangdache.model.entity.customer.CustomerInfo;
import com.qrh.youshangdache.model.form.customer.UpdateWxPhoneForm;
import com.qrh.youshangdache.model.vo.customer.CustomerInfoVo;
import com.qrh.youshangdache.model.vo.customer.CustomerLoginVo;
import com.qrh.youshangdache.model.vo.order.CurrentOrderInfoVo;
import com.qrh.youshangdache.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;

    /**
     * 乘客如果已经下过单了，而且这个订单在执行中，没有结束，
     * 那么乘客是不可以再下单的，页面会弹出层，进入执行中的订单。
     * @param customerId 用户id
     * @return 当前用户正在进行的订单信息
     */
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        return orderInfoFeignClient.searchCustomerCurrentOrder(customerId).getData();
    }

    /**
     * 绑定用户手机号
     * <p>登录后检查该用户是否绑定手机号，没有绑定，则提示并要求用户绑定手机号</p>
     *
     * @param updateWxPhoneForm
     * @return true绑定 | false未绑定
     */
    @Override
    public void updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm).getData();
    }

    /**
     * 获取用户的登录信息
     *
     * @param customerId 用户id
     * @return 用户登录后的相关数据
     */
    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
        return customerInfoFeignClient.getCustomerInfo(customerId).getData();
    }

    /**
     * 小程序登录接口-用户端
     *
     * <p>服务端通过随机生成的UUID作为用户的token，并以"redis前缀+token"作为redis key存储在redis中</p>
     *
     * @param code 微信颁发的授权码
     * @return token
     */
    @Override
    public String login(String code) {
        //5生成token字符串
        String token = UUID.randomUUID().toString().replace("-", "");
        //6把用户id方法放到redis，并设置过期时间
        stringRedisTemplate.opsForValue()
                .set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                        customerInfoFeignClient.login(code).getData().toString(),
                        RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                        TimeUnit.SECONDS);
        //返回token
        return token;
    }
}
