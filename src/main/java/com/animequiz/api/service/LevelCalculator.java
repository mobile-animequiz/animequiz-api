package com.animequiz.api.service;

import org.springframework.stereotype.Component;

/**
 * Regra única de nível e patente do perfil. É uma função pura do XP, sem acesso a banco.
 *
 * <p>Fórmula (aprovada): {@code xpParaIniciar(L) = 50 * L * (L - 1)}, de modo que o nível 1 começa em 0 XP
 * e o nível {@code L} tem largura de {@code 100 * L} XP. O nível máximo é {@value #MAX_LEVEL}.
 *
 * <p>No nível máximo {@code nextLevelXp} continua definido pela mesma fórmula ({@code xpParaIniciar(51) = 127500}),
 * nunca é nulo nem igual ao início da faixa. Quem calcula a barra de progresso,
 * {@code (xp - levelStartXp) / (nextLevelXp - levelStartXp)}, deve limitá-la a 100% (o XP continua acumulando
 * depois do teto, mas o nível não sobe).
 */
@Component
public class LevelCalculator {

    public static final int MAX_LEVEL = 50;

    /** Nível, patente e limites de XP da faixa em que um XP se encontra. */
    public record LevelProgress(int level, String rankTitle, long levelStartXp, long nextLevelXp) {}

    /** XP necessário para iniciar o nível informado (aceita 1 a {@code MAX_LEVEL + 1}). */
    public long xpToStartLevel(int level) {
        if (level < 1 || level > MAX_LEVEL + 1) {
            throw new IllegalArgumentException("Nível fora do intervalo 1.." + (MAX_LEVEL + 1) + ": " + level);
        }
        return 50L * level * (level - 1);
    }

    /** Maior nível cujo XP inicial não ultrapassa {@code xp}, limitado a {@code MAX_LEVEL}. */
    public int levelFor(long xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("O XP não pode ser negativo: " + xp);
        }
        int level = 1;
        while (level < MAX_LEVEL && xpToStartLevel(level + 1) <= xp) {
            level++;
        }
        return level;
    }

    /** Patente neutra (não amarrada a uma franquia de anime) para o nível informado. */
    public String rankTitleFor(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Nível fora do intervalo 1.." + MAX_LEVEL + ": " + level);
        }
        if (level < 5) return "Iniciante";
        if (level < 10) return "Aprendiz";
        if (level < 15) return "Explorador";
        if (level < 20) return "Veterano";
        if (level < 30) return "Mestre";
        if (level < 40) return "Grão-Mestre";
        return "Lenda";
    }

    public LevelProgress progressFor(long xp) {
        int level = levelFor(xp);
        return new LevelProgress(level, rankTitleFor(level), xpToStartLevel(level), xpToStartLevel(level + 1));
    }
}
