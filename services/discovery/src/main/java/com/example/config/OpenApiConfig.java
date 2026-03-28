package com.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Service Discovery (Eureka)")
                .version("1.0.0")
                .description("Service Registry and Discovery Server")
                .contact(new Contact()
                    .name("Backend Team")
                    .email("backend@example.com")));
    }
}

