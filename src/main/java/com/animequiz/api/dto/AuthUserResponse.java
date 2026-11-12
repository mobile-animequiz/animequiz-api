package com.animequiz.api.dto;

import java.time.LocalDate;

public record AuthUserResponse(Long id, String username, String email, LocalDate dateOfBirth) {}
