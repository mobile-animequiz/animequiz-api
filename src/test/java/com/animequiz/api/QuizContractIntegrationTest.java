package com.animequiz.api;

import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.entity.Option;
import com.animequiz.api.entity.Question;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.repository.OptionRepository;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Decisões D1 (categoria), D2 ({@code questionCount}) e D3 ({@code GET /quizzes/{quizId}/questions})
 * da Etapa 1, exercitadas contra o contexto completo (H2 + data.sql).
 *
 * <p>Depende dos dados de exemplo: quiz 1 (FACIL, SHONEN) com as questões 1, 3 e 4 e alternativas
 * 1-2, 5-7 e 8-10; quiz 2 (MEDIO, SHONEN) com as questões 2, 5 e 6 e alternativas 3-4, 11-13 e 14-16.
 * Cada teste é revertido ao final (@Transactional).
 */
@SpringBootTest
@Transactional
class QuizContractIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private WebApplicationContext context;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private OptionRepository optionRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

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

    /** Os itens da primeira (e única) coleção dentro de {@code _embedded}, sem depender do nome da chave. */
    private JsonNode embeddedItems(String body) throws Exception {
        JsonNode embedded = objectMapper.readTree(body).path("_embedded");
        assertThat(embedded.isMissingNode()).as("_embedded presente").isFalse();
        return embedded.elements().next();
    }

    private static List<Long> idsOf(JsonNode items) {
        List<Long> ids = new ArrayList<>();
        items.forEach(item -> ids.add(item.get("id").asLong()));
        return ids;
    }

    private static List<String> fieldNamesOf(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String quizJson(String title, String difficulty, String category) {
        return "{\"title\":\"" + title + "\",\"description\":\"Descrição do quiz\",\"difficulty\":\""
                + difficulty + "\",\"category\":\"" + category + "\"}";
    }

    // ------------------------------------------------------------------ D1: categoria

    @ParameterizedTest
    @ValueSource(strings = {"SHONEN", "SEINEN", "SHOJO", "JOSEI", "KODOMO"})
    void everyCategoryIsAcceptedAndReturnedExactly(String category) throws Exception {
        mockMvc.perform(post("/quizzes").contentType(MediaType.APPLICATION_JSON)
                        .content(quizJson("Quiz " + category, "MEDIO", category)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value(category));
    }

    @Test
    void creatingAQuizWithoutCategoryIsBadRequest() throws Exception {
        mockMvc.perform(post("/quizzes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quiz sem categoria\",\"description\":\"Descrição do quiz\",\"difficulty\":\"FACIL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("A categoria é obrigatória")));
    }

    @Test
    void updatingAQuizWithoutCategoryIsBadRequest() throws Exception {
        mockMvc.perform(put("/quizzes/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Naruto Shippuden Quiz\",\"description\":\"Conhecimento ninja!\",\"difficulty\":\"FACIL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("A categoria é obrigatória")));
    }

    @Test
    void unknownCategoryIsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post("/quizzes").contentType(MediaType.APPLICATION_JSON)
                        .content(quizJson("Quiz isekai", "FACIL", "ISEKAI")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatingAQuizChangesItsCategoryAndKeepsTheQuestionCount() throws Exception {
        mockMvc.perform(put("/quizzes/1").contentType(MediaType.APPLICATION_JSON)
                        .content(quizJson("Naruto Shippuden Quiz", "FACIL", "SEINEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("SEINEN"))
                .andExpect(jsonPath("$.questionCount").value(3));
        mockMvc.perform(get("/quizzes/1"))
                .andExpect(jsonPath("$.category").value("SEINEN"));
    }

    // ------------------------------------------------------------------ D2: questionCount

    @Test
    void quizByIdExposesCategoryAndQuestionCount() throws Exception {
        mockMvc.perform(get("/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Naruto Shippuden Quiz"))
                .andExpect(jsonPath("$.difficulty").value("FACIL"))
                .andExpect(jsonPath("$.category").value("SHONEN"))
                .andExpect(jsonPath("$.questionCount").value(3));
    }

    @Test
    void listingExposesCategoryAndTheQuestionCountOfEachQuiz() throws Exception {
        mockMvc.perform(get("/quizzes?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded..questionCount", contains(3, 3)))
                .andExpect(jsonPath("$._embedded..category", contains("SHONEN", "SHONEN")));
    }

    @Test
    void searchAndDifficultyListingsAlsoExposeTheQuestionCount() throws Exception {
        mockMvc.perform(get("/quizzes/search?title=naruto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded..questionCount", contains(3)))
                .andExpect(jsonPath("$._embedded..category", contains("SHONEN")));
        mockMvc.perform(get("/quizzes/difficulty/MEDIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded..title", contains("One Piece Quiz")))
                .andExpect(jsonPath("$._embedded..questionCount", contains(3)));
    }

    @Test
    void aNewQuizStartsWithZeroQuestionsAndTheCountFollowsTheQuestionsAdded() throws Exception {
        String created = bodyOf(post("/quizzes").contentType(MediaType.APPLICATION_JSON)
                .content(quizJson("Quiz recém criado", "FACIL", "KODOMO")));
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();
        assertThat((Integer) JsonPath.read(created, "$.questionCount")).isZero();

        mockMvc.perform(get("/quizzes/" + id)).andExpect(jsonPath("$.questionCount").value(0));

        mockMvc.perform(post("/questions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + id + ",\"text\":\"Primeira pergunta\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/quizzes/" + id)).andExpect(jsonPath("$.questionCount").value(1));

        mockMvc.perform(post("/questions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + id + ",\"text\":\"Segunda pergunta\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/quizzes/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(quizJson("Quiz recém criado", "FACIL", "KODOMO")))
                .andExpect(jsonPath("$.questionCount").value(2));
    }

    @Test
    void theQuestionCountIsNeverAcceptedFromTheClient() throws Exception {
        mockMvc.perform(post("/quizzes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quiz com contagem falsa\",\"description\":\"Descrição do quiz\","
                                + "\"difficulty\":\"FACIL\",\"category\":\"JOSEI\",\"questionCount\":99}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionCount").value(0));
    }

    @Test
    void listingStatementCountDoesNotGrowWithTheNumberOfQuizzes() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        mockMvc.perform(get("/quizzes?page=0&size=100")).andExpect(status().isOk());
        long withTwoQuizzes = statistics.getPrepareStatementCount();

        for (int i = 0; i < 10; i++) {
            quizRepository.save(new Quiz("Quiz extra " + i, "Descrição do quiz", Difficulty.FACIL, Category.SEINEN));
        }
        quizRepository.flush();

        statistics.clear();
        mockMvc.perform(get("/quizzes?page=0&size=100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(12));
        long withTwelveQuizzes = statistics.getPrepareStatementCount();

        // Com N+1 seriam 2 consultas a mais por quiz criado; com a consulta agrupada o custo é o mesmo.
        assertThat(withTwelveQuizzes).isEqualTo(withTwoQuizzes);
    }

    // ------------------------------------------------------------------ D3: GET /quizzes/{quizId}/questions

    @Test
    void quizOneQuestionsComeWithOptionsInIdOrderAndWithoutTheAnswerKey() throws Exception {
        String body = bodyOf(get("/quizzes/1/questions?page=0&size=100"));

        assertThat(body).doesNotContain("\"isCorrect\"").doesNotContain("\"correct\"");

        JsonNode items = embeddedItems(body);
        assertThat(idsOf(items)).containsExactly(1L, 3L, 4L);

        JsonNode first = items.get(0);
        assertThat(first.get("quizId").asLong()).isEqualTo(1L);
        assertThat(first.get("quizTitle").asText()).isEqualTo("Naruto Shippuden Quiz");
        assertThat(first.get("text").asText()).isEqualTo("Qual é o sonho de Naruto?");

        assertThat(idsOf(items.get(0).get("options"))).containsExactly(1L, 2L);
        assertThat(idsOf(items.get(1).get("options"))).containsExactly(5L, 6L, 7L);
        assertThat(idsOf(items.get(2).get("options"))).containsExactly(8L, 9L, 10L);
        assertThat(items.get(0).get("options").get(0).get("text").asText()).isEqualTo("Ser Hokage");
    }

    @Test
    void quizTwoQuestionsComeWithTheirOwnOptions() throws Exception {
        JsonNode items = embeddedItems(bodyOf(get("/quizzes/2/questions?page=0&size=100")));

        assertThat(idsOf(items)).containsExactly(2L, 5L, 6L);
        assertThat(idsOf(items.get(0).get("options"))).containsExactly(3L, 4L);
        assertThat(idsOf(items.get(1).get("options"))).containsExactly(11L, 12L, 13L);
        assertThat(idsOf(items.get(2).get("options"))).containsExactly(14L, 15L, 16L);
    }

    @Test
    void eachQuestionAndEachOptionExposeOnlyTheContractFields() throws Exception {
        JsonNode items = embeddedItems(bodyOf(get("/quizzes/1/questions?page=0&size=100")));

        for (JsonNode question : items) {
            assertThat(fieldNamesOf(question))
                    .containsExactlyInAnyOrder("id", "quizId", "quizTitle", "text", "options", "_links");
            assertThat(question.get("options")).hasSizeGreaterThanOrEqualTo(2);
            for (JsonNode option : question.get("options")) {
                assertThat(fieldNamesOf(option)).containsExactlyInAnyOrder("id", "text");
            }
        }
    }

    @Test
    void anOptionAddedLaterAppearsLastAndStillWithoutTheAnswerKey() throws Exception {
        String created = bodyOf(post("/options").contentType(MediaType.APPLICATION_JSON)
                .content("{\"questionId\":1,\"text\":\"Ser Jounin\",\"isCorrect\":true}"));
        long newId = ((Number) JsonPath.read(created, "$.id")).longValue();

        String body = bodyOf(get("/quizzes/1/questions?page=0&size=100"));
        assertThat(body).doesNotContain("\"isCorrect\"").doesNotContain("\"correct\"");

        JsonNode options = embeddedItems(body).get(0).get("options");
        assertThat(idsOf(options)).containsExactly(1L, 2L, newId);
        assertThat(options.get(2).get("text").asText()).isEqualTo("Ser Jounin");
    }

    @Test
    void quizQuestionsArePaginated() throws Exception {
        String firstPage = bodyOf(get("/quizzes/1/questions?page=0&size=2"));
        assertThat(idsOf(embeddedItems(firstPage))).containsExactly(1L, 3L);
        assertThat((Integer) JsonPath.read(firstPage, "$.page.totalElements")).isEqualTo(3);
        assertThat((Integer) JsonPath.read(firstPage, "$.page.totalPages")).isEqualTo(2);

        String secondPage = bodyOf(get("/quizzes/1/questions?page=1&size=2"));
        JsonNode items = embeddedItems(secondPage);
        assertThat(idsOf(items)).containsExactly(4L);
        assertThat(idsOf(items.get(0).get("options"))).containsExactly(8L, 9L, 10L);
    }

    @Test
    void aQuizWithoutQuestionsReturnsAnEmptyList() throws Exception {
        Quiz empty = quizRepository.save(new Quiz("Quiz vazio", "Nenhuma questão cadastrada", Difficulty.FACIL, Category.SHOJO));

        mockMvc.perform(get("/quizzes/" + empty.getId() + "/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void questionsOfAnUnknownQuizAreNotFound() throws Exception {
        mockMvc.perform(get("/quizzes/999/questions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Quiz não encontrado com ID: 999")));
    }

    @Test
    void aNonNumericQuizIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/quizzes/abc/questions"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theLegacyEndpointsKeepTheirContract() throws Exception {
        JsonNode questions = embeddedItems(bodyOf(get("/questions/by-quiz/1?page=0&size=100")));
        assertThat(idsOf(questions)).containsExactly(1L, 3L, 4L);
        for (JsonNode question : questions) {
            assertThat(fieldNamesOf(question)).containsExactlyInAnyOrder("id", "quizId", "quizTitle", "text", "_links");
        }

        JsonNode options = embeddedItems(bodyOf(get("/options/by-question/3?page=0&size=100")));
        assertThat(idsOf(options)).containsExactly(5L, 6L, 7L);
        for (JsonNode option : options) {
            assertThat(fieldNamesOf(option)).containsExactlyInAnyOrder("id", "questionId", "text", "_links");
        }
    }

    @Test
    void quizQuestionsStatementCountDoesNotGrowWithTheNumberOfQuestions() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        mockMvc.perform(get("/quizzes/1/questions?page=0&size=100")).andExpect(status().isOk());
        long withThreeQuestions = statistics.getPrepareStatementCount();

        Quiz quiz = quizRepository.findById(1L).orElseThrow();
        for (int i = 0; i < 10; i++) {
            Question question = questionRepository.save(new Question(quiz, "Pergunta extra número " + i));
            optionRepository.save(new Option(question, "Alternativa A", true));
            optionRepository.save(new Option(question, "Alternativa B", false));
        }
        questionRepository.flush();
        optionRepository.flush();

        statistics.clear();
        mockMvc.perform(get("/quizzes/1/questions?page=0&size=100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(13));
        long withThirteenQuestions = statistics.getPrepareStatementCount();

        // Sem N+1 o custo é constante: existência do quiz + página de questões + alternativas (uma consulta com IN).
        assertThat(withThirteenQuestions).isEqualTo(withThreeQuestions);
    }
}
