package com.animequiz.api.dto;

public record ScoreResponse(
        Long id,
        Long userId,
        String username,
        Long quizId,
        String quizTitle,
        Integer points
) {}