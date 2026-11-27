package com.animequiz.api.dto;

import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// As mensagens são as mesmas da entidade Quiz: o aplicativo exibe o campo `message` do erro diretamente ao usuário.
public record QuizRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(min = 3, max = 100, message = "O título deve ter entre 3 e 100 caracteres")
        String title,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(min = 5, max = 255, message = "A descrição deve ter entre 5 e 255 caracteres")
        String description,

        @NotNull(message = "A dificuldade é obrigatória")
        Difficulty difficulty,

        @NotNull(message = "A categoria é obrigatória")
        Category category,
        @Size(max = 1000)
        @jakarta.validation.constraints.Pattern(regexp = "https://[^\\s]+", message = "Use HTTPS for the quiz cover")
        String imageUrl,
        @Size(max = 255) String imageAltText,
        @Size(max = 100) String englishTitle,
        @Size(max = 255) String englishDescription
) {
    public QuizRequest(String title, String description, Difficulty difficulty, Category category) {
        this(title, description, difficulty, category, null, null, null, null);
    }
}
