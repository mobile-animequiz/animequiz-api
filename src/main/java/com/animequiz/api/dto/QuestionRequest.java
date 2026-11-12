package com.animequiz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionRequest(
        @NotNull(message = "O ID do quiz é obrigatório")
        Long quizId,

        // Mesmos limites da entidade Question: antes, um texto curto passava no DTO e falhava com 500 no banco.
        @NotBlank(message = "O texto da questão é obrigatório")
        @Size(min = 5, max = 255, message = "O texto da questão deve ter entre 5 e 255 caracteres")
        String text
) {}
