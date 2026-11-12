package com.animequiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Resposta do usuário a uma questão: a alternativa escolhida. */
public record AnswerRequest(
        @Schema(description = "ID da questão respondida (deve pertencer ao quiz)", example = "1")
        @NotNull(message = "O ID da questão é obrigatório")
        Long questionId,

        @Schema(description = "ID da alternativa escolhida (deve pertencer à questão)", example = "1")
        @NotNull(message = "O ID da opção escolhida é obrigatório")
        Long optionId
) {}
