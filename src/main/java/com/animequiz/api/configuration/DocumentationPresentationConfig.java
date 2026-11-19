package com.animequiz.api.configuration;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

@Configuration
public class DocumentationPresentationConfig {
    @Bean
    SwaggerIndexTransformer documentationVersionNavigation(SwaggerUiConfigProperties config,
            SwaggerUiOAuthProperties oauth, SwaggerUiConfigParameters parameters,
            SwaggerWelcomeCommon welcome, ObjectMapperProvider mapper) {
        return new SwaggerIndexPageTransformer(config, oauth, parameters, welcome, mapper) {
            @Override
            public Resource transform(HttpServletRequest request, Resource resource,
                    ResourceTransformerChain chain) throws IOException {
                Resource transformed = super.transform(request, resource, chain);
                if (!"index.html".equals(resource.getFilename())) return transformed;
                String html;
                try (var stream = transformed.getInputStream()) {
                    html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
                // Navigation only: no custom stylesheet, background or video is added to V1.
                html = html.replace("</head>", "<script src=\"/docs-version-switch.js\"></script>\n</head>");
                return new TransformedResource(transformed, html.getBytes(StandardCharsets.UTF_8));
            }
        };
    }
}
