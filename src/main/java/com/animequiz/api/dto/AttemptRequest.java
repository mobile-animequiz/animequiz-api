package com.animequiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Respostas enviadas pelo jogador. A identidade vem exclusivamente do JWT autenticado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AttemptRequest(
        @Schema(description = "Alternativa escolhida para cada questão respondida. Questões não enviadas contam como erradas.")
        @NotEmpty(message = "É obrigatório informar ao menos uma resposta")
        List<@NotNull(message = "Resposta nula não é permitida") @Valid AnswerRequest> answers
) {}
