package com.animequiz.api.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AnimeQuiz API")
                        .version("1.0.0")
                        .description("API RESTful desenvolvida em Spring Boot para o projeto AnimeQuiz")
                        .license(new License().name("Apache 2.0").url("https://spring.io")));
    }
}