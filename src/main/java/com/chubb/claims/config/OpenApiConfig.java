package com.chubb.claims.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI claimsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Chubb APAC Claims Management API")
                .description("Backend APIs for claim intake, review, settlement and workload dashboards")
                .version("v1"));
    }
}
