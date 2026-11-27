package com.animequiz.api;
import com.animequiz.api.configuration.CatalogInitializer;
import com.animequiz.api.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.DefaultApplicationArguments;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.catalog.seed-enabled=true","spring.sql.init.mode=never",
 "spring.datasource.url=jdbc:h2:mem:catalog-seed;DB_CLOSE_DELAY=-1","api.auth-limit.enabled=false"})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class CatalogIntegrationTest {
    @Autowired CatalogInitializer initializer;
    @Autowired QuizRepository quizzes;
    @Autowired QuestionRepository questions;
    @Autowired OptionRepository options;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Test void importsEightCompleteQuizzesIdempotentlyWithoutChangingQuestionIdentities() throws Exception {
        assertEquals(8,quizzes.count());assertEquals(128,questions.count());assertEquals(512,options.count());
        var firstIds=questions.findAll().stream().map(item->item.getId()).sorted().toList();
        initializer.run(new DefaultApplicationArguments());
        assertEquals(firstIds,questions.findAll().stream().map(item->item.getId()).sorted().toList());
        assertEquals(512,options.count());
        for(var quiz:quizzes.findAll()) assertEquals(16,questions.countByQuizId(quiz.getId()));
        for(var question:questions.findAll()) {
            var choices=options.findAll().stream().filter(option->option.getQuestion().getId().equals(question.getId())).toList();
            assertEquals(4,choices.size());assertEquals(1,choices.stream().filter(option->Boolean.TRUE.equals(option.getCorrect())).count());
        }
    }
    @Test void expandedQuizKeepsServerGradingXpAndRankingCompatible() throws Exception {
        var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/register")
            .contentType("application/json").content(json.writeValueAsString(java.util.Map.of(
                "username","catalog-player","email","catalog-player@example.test","password","ExamplePass9!")));
        var session=json.readTree(mvc.perform(request).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
            .andReturn().getResponse().getContentAsString());
        String authorization="Bearer "+session.path("accessToken").asText();
        long userId=session.path("user").path("id").asLong();
        var quiz=quizzes.findByTitleIgnoreCase("Naruto Shippuden Quiz").orElseThrow();
        var questionIds=questions.findAllByQuizId(quiz.getId()).stream().map(question->question.getId()).collect(java.util.stream.Collectors.toSet());
        var answers=options.findAll().stream().filter(option->questionIds.contains(option.getQuestion().getId()) && Boolean.TRUE.equals(option.getCorrect()))
            .map(option->java.util.Map.of("questionId",option.getQuestion().getId(),"optionId",option.getId())).toList();
        assertEquals(16,answers.size());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/quizzes/"+quiz.getId()+"/attempts")
            .header("Authorization",authorization).header("X-Idempotency-Key","catalog-expanded-attempt")
            .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("answers",answers))))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.correctAnswers").value(16))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalQuestions").value(16))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.points").value(160));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users/"+userId+"/profile").header("Authorization",authorization))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.xp").value(160));
    }

    @Test void v1KeepsBasicContractAndOnlyTwoVersionsAreAccepted() throws Exception {
        var quiz = quizzes.findAll().get(0);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/quizzes/"+quiz.getId())
            .header("X-API-Version","1").param("lang","en"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.imageUrl").doesNotExist())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.timeLimitSeconds").doesNotExist());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/quizzes").header("X-API-Version","3"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test void everySeedQuestionHasEnglishAlternativesAndV2ServesLocalizedContent() throws Exception {
        for (var question : questions.findAll()) assertNotNull(question.getEnglishText());
        for (var option : options.findAll()) assertNotNull(option.getEnglishText());
        var quiz = quizzes.findByTitleIgnoreCase("Naruto Shippuden Quiz").orElseThrow();
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/quizzes/"+quiz.getId()+"/questions")
            .header("X-API-Version","2").param("lang","en")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();
        var rows = json.readTree(result).path("_embedded").elements().next();
        assertEquals(16, rows.size());
        assertEquals("What is Naruto's dream?", rows.get(0).path("text").asText());
        assertEquals("en", rows.get(0).path("contentLanguage").asText());
        assertEquals(30, rows.get(0).path("timeLimitSeconds").asInt());
        assertFalse(result.contains("isCorrect"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(quiz.getImageUrl()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"));
    }

    @Test @org.springframework.transaction.annotation.Transactional
    void administratorCanCreateQuizAndPlayersSeeItsCoverWithoutGainingWritePermission() throws Exception {
        var admin = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
            .jwt(jwt->jwt.claim("roles", java.util.List.of("ADMIN")))
            .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
        String body = json.writeValueAsString(java.util.Map.of("title","Quiz novo do admin","description","Quiz para conferir integracao",
            "difficulty","DIFICIL","category","SHONEN","imageUrl","https://example.test/cover.png",
            "englishTitle","New admin quiz","englishDescription","An integration quiz"));
        String created = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/quizzes")
            .header("X-API-Version","2").with(admin).contentType("application/json").content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).path("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/quizzes/"+id)
            .header("X-API-Version","2").param("lang","en")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title").value("New admin quiz"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.imageUrl").value("https://example.test/cover.png"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.timeLimitSeconds").value(12));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/quizzes")
            .header("X-API-Version","2").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt())
            .contentType("application/json").content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Test void invalidContentLanguageIsRejected() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/quizzes")
            .header("X-API-Version","2").param("lang","xx")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }
}
