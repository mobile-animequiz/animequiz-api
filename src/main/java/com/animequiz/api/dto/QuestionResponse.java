package com.animequiz.api.dto;

public record QuestionResponse(
        Long id,
        String statement,
        Long quizId
) {}