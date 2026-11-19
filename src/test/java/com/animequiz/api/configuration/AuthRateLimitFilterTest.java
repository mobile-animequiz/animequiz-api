package com.animequiz.api.configuration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static org.junit.jupiter.api.Assertions.*;
class AuthRateLimitFilterTest {
    @Test void returns429AndRetryAfterAndDoesNotTrustForwardedHeader() throws Exception {
        var filter=new AuthRateLimitFilter(new ObjectMapper().registerModule(new JavaTimeModule()),true);
        int limit=AuthRateLimitFilter.POLICIES.get("/auth/forgot-password").limit();
        for(int i=0;i<=limit;i++) {
            var request=new MockHttpServletRequest("POST","/auth/forgot-password");
            request.setRemoteAddr("192.0.2.1");request.addHeader("X-Forwarded-For","192.0.2."+i);
            var response=new MockHttpServletResponse();
            filter.doFilter(request,response,new MockFilterChain());
            assertEquals(i<limit?200:429,response.getStatus());
            if(i==limit) {assertNotNull(response.getHeader("Retry-After"));assertEquals("0",response.getHeader("X-AuthRateLimit-Remaining"));}
        }
    }
}

