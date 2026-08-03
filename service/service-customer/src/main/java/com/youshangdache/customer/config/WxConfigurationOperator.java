package com.youshangdache.customer.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author QRH
 * @date 2024/7/17 19:49
 * @description 微信小程序配置操作类
 */
@Component
public class WxConfigurationOperator {

    @Resource
    private WxConfigurationProperties wxConfigurationProperties;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl wxMaDefaultConfig = new WxMaDefaultConfigImpl();
        wxMaDefaultConfig.setAppid(wxConfigurationProperties.getAppId());
        wxMaDefaultConfig.setSecret(wxConfigurationProperties.getSecret());

        WxMaServiceImpl service = new WxMaServiceImpl();
        service.setWxMaConfig(wxMaDefaultConfig);

        return service;
    }
}
