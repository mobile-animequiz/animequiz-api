package com.animequiz.api.dto;

import java.time.LocalDateTime;

public record APIError(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {}