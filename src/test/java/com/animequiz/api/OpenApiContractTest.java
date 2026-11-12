package com.animequiz.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que o OpenAPI publicado em {@code /api-docs} descreve o que o código realmente faz:
 * endpoints reais, endpoints removidos marcados como 410 e respostas de erro apenas onde existem.
 *
 * <p>Não escreve no banco, por isso não precisa de transação.
 */
@SpringBootTest
class OpenApiContractTest {

    private static final Set<String> EXPECTED_PATHS = Set.of(
            "/users", "/users/{id}", "/users/search", "/users/me", "/auth/register", "/auth/login",
            "/profiles", "/profiles/{id}", "/profiles/search",
            "/quizzes", "/quizzes/{id}", "/quizzes/search", "/quizzes/difficulty/{difficulty}",
            "/quizzes/{quizId}/attempts", "/quizzes/{quizId}/questions", "/users/{userId}/profile",
            "/questions", "/questions/{id}", "/questions/by-quiz/{quizId}",
            "/options", "/options/{id}", "/options/by-question/{questionId}",
            "/scores", "/scores/{id}", "/scores/ranking",
            "/api/hello");

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String docs() throws Exception {
        return mockMvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ somente endpoints reais

    @Test
    void everyRealEndpointIncludingAuthenticationIsDocumented() throws Exception {
        Map<String, Object> paths = JsonPath.read(docs(), "$.paths");

        assertThat(paths.keySet()).containsAll(EXPECTED_PATHS);
        assertThat(paths.keySet()).contains("/auth/register", "/auth/login", "/users/me");
    }

    @Test
    void authenticationAndProtectedOperationsDeclareBearerJwtSecurity() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/auth/register'].post.responses['201']").exists());
    }

    @Test
    void optionResponseSchemasNeverDeclareTheAnswerKey() throws Exception {
        Map<String, Map<String, Object>> schemas = JsonPath.read(docs(), "$.components.schemas");

        Set<String> optionResponseSchemas = schemas.keySet().stream()
                .filter(name -> name.contains("OptionResponse"))
                .collect(java.util.stream.Collectors.toSet());
        assertThat(optionResponseSchemas).isNotEmpty();

        for (String name : optionResponseSchemas) {
            String schema = String.valueOf(schemas.get(name));
            assertThat(schema).as("schema %s", name).doesNotContain("isCorrect").doesNotContain("correct=");
        }
    }

    // ------------------------------------------------------------------ categoria, questionCount e questões com alternativas

    @Test
    void quizSchemasDocumentCategoryAndQuestionCount() throws Exception {
        String docs = docs();
        Map<String, Map<String, Object>> schemas = JsonPath.read(docs, "$.components.schemas");

        String allSchemas = String.valueOf(schemas);
        for (String category : new String[] {"SHONEN", "SEINEN", "SHOJO", "JOSEI", "KODOMO"}) {
            assertThat(allSchemas).as("categoria %s documentada", category).contains(category);
        }
        assertThat(schemas.entrySet().stream()
                .filter(entry -> entry.getKey().contains("QuizResponse"))
                .map(entry -> String.valueOf(entry.getValue())))
                .as("algum schema de resposta de quiz declara questionCount")
                .anyMatch(schema -> schema.contains("questionCount"));

        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.components.schemas.QuizRequest.required", hasItem("category")));
    }

