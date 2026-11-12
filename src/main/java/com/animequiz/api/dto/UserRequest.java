package com.animequiz.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// As mensagens são as mesmas da entidade User: o aplicativo exibe o campo `message` do erro diretamente ao usuário.
public record UserRequest(
        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre 3 e 50 caracteres")
        String username,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email
) {}
