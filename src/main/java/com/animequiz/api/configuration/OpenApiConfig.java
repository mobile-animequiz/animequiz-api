package com.animequiz.api.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AnimeQuiz RESTful API Specification",
                version = "1.0.0",
                description = "API RESTful desenvolvida em Spring Boot para a gestão completa de quizzes, questões, alternativas, perfis de utilizador e pontuações.",
                contact = @Contact(
                        name = "Equipa de Desenvolvimento AnimeQuiz",
                        email = "suporte@animequiz.api"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor Local de Desenvolvimento (H2 Database)")
        },
        tags = {
                @Tag(name = "Usuários", description = "Gestão de utilizadores e registo"),
                @Tag(name = "Perfis de Utilizador", description = "Gestão de perfis e avatares"),
                @Tag(name = "Quizzes", description = "Gestão de questionários temáticos"),
                @Tag(name = "Questões", description = "Gestão de perguntas dos quizzes"),
                @Tag(name = "Opções", description = "Alternativas de resposta"),
                @Tag(name = "Scores e Rankings", description = "Registo de pontuações e classificações")
        }
)
public class OpenApiConfig {
};