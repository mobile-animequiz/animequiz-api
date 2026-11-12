package com.animequiz.api.dto;

/**
 * Alternativa embutida em {@link QuestionWithOptionsResponse}.
 *
 * <p>Não contém o gabarito: a correção é feita exclusivamente no servidor
 * (ver {@code POST /quizzes/{quizId}/attempts}).
 */
public record OptionSummaryResponse(
        Long id,
        String text
) {}
