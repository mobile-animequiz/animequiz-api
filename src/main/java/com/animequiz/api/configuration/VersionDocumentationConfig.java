package com.animequiz.api.configuration;

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
                .addOpenApiCustomizer(api -> AcademicOpenApiViews.apply(api, version)).build();
    }
}
