package com.animequiz.api.configuration;

import com.animequiz.api.dto.APIError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Negotiation shared by the real API resources, not only the status example. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiVersionFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;

    public ApiVersionFilter(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api-docs") || path.startsWith("/swagger-ui")
                || path.equals("/") || path.equals("/docs") || path.startsWith("/docs/")
                || path.equals("/docs.html") || path.equals("/docs-v2.html") || path.equals("/docs-version-switch.js")
                || path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader("X-API-Version");
        String version = supplied == null ? "1" : supplied;
        response.addHeader("Vary", "X-API-Version");
        if (!version.equals("1") && !version.equals("2")) {
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new APIError(400, "Bad Request",
                    "X-API-Version deve ser 1 ou 2", LocalDateTime.now()));
            return;
        }
        response.setHeader("X-API-Version", version);
        chain.doFilter(request, response);
    }
}
