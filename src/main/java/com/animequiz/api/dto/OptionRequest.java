package com.animequiz.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OptionRequest(
        @NotBlank @Size(min = 1, max = 150) String text,
        @NotNull Boolean correct,
        @NotNull Long questionId
) {}