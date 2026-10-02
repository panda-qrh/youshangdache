package com.youshangdache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
//登录日志使用 @Async("loginLogExecutor") 异步落库，必须开启异步支持，否则该注解不生效
@EnableAsync
public class ServiceDriverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceDriverApplication.class, args);
    }

}
