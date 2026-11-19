package com.animequiz.api.configuration;
import com.animequiz.api.dto.APIError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component @Order(100)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    public record Policy(int limit,int seconds) {}
    private record Bucket(long window,int used,long expires) {}
    public static final Map<String,Policy> POLICIES=Map.of(
        "/auth/login",new Policy(20,60),"/auth/register",new Policy(10,900),
        "/auth/google",new Policy(20,60),"/auth/google/link",new Policy(10,600),
        "/auth/forgot-password",new Policy(5,900),"/auth/reset-password",new Policy(10,900));
    private final ConcurrentHashMap<String,Bucket> usage=new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final boolean enabled;
    public AuthRateLimitFilter(ObjectMapper mapper,@Value("${api.auth-limit.enabled:true}") boolean enabled) {
        this.mapper=mapper; this.enabled=enabled;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !POLICIES.containsKey(request.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
        throws IOException,ServletException {
        Policy policy=POLICIES.get(request.getRequestURI());
        long now=Instant.now().getEpochSecond(),window=now/policy.seconds(),reset=(window+1)*policy.seconds();
        String key=request.getRequestURI()+"|"+request.getRemoteAddr();
        if(usage.size()>=10000) usage.entrySet().removeIf(entry -> entry.getValue().expires()<=now);
        boolean full=usage.size()>=10000 && !usage.containsKey(key);
        Bucket bucket=full ? new Bucket(window,policy.limit()+1,reset) : usage.compute(key,(ignored,old) ->
            old==null || old.window()!=window ? new Bucket(window,1,reset) : new Bucket(window,old.used()+1,reset));
        response.setHeader("X-AuthRateLimit-Limit",Integer.toString(policy.limit()));
        response.setHeader("X-AuthRateLimit-Remaining",Integer.toString(Math.max(0,policy.limit()-bucket.used())));
        response.setHeader("X-AuthRateLimit-Reset",Long.toString(reset));
        if(bucket.used()>policy.limit()) {
            response.setStatus(429); response.setHeader("Retry-After",Long.toString(Math.max(1,reset-now)));
            response.setContentType("application/json");
            mapper.writeValue(response.getOutputStream(),new APIError(429,"Too Many Requests",
                "Muitas solicitações. Aguarde e tente novamente.",LocalDateTime.now()));
            return;
        }
        chain.doFilter(request,response);
    }
}

