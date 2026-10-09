package com.animequiz.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoreRequest(
        @NotNull(message = "O ID do utilizador é obrigatório")
        Long userId,

        @NotNull(message = "O ID do quiz é obrigatório")
        Long quizId,

        @NotNull(message = "A pontuação é obrigatória")
        @Min(value = 0, message = "A pontuação não pode ser negativa")
        Integer points
) {}