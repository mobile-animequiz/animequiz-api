package com.animequiz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criação/atualização de uma alternativa.
 *
 * <p>{@code isCorrect} é um campo apenas de entrada (gabarito): é gravado, mas jamais devolvido
 * pela API.
 */
public record OptionRequest(
        @NotNull(message = "O ID da questão é obrigatório")
        Long questionId,

        // Mesmo limite da entidade Option: antes, um texto longo passava no DTO e falhava com 500 no banco.
        @NotBlank(message = "O texto da opção é obrigatório")
        @Size(max = 150, message = "O texto deve ter no máximo 150 caracteres")
        String text,

        @NotNull(message = "Deve indicar se a opção está correta ou não")
        Boolean isCorrect
) {}
