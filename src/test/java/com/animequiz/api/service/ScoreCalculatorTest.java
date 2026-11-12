package com.animequiz.api.service;

import com.animequiz.api.entity.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    @ParameterizedTest(name = "{0} acerto(s) em quiz {1} = {2} pontos")
    @CsvSource({
            "0, FACIL, 0",
            "1, FACIL, 10",
            "3, FACIL, 30",
            "0, MEDIO, 0",
            "1, MEDIO, 20",
            "2, MEDIO, 40",
            "0, DIFICIL, 0",
            "1, DIFICIL, 30",
            "5, DIFICIL, 150"
    })
    void calculatesTenPointsTimesDifficultyWeight(int correctAnswers, Difficulty difficulty, int expectedPoints) {
        assertThat(calculator.calculate(correctAnswers, difficulty)).isEqualTo(expectedPoints);
    }

    @Test
    void weightsAreOneTwoAndThree() {
        assertThat(Difficulty.FACIL.getWeight()).isEqualTo(1);
        assertThat(Difficulty.MEDIO.getWeight()).isEqualTo(2);
        assertThat(Difficulty.DIFICIL.getWeight()).isEqualTo(3);
    }

    @Test
    void pointsPerCorrectAnswerFollowsTheWeight() {
        assertThat(calculator.pointsPerCorrectAnswer(Difficulty.FACIL)).isEqualTo(10);
        assertThat(calculator.pointsPerCorrectAnswer(Difficulty.MEDIO)).isEqualTo(20);
        assertThat(calculator.pointsPerCorrectAnswer(Difficulty.DIFICIL)).isEqualTo(30);
    }

    @Test
    void rejectsNegativeCorrectAnswers() {
        assertThatThrownBy(() -> calculator.calculate(-1, Difficulty.FACIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingDifficulty() {
        assertThatThrownBy(() -> calculator.calculate(1, null))
                .isInstanceOf(NullPointerException.class);
    }
}
