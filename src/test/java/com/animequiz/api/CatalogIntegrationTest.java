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
        assertEquals(8,quizzes.count());assertEquals(120,questions.count());assertEquals(480,options.count());
        var firstIds=questions.findAll().stream().map(item->item.getId()).sorted().toList();
        initializer.run(new DefaultApplicationArguments());
        assertEquals(firstIds,questions.findAll().stream().map(item->item.getId()).sorted().toList());
        assertEquals(480,options.count());
        for(var quiz:quizzes.findAll()) assertEquals(15,questions.countByQuizId(quiz.getId()));
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
        assertEquals(15,answers.size());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/quizzes/"+quiz.getId()+"/attempts")
            .header("Authorization",authorization).header("X-Idempotency-Key","catalog-expanded-attempt")
            .contentType("application/json").content(json.writeValueAsString(java.util.Map.of("answers",answers))))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.correctAnswers").value(15))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalQuestions").value(15))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.points").value(150));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users/"+userId+"/profile").header("Authorization",authorization))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.xp").value(150));
    }
}

