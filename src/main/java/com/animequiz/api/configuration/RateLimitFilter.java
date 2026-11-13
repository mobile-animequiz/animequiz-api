package com.animequiz.api.configuration;

import com.animequiz.api.dto.APIError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limite simples por IP e janela fixa; para multiplas instancias usar Redis ou gateway. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Bucket(long window, int requests) { }
    private final ConcurrentHashMap<String, Bucket> usage = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final int limit;
    private final int windowSeconds;

    public RateLimitFilter(ObjectMapper mapper,
                           @Value("${api.rate-limit.requests:120}") int limit,
                           @Value("${api.rate-limit.window-seconds:60}") int windowSeconds) {
        this.mapper = mapper;
        this.limit = Math.max(1, limit);
        this.windowSeconds = Math.max(1, windowSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || request.getRequestURI().startsWith("/swagger-ui")
                || request.getRequestURI().startsWith("/api-docs")
                || request.getRequestURI().equals("/docs.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        long window = now / windowSeconds;
        // Endereco remoto validado pelo servidor; nao confiar em X-Forwarded-For fornecido pelo cliente.
        String ip = request.getRemoteAddr();
        Bucket bucket = usage.compute(ip, (key, previous) -> previous == null || previous.window != window
                ? new Bucket(window, 1) : new Bucket(window, previous.requests + 1));
        if (usage.size() > 10000) usage.entrySet().removeIf(entry -> entry.getValue().window < window);
        long retryAfter = Math.max(1, (window + 1) * windowSeconds - now);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - bucket.requests)));
        response.setHeader("X-RateLimit-Reset", String.valueOf((window + 1) * windowSeconds));
        if (bucket.requests > limit) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new APIError(429, "Too Many Requests",
                    "Limite de requisicoes excedido; tente novamente mais tarde", LocalDateTime.now()));
            return;
        }
        chain.doFilter(request, response);
    }
}
