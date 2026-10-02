package com.youshangdache.payment.config;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author QRH
 * @date 2024/8/24 22:31
 * @description 微信支付 API v3 配置（商户号、私钥、证书序列号、APIv3 密钥）
 */

@Slf4j
@Configuration
@ConfigurationProperties(prefix="wx.v3pay") //读取节点
@Data
public class WxPayV3Properties {

    /**
     * 私钥放在 classpath 时使用的前缀，例如 classpath:apiclient_key.pem
     */
    private static final String CLASSPATH_PREFIX = "classpath:";

    private String appid;
    /** 商户号 */
    private String merchantId;
    /** 商户API私钥路径（支持 classpath: 前缀，指向工程内的证书文件） */
    private String privateKeyPath;
    /** 商户证书序列号 */
    private String merchantSerialNumber;
    /** 商户APIV3密钥 */
    private String apiV3key;
    /** 回调地址 */
    private String notifyUrl;

    @Bean
    public RSAAutoCertificateConfig getConfig(){
        RSAAutoCertificateConfig.Builder builder = new RSAAutoCertificateConfig.Builder()
                .merchantId(this.getMerchantId())
                .merchantSerialNumber(this.getMerchantSerialNumber())
                .apiV3Key(this.getApiV3key());

        String path = this.getPrivateKeyPath();
        if (StringUtils.hasText(path) && path.startsWith(CLASSPATH_PREFIX)) {
            //从 classpath 读取商户私钥：避免把证书路径写死成某台机器上的绝对路径，
            //否则换台机器（或打成 jar 部署）时 privateKeyFromPath 会直接抛异常，服务起不来。
            String location = path.substring(CLASSPATH_PREFIX.length());
            builder.privateKey(readPrivateKeyFromClasspath(location));
        } else {
            builder.privateKeyFromPath(path);
        }
        return builder.build();
    }

    /**
     * 读取 classpath 下的商户私钥内容
     *
     * @param location classpath 路径，例如 apiclient_key.pem
     * @return 私钥文件内容
     */
    private String readPrivateKeyFromClasspath(String location) {
        ClassPathResource resource = new ClassPathResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("微信支付商户私钥文件不存在：" + location);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            log.info("已从 classpath 加载微信支付商户私钥：{}", location);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取微信支付商户私钥失败：" + location, e);
        }
    }
}
