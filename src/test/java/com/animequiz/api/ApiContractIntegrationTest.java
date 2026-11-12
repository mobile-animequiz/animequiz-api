package com.animequiz.api;

import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.entity.Option;
import com.animequiz.api.entity.Question;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.entity.Score;
import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.repository.OptionRepository;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizRepository;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserProfileRepository;
import com.animequiz.api.repository.UserRepository;
import com.animequiz.api.service.ScoreCalculator;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contratos HTTP da Fase 2, exercitados contra o contexto completo (H2 + data.sql).
 *
 * <p>Depende dos dados de exemplo (data.sql): usuários 1 a 3; quiz 1 (FACIL) com as questões 1, 3 e 4;
 * quiz 2 (MEDIO) com as questões 2, 5 e 6. Cada teste é revertido ao final (@Transactional).
 *
 * <p>O MockMvc é montado à mão (webAppContextSetup) para não depender de anotações de auto-configuração
 * de teste que mudam de pacote entre versões do Spring Boot.
 */
@SpringBootTest
@Transactional
class ApiContractIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private ScoreCalculator scoreCalculator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(token -> token.subject("1").claim("roles", List.of("ADMIN")))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                .build();
    }

    private String bodyOf(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------ gabarito

    @Test
    void optionResponsesNeverExposeTheAnswerKey() throws Exception {
        String single = bodyOf(get("/options/1"));
        String list = bodyOf(get("/options?size=100"));
        String byQuestion = bodyOf(get("/options/by-question/1"));

        for (String body : List.of(single, list, byQuestion)) {
            assertThat(body).contains("\"text\"");
            assertThat(body).doesNotContain("\"isCorrect\"");
            assertThat(body).doesNotContain("\"correct\"");
        }
    }

    @Test
    void creatingAnOptionStoresTheAnswerKeyButDoesNotReturnIt() throws Exception {
        String created = bodyOf(post("/options")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":1,\"text\":\"Ser Jounin\",\"isCorrect\":true}"));

        assertThat(created).contains("\"Ser Jounin\"");
        assertThat(created).doesNotContain("\"isCorrect\"");
        assertThat(created).doesNotContain("\"correct\"");
    }

    // ------------------------------------------------------------------ pontuação

    @Test
    void attemptInEasyQuizScoresTenPerCorrectAnswer() throws Exception {
        // questão 1 -> opção 1 (certa), questão 3 -> opção 5 (certa), questão 4 -> opção 9 (errada)
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[
                                  {"questionId":1,"optionId":1},
                                  {"questionId":3,"optionId":5},
                                  {"questionId":4,"optionId":9}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.difficulty").value("FACIL"))
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.answeredQuestions").value(3))
                .andExpect(jsonPath("$.correctAnswers").value(2))
                .andExpect(jsonPath("$.pointsPerCorrectAnswer").value(10))
                .andExpect(jsonPath("$.points").value(20))
                .andExpect(jsonPath("$._links.score.href").exists());
    }

    @Test
    void attemptInMediumQuizScoresTwentyPerCorrectAnswer() throws Exception {
        // questão 2 -> opção 3 (certa), questão 5 -> opção 12 (errada), questão 6 -> opção 14 (certa)
        mockMvc.perform(post("/quizzes/2/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[
                                  {"questionId":2,"optionId":3},
                                  {"questionId":5,"optionId":12},
                                  {"questionId":6,"optionId":14}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.difficulty").value("MEDIO"))
                .andExpect(jsonPath("$.correctAnswers").value(2))
                .andExpect(jsonPath("$.pointsPerCorrectAnswer").value(20))
                .andExpect(jsonPath("$.points").value(40));
    }

    @Test
    void attemptCreatesTheScoreInTheRanking() throws Exception {
        long before = scoreRepository.count();

        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isCreated());

        assertThat(scoreRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void attemptRejectsQuestionFromAnotherQuiz() throws Exception {
        // a questão 2 é do quiz 2
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":2,\"optionId\":3}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attemptRejectsEmptyAnswers() throws Exception {
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("answers")));
    }

    @Test
    void attemptOnUnknownQuizIsNotFound() throws Exception {
        mockMvc.perform(post("/quizzes/9999/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientSuppliedPointsEndpointsAreGone() throws Exception {
        mockMvc.perform(post("/scores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":1,\"points\":999999}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message", containsString("/quizzes/{quizId}/attempts")));

        mockMvc.perform(put("/scores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":1,\"points\":999999}"))
                .andExpect(status().isGone());
    }

    @Test
    void sampleScoresRespectTheScoringRule() {
        List<Score> scores = scoreRepository.findAll();
        assertThat(scores).isNotEmpty();

        for (Score score : scores) {
            int perCorrect = scoreCalculator.pointsPerCorrectAnswer(score.getQuiz().getDifficulty());
            int questionCount = questionRepository.findAllByQuizId(score.getQuiz().getId()).size();

            assertThat(score.getPoints() % perCorrect)
                    .as("score %s deve ser múltiplo de %s", score.getId(), perCorrect).isZero();
            assertThat(score.getPoints() / perCorrect)
                    .as("score %s não pode ter mais acertos que questões (%s)", score.getId(), questionCount)
                    .isLessThanOrEqualTo(questionCount);
        }
    }

    // ------------------------------------------------------------------ erros que antes eram 500

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownEnumValueInPathIsBadRequest() throws Exception {
        mockMvc.perform(get("/quizzes/difficulty/INEXISTENTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownRouteIsNotFound() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(patch("/quizzes/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));
    }

    @Test
    void shortQuestionTextIsValidationErrorNotServerError() throws Exception {
        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":1,\"text\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("text")));
    }

    @Test
    void tooLongOptionTextIsValidationErrorNotServerError() throws Exception {
        String longText = "x".repeat(151);
        mockMvc.perform(post("/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":1,\"text\":\"" + longText + "\",\"isCorrect\":false}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ integridade (409)

    @Test
    void updatingUserWithAnotherUsersUsernameIsConflict() throws Exception {
        mockMvc.perform(put("/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Naruto Uzumaki\",\"email\":\"sasuke@konoha.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingUserWithProfileIsConflict() throws Exception {
        User user = userRepository.save(new User("Roronoa Zoro", "zoro@onepiece.com"));
        userProfileRepository.save(new UserProfile(user, "Espadachim", null));

        mockMvc.perform(delete("/users/" + user.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingQuizWithQuestionsIsConflict() throws Exception {
        mockMvc.perform(delete("/quizzes/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingQuestionWithOptionsIsConflict() throws Exception {
        mockMvc.perform(delete("/questions/1"))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ fluxo de leitura do app

    @Test
    void quizzesAreListedWithSampleData() throws Exception {
        mockMvc.perform(get("/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void questionsOfAQuizAreListedWithoutAnyAnswerKey() throws Exception {
        String body = bodyOf(get("/questions/by-quiz/1"));

        assertThat(body).doesNotContain("\"isCorrect\"").doesNotContain("\"correct\"");
        mockMvc.perform(get("/questions/by-quiz/1"))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void optionsOfAQuestionAreListedByQuestion() throws Exception {
        // a questão 3 tem 3 alternativas (ids 5, 6 e 7)
        mockMvc.perform(get("/options/by-question/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void singleOptionExposesOnlyIdQuestionIdAndText() throws Exception {
        mockMvc.perform(get("/options/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.questionId").value(1))
                .andExpect(jsonPath("$.text").value("Ser Hokage"))
                .andExpect(jsonPath("$.isCorrect").doesNotExist())
                .andExpect(jsonPath("$.correct").doesNotExist());
    }

    @Test
    void helloEndpointResponds() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AnimeQuiz")));
    }

    // ------------------------------------------------------------------ tentativas (casos adicionais)

    @Test
    void unansweredQuestionsCountAsWrongInTheResponse() throws Exception {
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalQuestions").value(3))
                .andExpect(jsonPath("$.answeredQuestions").value(1))
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.points").value(10));
    }

    @Test
    void attemptWithOnlyWrongAnswersScoresZero() throws Exception {
        // questão 1 -> opção 2 (errada), questão 3 -> opção 6 (errada)
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":2},{\"questionId\":3,\"optionId\":6}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correctAnswers").value(0))
                .andExpect(jsonPath("$.points").value(0));
    }

    @Test
    void attemptIgnoresPointsSentByTheClient() throws Exception {
        // campos desconhecidos no corpo são descartados; a pontuação continua sendo a calculada no servidor
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":999999,\"correctAnswers\":3,"
                                + "\"answers\":[{\"questionId\":1,\"optionId\":1,\"isCorrect\":true}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.points").value(10));
    }

    @Test
    void attemptResponseNeverRevealsWhichQuestionsWereCorrect() throws Exception {
        String body = bodyOf(post("/quizzes/1/attempts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":1,\"optionId\":1},{\"questionId\":3,\"optionId\":6}]}"));

        assertThat(body).doesNotContain("\"isCorrect\"").doesNotContain("\"correct\"");
        assertThat(body).doesNotContain("\"answers\"").doesNotContain("\"results\"");
    }

    @Test
    void attemptRejectsOptionFromAnotherQuestion() throws Exception {
        // a opção 3 pertence à questão 2
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":3}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attemptRejectsDuplicateAnswersForTheSameQuestion() throws Exception {
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1},{\"questionId\":1,\"optionId\":2}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attemptRejectsNonexistentOption() throws Exception {
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":9999}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attemptUsesAuthenticatedUserInsteadOfClientSuppliedUserId() throws Exception {
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":9999,\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void attemptNoLongerRequiresClientSuppliedUserId() throws Exception {
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void attemptWithNonNumericQuizIdIsBadRequest() throws Exception {
        mockMvc.perform(post("/quizzes/abc/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attemptOnQuizWithoutQuestionsIsConflict() throws Exception {
        Quiz empty = quizRepository.save(new Quiz("Quiz sem questões", "Nenhuma questão cadastrada", Difficulty.FACIL, Category.SHONEN));

        mockMvc.perform(post("/quizzes/" + empty.getId() + "/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void scoreCreatedByAnAttemptIsReadableAndMatchesTheResult() throws Exception {
        String body = bodyOf(post("/quizzes/1/attempts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":1,\"optionId\":1},{\"questionId\":3,\"optionId\":5}]}"));
        long scoreId = ((Number) JsonPath.read(body, "$.scoreId")).longValue();

        mockMvc.perform(get("/scores/" + scoreId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.quizId").value(1))
                .andExpect(jsonPath("$.points").value(20));
    }

    @Test
    void attemptIdentityComesFromAuthenticatedPrincipalNotCreatedUserId() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Rock Lee\",\"email\":\"lee@konoha.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        long userId = ((Number) JsonPath.read(created, "$.id")).longValue();
        assertThat(userId).isNotEqualTo(1L);

        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"optionId\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("Naruto Uzumaki"));
    }

    // ------------------------------------------------------------------ erros: 400, 404, 405, 415

    @Test
    void unsupportedContentTypeIsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("naruto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void missingBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("'id'")));
    }

    @Test
    void unknownSortPropertyIsBadRequest() throws Exception {
        mockMvc.perform(get("/users?sort=propriedadeInexistente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("propriedadeInexistente")));
    }

    @Test
    void missingRequiredQueryParameterIsBadRequest() throws Exception {
        mockMvc.perform(get("/quizzes/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));
    }

    @Test
    void methodNotAllowedOnScoreCollectionListsAllowedMethods() throws Exception {
        mockMvc.perform(delete("/scores"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/users/9999", "/profiles/9999", "/quizzes/9999", "/questions/9999", "/options/9999", "/scores/9999",
            "/questions/by-quiz/9999", "/options/by-question/9999"
    })
    void unknownResourceIsNotFound(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("9999")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/users/9999", "/profiles/9999", "/quizzes/9999", "/questions/9999", "/options/9999", "/scores/9999"
    })
    void deletingUnknownResourceIsNotFound(String url) throws Exception {
        mockMvc.perform(delete(url)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ integridade: duplicidades (409) e remoção

    @Test
    void creatingUserWithExistingEmailIsConflict() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Outro Nome\",\"email\":\"naruto@konoha.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void creatingUserWithExistingUsernameIsConflictIgnoringCase() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"naruto uzumaki\",\"email\":\"novo@konoha.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void creatingQuizWithExistingTitleIsConflict() throws Exception {
        mockMvc.perform(post("/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Naruto Shippuden Quiz\",\"description\":\"Outra descrição\",\"difficulty\":\"FACIL\",\"category\":\"SHONEN\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void creatingASecondProfileForTheSameUserIsConflict() throws Exception {
        User user = userRepository.save(new User("Tenten", "tenten@konoha.com"));
        userProfileRepository.save(new UserProfile(user, "Especialista em armas", null));

        mockMvc.perform(post("/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + user.getId() + ",\"bio\":\"Outra bio\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingUserWithScoresIsConflict() throws Exception {
        // o usuário 1 tem um score nos dados de exemplo
        mockMvc.perform(delete("/users/1")).andExpect(status().isConflict());
    }

    @Test
    void deletingAnOptionThatNothingDependsOnSucceeds() throws Exception {
        String created = bodyOf(post("/options")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":1,\"text\":\"Opção descartável\",\"isCorrect\":false}"));
        long optionId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(delete("/options/" + optionId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/options/" + optionId)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ coerência dos dados de exemplo (data.sql)

    @Test
    void everySampleQuestionHasExactlyOneCorrectOption() {
        Map<Long, Integer> correctPerQuestion = new HashMap<>();
        for (Question question : questionRepository.findAll()) {
            correctPerQuestion.put(question.getId(), 0);
        }
        for (Option option : optionRepository.findAll()) {
            if (Boolean.TRUE.equals(option.getCorrect())) {
                correctPerQuestion.merge(option.getQuestion().getId(), 1, Integer::sum);
            }
        }

        assertThat(correctPerQuestion).hasSize(6);
        correctPerQuestion.forEach((questionId, correct) ->
                assertThat(correct).as("questão %s deve ter exatamente 1 alternativa correta", questionId).isEqualTo(1));
    }

    @Test
    void sampleQuizzesHaveThreeQuestionsEachAndExpectedDifficulty() {
        assertThat(quizRepository.findById(1L).orElseThrow().getDifficulty()).isEqualTo(Difficulty.FACIL);
        assertThat(quizRepository.findById(2L).orElseThrow().getDifficulty()).isEqualTo(Difficulty.MEDIO);
        assertThat(quizRepository.findById(1L).orElseThrow().getCategory()).isEqualTo(Category.SHONEN);
        assertThat(quizRepository.findById(2L).orElseThrow().getCategory()).isEqualTo(Category.SHONEN);
        assertThat(questionRepository.findAllByQuizId(1L)).extracting(Question::getId).containsExactlyInAnyOrder(1L, 3L, 4L);
        assertThat(questionRepository.findAllByQuizId(2L)).extracting(Question::getId).containsExactlyInAnyOrder(2L, 5L, 6L);
    }

    @Test
    void sampleOptionIdsKeepTheirOriginalMeaning() {
        // IDs 1 a 4 existiam antes da Fase 2 e foram preservados
        assertThat(optionRepository.findById(1L).orElseThrow().getText()).isEqualTo("Ser Hokage");
        assertThat(optionRepository.findById(3L).orElseThrow().getText()).isEqualTo("Gomu Gomu no Pistol");
        assertThat(optionRepository.count()).isEqualTo(16);
    }

    // ------------------------------------------------------------------ contrato com o aplicativo móvel

    @Test
    void listingsAreHalEvenWhenTheClientAcceptsPlainJson() throws Exception {
        // O aplicativo envia "Accept: application/json" e lê os itens em _embedded, sem depender do nome da chave
        mockMvc.perform(get("/quizzes?page=0&size=100").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
                .andExpect(jsonPath("$._embedded").exists())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void quizResponseExposesTheFieldsTheHomeCardUses() throws Exception {
        mockMvc.perform(get("/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Naruto Shippuden Quiz"))
                .andExpect(jsonPath("$.difficulty").value("FACIL"))
                .andExpect(jsonPath("$.category").value("SHONEN"))
                .andExpect(jsonPath("$.questionCount").value(3))
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    void questionsOfAQuizAreReturnedInIdOrder() throws Exception {
        mockMvc.perform(get("/questions/by-quiz/1?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded..id", contains(1, 3, 4)));
        mockMvc.perform(get("/questions/by-quiz/2?page=0&size=100"))
                .andExpect(jsonPath("$._embedded..id", contains(2, 5, 6)));
    }

    @Test
    void optionsOfAQuestionAreReturnedInIdOrder() throws Exception {
        mockMvc.perform(get("/options/by-question/3?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded..id", contains(5, 6, 7)));
        mockMvc.perform(get("/options/by-question/5?page=0&size=100"))
                .andExpect(jsonPath("$._embedded..id", contains(11, 12, 13)));
    }

    @Test
    void attemptAcceptsIdsSentAsNumericStrings() throws Exception {
        // O aplicativo converte os IDs com String(id) antes de montar as respostas
        mockMvc.perform(post("/quizzes/1/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":\"1\",\"optionId\":\"1\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correctAnswers").value(1));
    }

    @Test
    void blankQuizTitleReturnsThePortugueseValidationMessage() throws Exception {
        mockMvc.perform(post("/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"Descrição válida\",\"difficulty\":\"FACIL\",\"category\":\"SHONEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("O título é obrigatório")));
    }

    @Test
    void invalidEmailReturnsThePortugueseValidationMessage() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Nome Valido\",\"email\":\"isto-nao-e-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Informe um e-mail válido")));
    }
}
