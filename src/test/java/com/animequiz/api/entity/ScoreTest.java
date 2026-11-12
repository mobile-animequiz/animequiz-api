package com.animequiz.api.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariante da tentativa: 0 <= acertos <= respondidas <= total de questões, com total >= 1. */
class ScoreTest {

    private final User user = new User("naruto", "naruto@konoha.com");
    private final Quiz quiz = new Quiz("Quiz de teste", "Descrição do quiz", Difficulty.FACIL, Category.SHONEN);

    @Test
    void aFullAttemptKeepsItsExactNumbers() {
        Score score = new Score(user, quiz, 30, 3, 3, 3);

        assertThat(score.getPoints()).isEqualTo(30);
        assertThat(score.getCorrectAnswers()).isEqualTo(3);
        assertThat(score.getAnsweredQuestions()).isEqualTo(3);
        assertThat(score.getTotalQuestions()).isEqualTo(3);
        assertThat(score.getUser()).isSameAs(user);
        assertThat(score.getQuiz()).isSameAs(quiz);
    }

    @Test
    void aPartialAttemptKeepsAnsweredAndTotalSeparate() {
        Score score = new Score(user, quiz, 10, 1, 2, 3);

        assertThat(score.getCorrectAnswers()).isEqualTo(1);
        assertThat(score.getAnsweredQuestions()).isEqualTo(2);
        assertThat(score.getTotalQuestions()).isEqualTo(3);
    }

    @Test
    void anAttemptWithNoAnswersIsValid() {
        Score score = new Score(user, quiz, 0, 0, 0, 3);

        assertThat(score.getAnsweredQuestions()).isZero();
        assertThat(score.getTotalQuestions()).isEqualTo(3);
    }

    @Test
    void inconsistentNumbersAreRejected() {
        assertThatThrownBy(() -> new Score(user, quiz, 0, 0, 0, 0)).as("total zero")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Score(user, quiz, 0, -1, 0, 3)).as("acertos negativos")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Score(user, quiz, 0, 2, 1, 3)).as("mais acertos que respostas")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Score(user, quiz, 0, 1, 4, 3)).as("mais respostas que questões")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
