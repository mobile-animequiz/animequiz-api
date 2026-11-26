package com.animequiz.api;

import com.animequiz.api.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiVersionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired QuestionRepository questions;

    private com.fasterxml.jackson.databind.JsonNode first(String version) throws Exception {
        String json = mvc.perform(get("/quizzes/1/questions").with(jwt().jwt(token -> token.subject("1"))).header("X-API-Version", version))
                .andExpect(status().isOk()).andExpect(header().string("X-API-Version", version))
                .andReturn().getResponse().getContentAsString();
        var embedded = mapper.readTree(json).path("_embedded");
        return embedded.elements().next().get(0);
    }

    @Test
    void switchesRealQuestionContractAndPreservesV1() throws Exception {
        var original = first("1");
        var modern = first("2");
        assertThat(original.has("imageUrl")).isFalse();
        assertThat(original.has("timeLimitSeconds")).isFalse();
        for (String field : new String[] {"id", "quizId", "quizTitle", "text", "options"})
            assertThat(modern.get(field)).isEqualTo(original.get(field));
        assertThat(modern.path("imageUrl").asText()).startsWith("https://");
        assertThat(modern.path("timeLimitSeconds").asInt()).isEqualTo(30);
        assertThat(modern.toString()).doesNotContain("isCorrect", "\"correct\"");
        assertThat(first("1")).isEqualTo(original);
    }

    @Test
    void defaultIsV1AndBadVersionsAreRejectedAcrossResources() throws Exception {
        mvc.perform(get("/quizzes/1/questions")).andExpect(status().isOk())
                .andExpect(header().string("X-API-Version", "1"));
        for (String path : new String[] {"/quizzes", "/quizzes/1/questions", "/users/me"})
            mvc.perform(get(path).header("X-API-Version", "3")).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("X-API-Version deve ser 1 ou 2"));
    }

    @Test
    void missingMediaIsNullableAndDoesNotBreakQuestion() throws Exception {
        var question = questions.findById(1L).orElseThrow();
        question.setImageUrl(null); question.setImageAltText(null); questions.saveAndFlush(question);
        var result = first("2");
        assertThat(result.has("imageUrl")).isTrue();
        assertThat(result.get("imageUrl").isNull()).isTrue();
        assertThat(result.path("options").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void v2RetainsPaginationAndAuthentication() throws Exception {
        mvc.perform(get("/quizzes/1/questions").with(jwt().jwt(token -> token.subject("1"))).header("X-API-Version", "2").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(3));
        mvc.perform(get("/quizzes/9999/questions").with(jwt().jwt(token -> token.subject("1"))).header("X-API-Version", "2")).andExpect(status().isNotFound());
        mvc.perform(post("/quizzes/1/attempts").header("X-API-Version", "2")
                .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/quizzes/1/attempts").with(jwt().jwt(token -> token.subject("1")))
                .header("X-API-Version", "2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.points").value(10));
    }

    @Test
    void versionDocumentsArePublicAndSelectTheirHeader() throws Exception {
        for (String version : new String[] {"1", "2"}) {
            mvc.perform(get("/api-docs/v" + version)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.version").value(version + ".0.0"));
        }
        mvc.perform(get("/api/versioned/status").header("X-API-Version", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionMedia").value(true));
    }
}
