package com.youshangdache.config.driver;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author QRH
 * @date 2024/7/21 23:15
 * @description
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tencent.cloud")
public class TencentCloudProperties {
    private String secretId;
    private String secretKey;
    private String region;
    private String bucketPrivate;
    private String persionGroupId;
}
