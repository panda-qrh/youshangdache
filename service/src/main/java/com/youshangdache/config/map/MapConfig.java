package com.youshangdache.config.map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author QRH
 * @date 2024/7/24 19:51
 * @description
 */
@Configuration
public class MapConfig {
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
