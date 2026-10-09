package com.animequiz.api.dto;

public record OptionResponse(
        Long id,
        Long questionId,
        String text,
        Boolean isCorrect
) {}