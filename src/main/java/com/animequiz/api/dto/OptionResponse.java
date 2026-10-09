package com.animequiz.api.dto;

public record OptionResponse(
        Long id,
        String text,
        Boolean correct,
        Long questionId
) {}