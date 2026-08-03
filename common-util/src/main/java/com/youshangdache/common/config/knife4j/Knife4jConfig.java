package com.youshangdache.common.config.knife4j;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    /**
     * 用户端api
     */
    @Bean
    public GroupedOpenApi webCustomerApi() {
        return GroupedOpenApi.builder()
                .group("web-customer-api")
                .pathsToMatch("/customer/**")
                .build();
    }

    /**
     * 司机端api
     */
    @Bean
    public GroupedOpenApi webDriverApi() {
        return GroupedOpenApi.builder()
                .group("web-driver-api")
                .pathsToMatch(
                        "/driver/**",
                        "/cos/**",
                        "/file/**",
                        "/ci/**",
                        "/ocr/**"
                )
                .build();
    }


    /***
     * @description 自定义接口信息
     */
    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("优尚打车API接口文档")
                        .version("1.0")
                        .description("优尚打车API接口文档")
                        .contact(new Contact().name("QRH"))
                );
    }


}
