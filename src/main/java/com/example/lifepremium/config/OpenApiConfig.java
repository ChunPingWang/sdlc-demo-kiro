package com.example.lifepremium.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lifePremiumOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("壽險保費試算 API")
                .version("1.0.0")
                .description("Life Insurance Premium Calculation Service — SD-LIFE-v1.0")
            );
    }
}
