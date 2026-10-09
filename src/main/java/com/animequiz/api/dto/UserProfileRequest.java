package com.animequiz.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotNull(message = "O ID do utilizador é obrigatório")
        Long userId,

        @Size(max = 255, message = "A bio deve ter no máximo 255 caracteres")
        String bio,

        @Size(max = 500, message = "O URL do avatar é demasiado longo")
        String avatarUrl
) {}