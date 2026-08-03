package com.youshangdache.driver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author QRH
 * @date 2024/8/21 20:28
 * @description TODO
 */
@Data
@ConfigurationProperties(prefix = "minio")
@Configuration
public class MinioProperties {
    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
