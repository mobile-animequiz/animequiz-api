package com.animequiz.api;

import com.animequiz.api.dto.AnswerRequest;
import com.animequiz.api.dto.AttemptRequest;
import com.animequiz.api.dto.AttemptResponse;
import com.animequiz.api.dto.OptionResponse;
import com.animequiz.api.dto.OptionSummaryResponse;
import com.animequiz.api.dto.QuestionResponse;
import com.animequiz.api.dto.QuestionWithOptionsResponse;
import com.animequiz.api.dto.QuizRequest;
import com.animequiz.api.dto.QuizResponse;
import com.animequiz.api.dto.ScoreResponse;
import com.animequiz.api.dto.UserProfileDetailsResponse;
import com.animequiz.api.dto.UserProfileResponse;
import com.animequiz.api.dto.UserResponse;
import com.animequiz.api.entity.Category;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Trava, no nível do tipo, o que o contrato permite sair e entrar pela API.
 * Se alguém acrescentar um campo de gabarito ou de pontuação enviada pelo cliente, estes testes quebram.
 */
class DtoContractTest {

    private static List<String> components(Class<?> record) {
        return Arrays.stream(record.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    @Test
    void optionResponseExposesOnlyIdQuestionIdAndText() {
        assertThat(components(OptionResponse.class)).containsExactly("id", "questionId", "text");
    }

    @Test
    void attemptResponseHasOnlyAggregateFields() {
        assertThat(components(AttemptResponse.class)).containsExactly(
                "scoreId", "userId", "username", "quizId", "quizTitle", "difficulty",
                "totalQuestions", "answeredQuestions", "correctAnswers", "pointsPerCorrectAnswer", "points");
    }

    @Test
    void attemptRequestCarriesOnlyUserAndChoices() {
        assertThat(components(AttemptRequest.class)).containsExactly("answers");
        assertThat(components(AnswerRequest.class)).containsExactly("questionId", "optionId");
    }

    @Test
    void noResponseDtoHasAnAnswerKeyField() {
        List<Class<?>> responses = List.of(OptionResponse.class, QuestionResponse.class, QuizResponse.class,
                ScoreResponse.class, UserResponse.class, UserProfileResponse.class, AttemptResponse.class,
                QuestionWithOptionsResponse.class, OptionSummaryResponse.class, UserProfileDetailsResponse.class);

        for (Class<?> response : responses) {
            assertThat(components(response))
                    .as("DTO %s", response.getSimpleName())
                    .noneMatch(name -> name.equalsIgnoreCase("isCorrect") || name.equalsIgnoreCase("correct"));
        }
    }

    @Test
    void quizResponseCarriesCategoryAndDerivedQuestionCount() {
        assertThat(components(QuizResponse.class))
                .containsExactly("id", "title", "description", "difficulty", "category", "questionCount");
    }

    @Test
    void quizRequestCarriesCategoryButNeverTheQuestionCount() {
        assertThat(components(QuizRequest.class)).containsExactly("title", "description", "difficulty", "category");
    }

    @Test
    void questionsWithOptionsEmbedOnlyIdAndTextOfEachOption() {
        assertThat(components(QuestionWithOptionsResponse.class))
                .containsExactly("id", "quizId", "quizTitle", "text", "options");
        assertThat(components(OptionSummaryResponse.class)).containsExactly("id", "text");
        assertThat(QuestionWithOptionsResponse.class.getRecordComponents()[4].getGenericType().getTypeName())
                .contains(OptionSummaryResponse.class.getName());
    }

    @Test
    void scoreResponseAddsTheAttemptNumbersAtTheEnd() {
        assertThat(components(ScoreResponse.class)).containsExactly("id", "userId", "username", "quizId", "quizTitle",
                "points", "correctAnswers", "answeredQuestions", "totalQuestions");
    }

    @Test
    void theLegacyProfileResponseHasNoStats() {
        assertThat(components(UserProfileResponse.class)).containsExactly("id", "userId", "username", "bio", "avatarUrl");
    }

    @Test
    void theDetailedProfileHasTheProfileFieldsAndTheStatsTheAppUses() {
        assertThat(components(UserProfileDetailsResponse.class)).containsExactly("id", "userId", "username", "bio",
                "avatarUrl", "level", "rankTitle", "xp", "levelStartXp", "nextLevelXp", "completedQuizzes",
                "correctAnswers", "totalAnswers");
    }

    @Test
    void categoryHasExactlyTheFiveValuesTheAppExpects() {
        assertThat(Arrays.stream(Category.values()).map(Enum::name).toList())
                .containsExactly("SHONEN", "SEINEN", "SHOJO", "JOSEI", "KODOMO");
    }

    @Test
    void thereIsNoRequestDtoThroughWhichTheClientCouldSendAScore() {
        assertThatThrownBy(() -> Class.forName("com.animequiz.api.dto.ScoreRequest"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
