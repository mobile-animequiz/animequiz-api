package com.animequiz.api.dto;

import com.animequiz.api.entity.Difficulty;

public record QuizResponse(
        Long id,
        String title,
        String description,
        Difficulty difficulty
) {}