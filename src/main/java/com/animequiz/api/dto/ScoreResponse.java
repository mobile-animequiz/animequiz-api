package com.animequiz.api.dto;

public record ScoreResponse(
        Long id,
        Integer points,
        Long userId,
        String username
) {}