package com.animequiz.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoreRequest(
        @NotNull @Min(0) Integer points,
        @NotNull Long userId
) {}