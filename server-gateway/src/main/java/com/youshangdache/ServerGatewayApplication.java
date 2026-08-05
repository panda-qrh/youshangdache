package com.youshangdache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ServerGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerGatewayApplication.class, args);
	}

}
