package com.animequiz.api.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, AuthUserResponse user) {}
