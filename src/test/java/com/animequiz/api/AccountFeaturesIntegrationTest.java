package com.animequiz.api;

import com.animequiz.api.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"app.public-origin=https://quiz.example.test","api.auth-limit.enabled=false","app.catalog.seed-enabled=false"})
@AutoConfigureMockMvc
class AccountFeaturesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean PasswordMailer mailer;
    @MockBean GoogleIdentityVerifier google;
    @BeforeEach void mailConfiguration() { when(mailer.isConfigured()).thenReturn(true); }
    private record Session(String email,String token,long id) {}
    private Session register() throws Exception {
        String suffix=UUID.randomUUID().toString().replace("-","");
        String email=suffix+"@example.test";
        String body=json.writeValueAsString(Map.of("username","jogador"+suffix,"email",email,"password","ExamplePass9!"));
        JsonNode result=json.readTree(mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return new Session(email,result.path("accessToken").asText(),result.path("user").path("id").asLong());
    }
    @Test void ownSettingsAndFavoritesPersistAcrossNewSessionsWithoutLeakingOtherAccounts() throws Exception {
        Session a=register(),b=register();
        mvc.perform(patch("/users/me/settings").header("Authorization","Bearer "+a.token()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Isaac\",\"theme\":\"eclipse\",\"accent\":\"cyan\",\"locale\":\"en\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Isaac"));
        for(int i=0;i<2;i++) mvc.perform(put("/users/me/favorites/1").header("Authorization","Bearer "+a.token())).andExpect(status().isNoContent());
        mvc.perform(get("/users/me/favorites").header("Authorization","Bearer "+a.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/users/me/favorites").header("Authorization","Bearer "+b.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/users/me/settings").header("Authorization","Bearer "+b.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("midnight"));
        String login=json.writeValueAsString(Map.of("email",a.email(),"password","ExamplePass9!"));
        String token=json.readTree(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("accessToken").asText();
        mvc.perform(get("/users/me/settings").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Isaac")).andExpect(jsonPath("$.locale").value("en"));
        for(int i=0;i<2;i++) mvc.perform(delete("/users/me/favorites/1").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        mvc.perform(get("/users/me/favorites").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/users/me/settings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/me/favorites?page=-1").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
    }
    @Test void avatarRequiresAuthenticationValidatesContentAndCanBeRemoved() throws Exception {
        Session a=register();
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",output);
        byte[] bytes=output.toByteArray();
        var upload=MockMvcRequestBuilders.multipart("/users/me/avatar")
            .file(new MockMultipartFile("file","photo.png","image/png",bytes))
            .header("Authorization","Bearer "+a.token()).with(request->{request.setMethod("PUT");return request;});
        mvc.perform(upload).andExpect(status().isOk()).andExpect(jsonPath("$.hasAvatar").value(true));
        mvc.perform(get("/users/me/avatar").header("Authorization","Bearer "+a.token()))
            .andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(bytes));
        mvc.perform(get("/users/me/avatar")).andExpect(status().isUnauthorized());
        mvc.perform(MockMvcRequestBuilders.multipart("/users/me/avatar")
            .file(new MockMultipartFile("file","fake.png","image/png","<svg/>".getBytes()))
            .header("Authorization","Bearer "+a.token()).with(request->{request.setMethod("PUT");return request;}))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/users/me/avatar").header("Authorization","Bearer "+a.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hasAvatar").value(false));
        mvc.perform(get("/users/me/avatar").header("Authorization","Bearer "+a.token())).andExpect(status().isNotFound());
    }
    @Test void passwordResetIsGenericSingleUseAndRevokesOldSessions() throws Exception {
        Session a=register();
        String known=mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",a.email())))).andExpect(status().isAccepted())
            .andReturn().getResponse().getContentAsString();
        String unknown=mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"absent@example.test\"}")).andExpect(status().isAccepted())
            .andReturn().getResponse().getContentAsString();
        assertEquals(known,unknown);
        var link=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailer,timeout(4000)).send(eq(a.email()),link.capture());
        String token=link.getValue().substring(link.getValue().indexOf("#token=")+7);
        String reset=json.writeValueAsString(Map.of("token",token,"password","ChangedPass9!"));
        mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(reset)).andExpect(status().isNoContent());
        mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(reset)).andExpect(status().isBadRequest());
        mvc.perform(get("/users/me").header("Authorization","Bearer "+a.token())).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",a.email(),"password","ExamplePass9!")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",a.email(),"password","ChangedPass9!")))).andExpect(status().isOk());
    }
    @Test void googleDoesNotTakeOverAnExistingPasswordAccount() throws Exception {
        Session a=register();
        when(google.verify("verified-token")).thenReturn(new GoogleIdentityVerifier.Identity("google-sub-"+a.id(),a.email()));
        mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"verified-token\"}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/auth/google/link").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"verified-token\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/google/link").header("Authorization","Bearer "+a.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"verified-token\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"verified-token\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(a.id()));
        mvc.perform(get("/users/me/settings").header("Authorization","Bearer "+a.token()))
            .andExpect(jsonPath("$.googleLinked").value(true));
    }
    @Test void invalidSettingsNeverChangeTheSavedName() throws Exception {
        Session a=register();
        mvc.perform(patch("/users/me/settings").header("Authorization","Bearer "+a.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"  \",\"accent\":\"bad\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/users/me/settings").header("Authorization","Bearer "+a.token())).andExpect(jsonPath("$.accent").value("violet"));
    }
}

