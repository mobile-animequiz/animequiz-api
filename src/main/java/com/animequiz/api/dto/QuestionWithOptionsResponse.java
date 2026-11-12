package com.animequiz.api.dto;

import java.util.List;

/**
 * Questão de um quiz com as suas alternativas, no formato usado pela tela de resolução do aplicativo.
 *
 * <p>As alternativas vêm em ordem crescente de {@code id} e <b>sem gabarito</b>.
 */
public record QuestionWithOptionsResponse(
        Long id,
        Long quizId,
        String quizTitle,
        String text,
        List<OptionSummaryResponse> options
) {}
