package com.animequiz.api.dto;

public record QuestionResponse(
        Long id,
        Long quizId,
        String quizTitle,
        String text
) {}