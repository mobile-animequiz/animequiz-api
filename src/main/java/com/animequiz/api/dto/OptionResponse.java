package com.animequiz.api.dto;

/**
 * Representação pública de uma alternativa de resposta.
 *
 * <p>Não contém o gabarito: a correção é feita exclusivamente no servidor
 * (ver {@code POST /quizzes/{quizId}/attempts}).
 */
public record OptionResponse(
        Long id,
        Long questionId,
        String text
) {}
