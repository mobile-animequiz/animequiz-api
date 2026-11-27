package com.animequiz.api;

import com.animequiz.api.configuration.AdminBootstrapInitializer;
import com.animequiz.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("prod")
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:production-admin-test;DB_CLOSE_DELAY=-1",
    "spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.admin-bootstrap.enabled=true", "app.admin-bootstrap.email=bootstrap@example.test",
    "app.admin-bootstrap.username=bootstrap-admin",
    "app.admin-bootstrap.password=Bootstrap-test-only-passphrase9!"
})
@AutoConfigureMockMvc
class AdminBootstrapProductionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired AdminBootstrapInitializer bootstrap;

    @Test void productionAdminCanLoginAndCreateWhilePublicRegistrationCannot() throws Exception {
        var response=mvc.perform(post("/auth/login").header("X-API-Version","2")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"bootstrap@example.test\",\"password\":\"Bootstrap-test-only-passphrase9!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token=json.readTree(response).path("accessToken").asText();
        String quiz="{\"title\":\"Admin bootstrap quiz\",\"description\":\"Created by provisioned operator\",\"difficulty\":\"DIFICIL\",\"category\":\"SHONEN\"}";
        mvc.perform(post("/quizzes").header("X-API-Version","2").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(quiz)).andExpect(status().isCreated());
        var signup=mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"bootstrap-player\",\"email\":\"player.bootstrap@example.test\",\"password\":\"Player-test-only-passphrase9!\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String player=json.readTree(signup).path("accessToken").asText();
        mvc.perform(post("/quizzes").header("X-API-Version","2").header("Authorization","Bearer "+player)
            .contentType(MediaType.APPLICATION_JSON).content(quiz)).andExpect(status().isForbidden());
    }
    @Test void restartPreservesHashAndSessionsWhenConfigurationIsUnchanged() {
        var before=users.findByEmailIgnoreCase("bootstrap@example.test").orElseThrow();
        String hash=before.getPasswordHash(); int version=before.getCredentialVersion();
        bootstrap.run(null);
        var after=users.findByEmailIgnoreCase("bootstrap@example.test").orElseThrow();
        assertEquals(hash,after.getPasswordHash()); assertEquals(version,after.getCredentialVersion());
        assertTrue(passwords.matches("Bootstrap-test-only-passphrase9!",hash));
    }
}
