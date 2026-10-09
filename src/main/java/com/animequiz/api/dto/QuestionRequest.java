package com.animequiz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionRequest(
        @NotNull(message = "O ID do quiz é obrigatório")
        Long quizId,

        @NotBlank(message = "O texto da questão é obrigatório")
        String text
) {}