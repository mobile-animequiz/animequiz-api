package com.animequiz.api.service;

import com.animequiz.api.entity.Difficulty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Regra única de pontuação do AnimeQuiz, calculada sempre no servidor.
 *
 * <p>pontos = acertos x {@value #POINTS_PER_CORRECT_ANSWER} x peso da dificuldade
 * (FACIL = 1, MEDIO = 2, DIFICIL = 3).
 */
@Component
public class ScoreCalculator {

    public static final int POINTS_PER_CORRECT_ANSWER = 10;

    /** Pontos concedidos por cada acerto em um quiz da dificuldade informada. */
    public int pointsPerCorrectAnswer(Difficulty difficulty) {
        Objects.requireNonNull(difficulty, "A dificuldade é obrigatória");
        return POINTS_PER_CORRECT_ANSWER * difficulty.getWeight();
    }

    /** Pontuação total de uma tentativa com {@code correctAnswers} acertos. */
    public int calculate(int correctAnswers, Difficulty difficulty) {
        if (correctAnswers < 0) {
            throw new IllegalArgumentException("O número de acertos não pode ser negativo");
        }
        return Math.multiplyExact(correctAnswers, pointsPerCorrectAnswer(difficulty));
    }
}