    @Test
    void quizQuestionsEndpointDocumentsOnlyTheResponsesItCanProduce() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/questions'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/questions'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/questions'].get.responses['404']").exists())
                // é uma leitura: não há corpo (415) nem conflito (409)
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/questions'].get.responses['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/questions'].get.responses['415']").doesNotExist());
    }

    @Test
    void profileByUserEndpointIsDocumentedAsAReadOnlyLookup() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].get.responses['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].get.responses['415']").doesNotExist())
                // só leitura: o caminho não aceita escrita
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].post").doesNotExist())
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].put").doesNotExist())
                .andExpect(jsonPath("$.paths['/users/{userId}/profile'].delete").doesNotExist());
    }

    @Test
    void detailedProfileAndScoreSchemasDeclareTheNewFields() throws Exception {
        Map<String, Map<String, Object>> schemas = JsonPath.read(docs(), "$.components.schemas");

        String profileSchemas = schemas.entrySet().stream()
                .filter(entry -> entry.getKey().contains("UserProfileDetailsResponse"))
                .map(entry -> String.valueOf(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(" "));
        for (String field : new String[] {"level", "rankTitle", "xp", "levelStartXp", "nextLevelXp",
                "completedQuizzes", "correctAnswers", "totalAnswers"}) {
            assertThat(profileSchemas).as("campo %s no schema do perfil detalhado", field).contains(field);
        }

        String scoreSchemas = schemas.entrySet().stream()
                .filter(entry -> entry.getKey().contains("ScoreResponse"))
                .map(entry -> String.valueOf(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(" "));
        for (String field : new String[] {"correctAnswers", "answeredQuestions", "totalQuestions"}) {
            assertThat(scoreSchemas).as("campo %s no schema de score", field).contains(field);
        }
    }

    @Test
    void questionsWithOptionsSchemasNeverDeclareTheAnswerKey() throws Exception {
        Map<String, Map<String, Object>> schemas = JsonPath.read(docs(), "$.components.schemas");

        Set<String> responseSchemas = schemas.keySet().stream()
                .filter(name -> name.contains("QuestionWithOptionsResponse") || name.contains("OptionSummaryResponse"))
                .collect(java.util.stream.Collectors.toSet());
        assertThat(responseSchemas).isNotEmpty();

        for (String name : responseSchemas) {
            String schema = String.valueOf(schemas.get(name));
            assertThat(schema).as("schema %s", name).doesNotContain("isCorrect").doesNotContain("correct=");
        }
    }

    // ------------------------------------------------------------------ endpoints removidos (410)

    @Test
    void removedScoreEndpointsAreDeprecatedAndDocumentOnlyGone() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/scores'].post.deprecated").value(true))
                .andExpect(jsonPath("$.paths['/scores'].post.responses['410']").exists())
                .andExpect(jsonPath("$.paths['/scores'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/scores'].post.responses['201']").doesNotExist())
                .andExpect(jsonPath("$.paths['/scores/{id}'].put.deprecated").value(true))
                .andExpect(jsonPath("$.paths['/scores/{id}'].put.responses['410']").exists())
                .andExpect(jsonPath("$.paths['/scores/{id}'].put.responses['200']").doesNotExist());
    }

    // ------------------------------------------------------------------ fluxo de tentativas

    @Test
    void attemptEndpointDocumentsSuccessAndEveryErrorItCanProduce() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{quizId}/attempts'].post.responses['415']").exists());
    }

    // ------------------------------------------------------------------ respostas de erro só onde existem

    @Test
    void creationsReturn201WithLocationAndOnlyTheErrorsTheyCanProduce() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/users'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/users'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/users'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/users'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/users'].post.responses['415']").exists())
                // criar usuário não consulta outro recurso
                .andExpect(jsonPath("$.paths['/users'].post.responses['404']").doesNotExist())
                // criar questão exige um quiz existente, mas nunca gera conflito
                .andExpect(jsonPath("$.paths['/questions'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/questions'].post.responses['409']").doesNotExist());
    }

    @Test
    void readsDocument404OnlyWhenAnIdIsLookedUp() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/users/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/users/{id}'].get.responses['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/users/{id}'].get.responses['415']").doesNotExist())
                .andExpect(jsonPath("$.paths['/users'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/scores/ranking'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/scores/ranking'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/difficulty/{difficulty}'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/difficulty/{difficulty}'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/questions/by-quiz/{quizId}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/options/by-question/{questionId}'].get.responses['404']").exists());
    }

    @Test
    void deletesDocument204AndConflictOnlyWhereDependentsExist() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/options/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/options/{id}'].delete.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/options/{id}'].delete.responses['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/scores/{id}'].delete.responses['409']").doesNotExist())
                .andExpect(jsonPath("$.paths['/users/{id}'].delete.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/quizzes/{id}'].delete.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/questions/{id}'].delete.responses['409']").exists());
    }

    @Test
    void helloEndpointDocumentsOnlyItsSuccess() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(jsonPath("$.paths['/api/hello'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/hello'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/hello'].get.responses['400']").doesNotExist());
    }
}
