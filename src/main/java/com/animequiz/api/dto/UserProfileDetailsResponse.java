package com.animequiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Perfil de um usuário com nível e estatísticas ({@code GET /users/{userId}/profile}).
 *
 * <p>É um superconjunto de {@link UserProfileResponse}: os campos do perfil são os mesmos e os de nível e
 * estatísticas são calculados na hora a partir das tentativas (nada disso é armazenado no usuário).
 */
public record UserProfileDetailsResponse(
        @Schema(description = "ID do perfil", example = "1") Long id,
        @Schema(description = "ID do usuário dono do perfil", example = "1") Long userId,
        @Schema(example = "Naruto Uzumaki") String username,
        @Schema(description = "Biografia (pode ser nula)", example = "Futuro Hokage") String bio,
        @Schema(description = "URL do avatar (pode ser nula)", example = "https://exemplo.com/avatar.png") String avatarUrl,

        @Schema(description = "Nível atual, de 1 a 50 (xpParaIniciar(L) = 50 * L * (L - 1))", example = "7")
        int level,
        @Schema(description = "Patente do nível: Iniciante (1-4), Aprendiz (5-9), Explorador (10-14), Veterano (15-19), "
                + "Mestre (20-29), Grão-Mestre (30-39) e Lenda (40-50)", example = "Aprendiz")
        String rankTitle,
        @Schema(description = "XP total: soma da MELHOR pontuação de cada quiz distinto. Repetir um quiz só aumenta o XP "
                + "se superar a melhor pontuação anterior", example = "2450")
        long xp,
        @Schema(description = "XP em que o nível atual começa", example = "2100")
        long levelStartXp,
        @Schema(description = "XP em que o próximo nível começa. No nível 50 vale 127500 e a barra de progresso deve ser "
                + "limitada a 100%", example = "2800")
        long nextLevelXp,
        @Schema(description = "Quizzes distintos que o usuário já respondeu ao menos uma vez (repetir não aumenta)", example = "5")
        int completedQuizzes,
        @Schema(description = "Respostas corretas somadas em TODAS as tentativas", example = "34")
        long correctAnswers,
        @Schema(description = "Respostas efetivamente enviadas em TODAS as tentativas (não é o total de perguntas "
                + "disponíveis; questões não respondidas não contam)", example = "40")
        long totalAnswers
) {}
