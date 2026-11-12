package com.animequiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ScoreResponse(
        Long id,
        Long userId,
        String username,
        Long quizId,
        String quizTitle,
        Integer points,
        @Schema(description = "Respostas corretas nesta tentativa", example = "2")
        Integer correctAnswers,
        @Schema(description = "Respostas efetivamente enviadas nesta tentativa (a tentativa pode ser parcial)", example = "3")
        Integer answeredQuestions,
        @Schema(description = "Total de questões do quiz no momento da tentativa", example = "3")
        Integer totalQuestions
) {}
