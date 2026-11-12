package com.animequiz.api.dto;

import com.animequiz.api.entity.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de uma tentativa, já corrigida e pontuada pelo servidor.
 *
 * <p>Propositalmente agregado: informa quantos acertos houve, mas não quais questões foram
 * acertadas nem qual era a alternativa correta (o gabarito não sai do servidor).
 */
public record AttemptResponse(
        @Schema(description = "ID do score registrado para esta tentativa", example = "3")
        Long scoreId,
        @Schema(example = "1") Long userId,
        @Schema(example = "Naruto Uzumaki") String username,
        @Schema(example = "1") Long quizId,
        @Schema(example = "Naruto Shippuden Quiz") String quizTitle,
        @Schema(example = "FACIL") Difficulty difficulty,
        @Schema(description = "Total de questões do quiz", example = "3")
        int totalQuestions,
        @Schema(description = "Questões respondidas nesta tentativa", example = "3")
        int answeredQuestions,
        @Schema(description = "Respostas corretas", example = "2")
        int correctAnswers,
        @Schema(description = "Pontos por acerto: 10 x peso da dificuldade (FACIL=1, MEDIO=2, DIFICIL=3)", example = "10")
        int pointsPerCorrectAnswer,
        @Schema(description = "Pontuação total = acertos x pontos por acerto", example = "20")
        int points
) {}
