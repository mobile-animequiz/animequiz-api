package com.animequiz.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"api.rate-limit.requests=2","api.rate-limit.window-seconds=3600"})
@AutoConfigureMockMvc
@Transactional
class AcademicVersionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private static final AtomicInteger IPS=new AtomicInteger();
    private RequestPostProcessor ip(){String value="192.0.2."+IPS.incrementAndGet();return request->{request.setRemoteAddr(value);return request;};}
    private RequestPostProcessor player(){return jwt().jwt(token->token.subject("1").claim("roles",List.of("USER")))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));}

    @Test void v1QueriesArePublicIgnoreBearerAndDoNotConsumeAnyRateLimit() throws Exception {
        var address=ip();
        for(int i=0;i<8;i++) mvc.perform(get("/quizzes").header("X-API-Version","1")
            .header("Authorization","Bearer deliberately-invalid").with(address))
            .andExpect(status().isOk()).andExpect(header().doesNotExist("X-RateLimit-Limit"))
            .andExpect(header().doesNotExist("X-AuthRateLimit-Limit"));
    }

    @Test void v2CatalogNeedsJwtAndAppliesTheImplementedLimit() throws Exception {
        mvc.perform(get("/quizzes").header("X-API-Version","2").with(ip())).andExpect(status().isUnauthorized());
        var address=ip();
        for(int i=0;i<3;i++){
            var result=mvc.perform(get("/quizzes").header("X-API-Version","2").with(player()).with(address));
            if(i<2)result.andExpect(status().isOk()).andExpect(header().string("X-RateLimit-Limit","2"));
            else result.andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        }
    }

    @Test void normalUsersCannotCreateOrManageContentEvenByDowngradingToV1() throws Exception {
        for(String version:List.of("1","2")){
            for(String path:List.of("/users","/profiles","/quizzes","/questions","/options","/scores","/api-keys"))
                mvc.perform(post(path).header("X-API-Version",version).with(player()).with(ip())
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
            for(String path:List.of("/users/2","/profiles/1","/quizzes/1","/questions/1","/options/1","/scores/1")){
                mvc.perform(put(path).header("X-API-Version",version).with(player()).with(ip())
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
                mvc.perform(delete(path).header("X-API-Version",version).with(player()).with(ip()))
                    .andExpect(status().isForbidden());
            }
        }
    }

    @Test void v1HeaderCannotOpenPrivateAccountsOrAdministrativeWrites() throws Exception {
        for(String path:List.of("/users/me","/users/me/settings","/users/1/profile"))
            mvc.perform(get(path).header("X-API-Version","1").with(ip())).andExpect(status().isUnauthorized());
        mvc.perform(post("/quizzes").header("X-API-Version","1").with(ip())
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void playerMaySubmitAnswersReadOwnResultAndEditOnlyOwnSettings() throws Exception {
        String result=mvc.perform(post("/quizzes/1/attempts").header("X-API-Version","2").with(player()).with(ip())
            .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(1))
            .andReturn().getResponse().getContentAsString();
        long id=json.readTree(result).path("scoreId").asLong();
        mvc.perform(get("/scores/"+id).header("X-API-Version","2").with(player()).with(ip())).andExpect(status().isOk());
        mvc.perform(get("/scores/"+id).header("X-API-Version","2")
            .with(jwt().jwt(token->token.subject("2").claim("roles",List.of("USER")))).with(ip())).andExpect(status().isForbidden());
        mvc.perform(get("/scores").header("X-API-Version","2").with(player()).with(ip())).andExpect(status().isForbidden());
        mvc.perform(patch("/users/me/settings").header("X-API-Version","2").with(player()).with(ip())
            .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Jogador\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Jogador"));
    }

    @Test void responseLinksDoNotOfferAdministrativeActionsToPlayers() throws Exception {
        for(String version:List.of("1","2")){
            String result=mvc.perform(get("/quizzes/1").header("X-API-Version",version).with(player()).with(ip()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(json.readTree(result).path("_links").has("update")).isFalse();
            assertThat(json.readTree(result).path("_links").has("delete")).isFalse();
        }
    }
}
