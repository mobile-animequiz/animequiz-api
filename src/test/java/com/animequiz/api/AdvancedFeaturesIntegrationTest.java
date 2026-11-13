package com.animequiz.api;

import com.animequiz.api.repository.ApiKeyRepository;
import com.animequiz.api.repository.UserRepository;
import com.animequiz.api.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdvancedFeaturesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired ApiKeyRepository apiKeys;
    @Autowired com.animequiz.api.repository.ScoreRepository scores;

    @Test
    void versionOneIsDefaultAndVersionTwoHasAdditionalFields() throws Exception {
        mvc.perform(get("/api/versioned/status"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value("1"));
        mvc.perform(get("/api/versioned/status").header("X-API-Version", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value("2"))
                .andExpect(jsonPath("$.features").value("advanced-api"));
        mvc.perform(get("/api/versioned/status").header("X-API-Version", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsAcceptsConfiguredOriginAndRejectsOtherOrigin() throws Exception {
        mvc.perform(options("/api/versioned/status").header("Origin", "http://localhost:19006")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:19006"));
        mvc.perform(options("/api/versioned/status").header("Origin", "https://not-allowed.example")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedAttemptWithIdempotencyKeyReturnsSameScoreAndConflictForChangedPayload() throws Exception {
        User user = users.save(new User("idempotency_demo_user", "idempotency-demo@example.test"));
        String key = "test-idempotency-key-2026";
        String first = mvc.perform(post("/quizzes/1/attempts")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(user.getId().toString())))
                .header("X-Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(first).path("scoreId").asLong();
        long afterFirst = scores.count();
        mvc.perform(post("/quizzes/1/attempts")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(user.getId().toString())))
                .header("X-Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.scoreId").value(id));
        assertThat(scores.count()).isEqualTo(afterFirst);
        mvc.perform(post("/quizzes/1/attempts")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(user.getId().toString())))
                .header("X-Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":1,\"optionId\":2}]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void apiKeyIsCreatedWithJwtAndRequiredForProtectedDemoEndpoint() throws Exception {
        User user = users.save(new User("api_key_advanced_user", "advanced-key@example.test"));
        String body = mvc.perform(post("/api-keys")
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(user.getId().toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var data = mapper.readTree(body);
        String secret = data.get("apiKey").asText();
        long id = data.get("id").asLong();
        assertThat(secret).startsWith("aq_");
        mvc.perform(get("/api-keys/verify"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api-keys/verify").header("X-API-Key", secret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(delete("/api-keys/" + id)
                .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(user.getId().toString()))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api-keys/verify").header("X-API-Key", secret))
                .andExpect(status().isUnauthorized());
    }
}
