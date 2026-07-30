package com.qrh.youshangdache.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author QRH
 * @date 2024/7/17 19:47
 * @description 微信小程序属性相关配置类
 */
@Configuration
@ConfigurationProperties(prefix = "wx.miniapp")
@Data
public class WxConfigurationProperties {
    /** 小程序微信公众平台appId */
    private String appId;
    /** 小程序微信公众平台api秘钥 */
    private String secret;
}
