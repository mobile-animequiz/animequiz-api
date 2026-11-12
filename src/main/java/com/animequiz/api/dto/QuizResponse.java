package com.animequiz.api.dto;

import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;

public record QuizResponse(
        Long id,
        String title,
        String description,
        Difficulty difficulty,
        Category category,

        @Schema(description = "Quantidade de questões do quiz. Calculada a cada resposta (não é armazenada), " +
                "portanto sempre reflete o número real de questões.", example = "3")
        long questionCount
) {}
