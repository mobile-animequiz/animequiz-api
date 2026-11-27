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
                .addOpenApiMethodFilter(method -> {
                    boolean v2 = method.getDeclaringClass() == com.animequiz.api.controller.QuizV2Controller.class
                        || method.getName().equals("getQuizQuestionsV2");
                    if (version.equals("1")) return !v2;
                    boolean originalGet = method.getDeclaringClass() == com.animequiz.api.controller.QuizController.class
                        && method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class);
                    return !originalGet && !method.getName().equals("getQuizQuestions");
                })
                .addOpenApiCustomizer(config.globalResponsesCustomizer())
                .addOpenApiCustomizer(api -> AcademicOpenApiViews.apply(api, version)).build();
    }
}
