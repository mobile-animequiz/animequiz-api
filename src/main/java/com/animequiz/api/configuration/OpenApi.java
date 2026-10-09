package com.animequiz.api.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
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
                        .title("⛩️ AnimeQuiz REST API")
                        .description("Documentação oficial do backend da API do **AnimeQuiz**. " +
                                "Desenvolvida com Spring Boot, arquitetura em camadas, suporte a HATEOAS (REST Level 3) " +
                                "e persistência avançada para integração direta com a aplicação mobile.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Desenvolvido por Isaac & Equipe AnimeQuiz")
                                .email("suporte@animequiz.com")
                                .url("https://github.com/mobile-animequiz/animequiz-api"))
                        .license(new License()
                                .name("Licença MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}