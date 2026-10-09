package com.animequiz.api.dto;

public record UserProfileResponse(
        Long id,
        Long userId,
        String username,
        String bio,
        String avatarUrl
) {}