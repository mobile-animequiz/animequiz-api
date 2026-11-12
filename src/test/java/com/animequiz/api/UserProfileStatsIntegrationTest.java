package com.animequiz.api;

import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.entity.Score;
import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.repository.QuizRepository;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserProfileRepository;
import com.animequiz.api.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D5: perfil por usuário ({@code GET /users/{userId}/profile}) com XP, nível, patente e estatísticas, perfil
 * automático para todo usuário e os novos números de cada tentativa no {@code Score}.
 *
 * <p>Dados de exemplo: usuários 1 (Naruto), 2 (Sasuke) e 3 (Luffy); scores 1 (Naruto, quiz 1, 30 pts, 3/3/3),
 * 2 (Sasuke, quiz 1, 20 pts, 2 acertos em 3 respostas) e 3 (Luffy, quiz 2, 40 pts, 2 acertos em 3 respostas).
 * Respostas certas por questão: quiz 1 = (1,1) (3,5) (4,8); quiz 2 = (2,3) (5,11) (6,14).
 * Cada teste é revertido ao final (@Transactional).
 */
@SpringBootTest
@Transactional
class UserProfileStatsIntegrationTest {

    private static final long[][] QUIZ_1_ALL_CORRECT = {{1, 1}, {3, 5}, {4, 8}};
    private static final long[][] QUIZ_1_ONE_CORRECT_TWO_WRONG = {{1, 1}, {3, 6}, {4, 9}};
    private static final long[][] QUIZ_2_ALL_CORRECT = {{2, 3}, {5, 11}, {6, 14}};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private ScoreRepository scoreRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .defaultRequest(get("/").with(jwt().jwt(token -> token.subject("1").claim("roles", List.of("ADMIN")))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                .build();
    }

    // ------------------------------------------------------------------ auxiliares

    private String bodyOf(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String attemptJson(long userId, long[][] answers) {
        StringBuilder json = new StringBuilder("{\"answers\":[");
        for (int i = 0; i < answers.length; i++) {
            if (i > 0) json.append(',');
            json.append("{\"questionId\":").append(answers[i][0]).append(",\"optionId\":").append(answers[i][1]).append('}');
        }
        return json.append("]}").toString();
    }

    /** Envia uma tentativa e devolve o ID do score criado. */
    private long submit(long userId, long quizId, long[][] answers) throws Exception {
        String body = bodyOf(post("/quizzes/" + quizId + "/attempts")
                .contentType(MediaType.APPLICATION_JSON).content(attemptJson(userId, answers))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject(Long.toString(userId)).claim("roles", List.of("USER")))));
        return ((Number) JsonPath.read(body, "$.scoreId")).longValue();
    }

