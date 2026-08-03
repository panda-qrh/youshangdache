package com.qrh.youshangdache.common.config.redis;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author QRH
 * @date 2024/8/6 23:48
 * @description TODO
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedissonConfig {
    private String host;
    private String port;
    private String password;

    private int timeout = 3000;
    private static String ADDRESS_PREFIX = "redis://";

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        if (!StringUtils.hasText(host)) {
            throw new RuntimeException("redis地址为空");
        }
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(ADDRESS_PREFIX + this.host + ":" + port)
                .setTimeout(this.timeout);
        if (StringUtils.hasText(this.password)) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
