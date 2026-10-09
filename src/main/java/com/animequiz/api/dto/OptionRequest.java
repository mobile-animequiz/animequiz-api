package com.animequiz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OptionRequest(
        @NotNull(message = "O ID da questão é obrigatório")
        Long questionId,

        @NotBlank(message = "O texto da opção é obrigatório")
        String text,

        @NotNull(message = "Deve indicar se a opção está correta ou não")
        Boolean isCorrect
) {}