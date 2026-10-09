package com.animequiz.api.dto;

import com.animequiz.api.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuizRequest(
        @NotBlank @Size(min = 3, max = 100) String title,
        @NotBlank @Size(min = 5, max = 255) String description,
        @NotNull Difficulty difficulty
) {}