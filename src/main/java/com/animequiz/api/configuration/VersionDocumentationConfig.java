package com.animequiz.api.configuration;

import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VersionDocumentationConfig {
    @Bean
    GroupedOpenApi versionOne(OpenApiConfig config) { return group("1", config); }

    @Bean
    GroupedOpenApi versionTwo(OpenApiConfig config) { return group("2", config); }

    private GroupedOpenApi group(String version, OpenApiConfig config) {
        return GroupedOpenApi.builder().group("v" + version).pathsToMatch("/**")
                .addOpenApiMethodFilter(method -> version.equals("1")
                        ? !method.getName().equals("getQuizQuestionsV2")
                        : !method.getName().equals("getQuizQuestions"))
                .addOpenApiCustomizer(config.globalResponsesCustomizer())
                .addOpenApiCustomizer(api -> {
                    api.getInfo().setVersion(version + ".0.0");
                    api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                        var parameters = operation.getParameters();
                        if (parameters != null) parameters.removeIf(parameter -> "X-API-Version".equals(parameter.getName()));
                        operation.addParametersItem(new Parameter().name("X-API-Version").in("header")
                                .description("Versão selecionada. Sem cabeçalho, a API mantém a V1.")
                                .schema(new StringSchema()._default(version)._enum(List.of(version))));
                    }));
                }).build();
    }
}
