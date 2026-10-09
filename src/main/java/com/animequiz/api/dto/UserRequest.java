package com.animequiz.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email
        String email
) {}