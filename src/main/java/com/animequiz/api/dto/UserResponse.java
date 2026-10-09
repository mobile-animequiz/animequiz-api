package com.animequiz.api.dto;

public record UserResponse(
        Long id,
        String username,
        String email
) {}