    private void assertStats(long userId, long xp, int completedQuizzes, long correctAnswers, long totalAnswers) throws Exception {
        mockMvc.perform(get("/users/" + userId + "/profile")
                        .with(jwt().jwt(token -> token.subject(Long.toString(userId)).claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").value(xp))
                .andExpect(jsonPath("$.completedQuizzes").value(completedQuizzes))
                .andExpect(jsonPath("$.correctAnswers").value(correctAnswers))
                .andExpect(jsonPath("$.totalAnswers").value(totalAnswers));
    }

    // ------------------------------------------------------------------ perfil para todo usuário

    @Test
    void everySampleUserHasAProfile() {
        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(3);
        for (User user : users) {
            assertThat(userProfileRepository.existsByUserId(user.getId())).as("perfil de %s", user.getUsername()).isTrue();
        }
        assertThat(userProfileRepository.count()).isEqualTo(users.size());
    }

    @Test
    void aUserCreatedThroughTheApiGetsAnEmptyProfileWithZeroedStats() throws Exception {
        String created = bodyOf(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"Usuário novo\",\"email\":\"novo@teste.com\"}"));
        long userId = ((Number) JsonPath.read(created, "$.id")).longValue();

        assertThat(userProfileRepository.existsByUserId(userId)).isTrue();
        mockMvc.perform(get("/users/" + userId + "/profile")
                        .with(jwt().jwt(token -> token.subject(Long.toString(userId)).claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("Usuário novo"))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.rankTitle").value("Iniciante"))
                .andExpect(jsonPath("$.xp").value(0))
                .andExpect(jsonPath("$.levelStartXp").value(0))
                .andExpect(jsonPath("$.nextLevelXp").value(100))
                .andExpect(jsonPath("$.completedQuizzes").value(0))
                .andExpect(jsonPath("$.correctAnswers").value(0))
                .andExpect(jsonPath("$.totalAnswers").value(0));
    }

    @Test
    void creatingTheSameUserTwiceDoesNotCreateASecondProfile() throws Exception {
        long profilesBefore = userProfileRepository.count();
        String payload = "{\"username\":\"Usuário repetido\",\"email\":\"repetido@teste.com\"}";

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());

        assertThat(userProfileRepository.count()).isEqualTo(profilesBefore + 1);
    }

    // ------------------------------------------------------------------ leitura sem escrita

    @Test
    void readingTheProfileOfAUserWithoutOneIsNotFoundAndNeverCreatesIt() throws Exception {
        User orphan = userRepository.save(new User("Sem Perfil", "sem.perfil@teste.com"));
        long profilesBefore = userProfileRepository.count();

        mockMvc.perform(get("/users/" + orphan.getId() + "/profile")
                        .with(jwt().jwt(token -> token.subject(Long.toString(orphan.getId())).claim("roles", List.of("USER")))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Perfil não encontrado para o usuário com ID: " + orphan.getId())));

        assertThat(userProfileRepository.count()).isEqualTo(profilesBefore);
        assertThat(userProfileRepository.existsByUserId(orphan.getId())).isFalse();
    }

    @Test
    void deletingTheProfileMakesTheRouteNotFoundWithoutRecreatingIt() throws Exception {
        long profileId = userProfileRepository.findByUserId(1L).orElseThrow().getId();

        mockMvc.perform(delete("/profiles/" + profileId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/users/1/profile")).andExpect(status().isNotFound());
        assertThat(userProfileRepository.existsByUserId(1L)).isFalse();
    }

    @Test
    void readingAProfileDoesNotChangeAnyData() throws Exception {
        long profiles = userProfileRepository.count();
        long scores = scoreRepository.count();

        mockMvc.perform(get("/users/1/profile")).andExpect(status().isOk());
        mockMvc.perform(get("/users/1/profile")).andExpect(status().isOk());

        assertThat(userProfileRepository.count()).isEqualTo(profiles);
        assertThat(scoreRepository.count()).isEqualTo(scores);
    }

    @Test
    void anUnknownUserIsNotFound() throws Exception {
        mockMvc.perform(get("/users/9999/profile")
                        .with(jwt().jwt(token -> token.subject("9999").claim("roles", List.of("USER")))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Usuário não encontrado com ID: 9999")));
    }

    @Test
    void aNonNumericUserIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/users/abc/profile")).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ estatísticas dos dados de exemplo

    @Test
    void narutoSampleStats() throws Exception {
        mockMvc.perform(get("/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("Naruto Uzumaki"))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.rankTitle").value("Iniciante"))
                .andExpect(jsonPath("$.levelStartXp").value(0))
                .andExpect(jsonPath("$.nextLevelXp").value(100));
        assertStats(1, 30, 1, 3, 3);
    }

    @Test
    void sasukeAndLuffySampleStats() throws Exception {
        // Sasuke: 2 acertos em 3 respostas no quiz 1 (20 pts); Luffy: 2 acertos em 3 respostas no quiz 2 (40 pts)
        assertStats(2, 20, 1, 2, 3);
        assertStats(3, 40, 1, 2, 3);
    }

    // ------------------------------------------------------------------ regras de XP e histórico

    @Test
    void repeatingAQuizWithAWorseScoreKeepsTheXpButAccumulatesTheHistory() throws Exception {
        // Naruto já tem 30 no quiz 1; nova tentativa: 1 acerto em 3 respostas = 10 pontos
        submit(1, 1, QUIZ_1_ONE_CORRECT_TWO_WRONG);

        assertStats(1, 30, 1, 3 + 1, 3 + 3);
    }

    @Test
    void repeatingAQuizWithTheSameScoreDoesNotIncreaseXpOrCompletedQuizzes() throws Exception {
        submit(1, 1, QUIZ_1_ALL_CORRECT);
        submit(1, 1, QUIZ_1_ALL_CORRECT);

        assertStats(1, 30, 1, 3 + 3 + 3, 3 + 3 + 3);
    }

    @Test
    void aBetterAttemptRaisesTheXpToTheNewBestScoreOnly() throws Exception {
        // Sasuke tinha 20 no quiz 1; tentativa perfeita = 30 (o XP vira 30, não 20 + 30)
        submit(2, 1, QUIZ_1_ALL_CORRECT);

        assertStats(2, 30, 1, 2 + 3, 3 + 3);
    }

    @Test
    void aSecondQuizAddsItsBestScoreAndCountsAsAnotherCompletedQuiz() throws Exception {
        submit(1, 2, QUIZ_2_ALL_CORRECT);   // 3 acertos x 20 = 60

        assertStats(1, 30 + 60, 2, 3 + 3, 3 + 3);
        mockMvc.perform(get("/users/1/profile"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.nextLevelXp").value(100));
    }

    @Test
    void aPartialAttemptCountsOnlyTheAnswersThatWereSent() throws Exception {
        long scoreId = submit(1, 1, new long[][] {{1, 1}, {3, 5}});   // 2 respostas, 2 acertos, 1 questão em branco

        Score stored = scoreRepository.findById(scoreId).orElseThrow();
        assertThat(stored.getCorrectAnswers()).isEqualTo(2);
        assertThat(stored.getAnsweredQuestions()).isEqualTo(2);
        assertThat(stored.getTotalQuestions()).isEqualTo(3);
        assertThat(stored.getPoints()).isEqualTo(20);

        // totalAnswers soma 2 (respostas enviadas), não 3 (questões do quiz); o XP segue sendo o melhor (30)
        assertStats(1, 30, 1, 3 + 2, 3 + 2);
    }

    @Test
    void theNumbersOfEachAttemptAreStoredInTheScore() throws Exception {
        long scoreId = submit(3, 2, new long[][] {{2, 3}, {5, 12}});   // acerta a 2, erra a 5, deixa a 6 em branco

        Score stored = scoreRepository.findById(scoreId).orElseThrow();
        assertThat(stored.getCorrectAnswers()).isEqualTo(1);
        assertThat(stored.getAnsweredQuestions()).isEqualTo(2);
        assertThat(stored.getTotalQuestions()).isEqualTo(3);
        assertThat(stored.getPoints()).isEqualTo(20);
    }

    @Test
    void editingTheQuizLaterDoesNotChangeTheStoredHistory() throws Exception {
        long scoreId = submit(1, 1, QUIZ_1_ALL_CORRECT);
        assertStats(1, 30, 1, 6, 6);

        // o quiz ganha uma 4ª questão: as tentativas antigas continuam valendo 3 de 3
        mockMvc.perform(post("/questions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":1,\"text\":\"Pergunta nova do quiz\"}"))
                .andExpect(status().isCreated());

        assertStats(1, 30, 1, 6, 6);
        assertThat(scoreRepository.findById(scoreId).orElseThrow().getTotalQuestions()).isEqualTo(3);
    }

    // ------------------------------------------------------------------ nível e patente pela API

    @ParameterizedTest
    @CsvSource({
            "0,1,Iniciante,0,100",
            "99,1,Iniciante,0,100",
            "100,2,Iniciante,100,300",
            "999,4,Iniciante,600,1000",
            "1000,5,Aprendiz,1000,1500",
            "2450,7,Aprendiz,2100,2800",
            "4500,10,Explorador,4500,5500",
            "10500,15,Veterano,10500,12000",
            "19000,20,Mestre,19000,21000",
            "43500,30,Grão-Mestre,43500,46500",
            "78000,40,Lenda,78000,82000",
            "122499,49,Lenda,117600,122500",
            "122500,50,Lenda,122500,127500",
            "200000,50,Lenda,122500,127500"})
    void levelRankAndLevelBoundsAreReturnedForTheXp(int xp, int level, String rank, long start, long next) throws Exception {
        User user = userRepository.save(new User("Nível " + xp, "nivel" + xp + "@teste.com"));
        userProfileRepository.save(new UserProfile(user, null, null));
        Quiz quiz = quizRepository.save(new Quiz("Quiz do nível " + xp, "Descrição do quiz", Difficulty.FACIL, Category.SHONEN));
        scoreRepository.save(new Score(user, quiz, xp, 3, 3, 3));

        mockMvc.perform(get("/users/" + user.getId() + "/profile")
                        .with(jwt().jwt(token -> token.subject(Long.toString(user.getId())).claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").value(xp))
                .andExpect(jsonPath("$.level").value(level))
                .andExpect(jsonPath("$.rankTitle").value(rank))
                .andExpect(jsonPath("$.levelStartXp").value(start))
                .andExpect(jsonPath("$.nextLevelXp").value(next));
    }

    // ------------------------------------------------------------------ contratos

    @Test
    void theDetailedProfileIsASupersetOfTheLegacyProfile() throws Exception {
        JsonNode node = objectMapper.readTree(bodyOf(get("/users/1/profile")));
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);

        assertThat(fields).contains("id", "userId", "username",
                "level", "rankTitle", "xp", "levelStartXp", "nextLevelXp",
                "completedQuizzes", "correctAnswers", "totalAnswers", "_links");
        assertThat(fields).doesNotContain("totalQuestions", "answeredQuestions", "isCorrect", "correct");
        assertThat(node.path("_links").has("self")).isTrue();
        assertThat(node.path("_links").has("profile")).isTrue();
    }

    @Test
    void theLegacyProfileEndpointsKeepWorkingWithoutStats() throws Exception {
        long profileId = userProfileRepository.findByUserId(1L).orElseThrow().getId();

        mockMvc.perform(get("/profiles/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("Naruto Uzumaki"))
                .andExpect(jsonPath("$.xp").doesNotExist())
                .andExpect(jsonPath("$.level").doesNotExist())
                .andExpect(jsonPath("$.rankTitle").doesNotExist())
                .andExpect(jsonPath("$.totalAnswers").doesNotExist());
        mockMvc.perform(get("/profiles?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void scoresExposeTheExactNumbersOfEachAttempt() throws Exception {
        mockMvc.perform(get("/scores/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(20))
                .andExpect(jsonPath("$.correctAnswers").value(2))
                .andExpect(jsonPath("$.answeredQuestions").value(3))
                .andExpect(jsonPath("$.totalQuestions").value(3));

        // o ranking continua ordenado por pontos e agora traz os mesmos campos
        mockMvc.perform(get("/scores/ranking?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded..points").value(org.hamcrest.Matchers.contains(40, 30, 20)))
                .andExpect(jsonPath("$._embedded..correctAnswers").value(org.hamcrest.Matchers.contains(2, 3, 2)))
                .andExpect(jsonPath("$._embedded..totalQuestions").value(org.hamcrest.Matchers.contains(3, 3, 3)));
    }
}
