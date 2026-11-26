package com.animequiz.api.configuration;

import org.springframework.core.MethodParameter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Keep the advertised HAL representation consistent for clients accepting plain JSON. */
@RestControllerAdvice(basePackages = "com.animequiz.api.controller")
public class HalResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converter) { return true; }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof RepresentationModel<?> model) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            boolean admin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean basic = !"2".equals(request.getHeaders().getFirst("X-API-Version"));
            if (basic || !admin) removeAdministrativeLinks(model);
            response.getHeaders().setContentType(MediaTypes.HAL_JSON);
        }
        return body;
    }

    private static void removeAdministrativeLinks(RepresentationModel<?> model) {
        Set<String> hidden = Set.of("create", "update", "delete", "all-users", "all-profiles", "all-scores", "profile");
        var visible = model.getLinks().stream().filter(link -> !hidden.contains(link.getRel().value())).toList();
        model.removeLinks();
        model.add(visible);
        if (model instanceof CollectionModel<?> collection) {
            for (Object item : collection.getContent()) if (item instanceof RepresentationModel<?> nested) removeAdministrativeLinks(nested);
        }
    }
}
