package com.animequiz.api.entity;

/**
 * Dificuldade de um quiz. O peso multiplica os pontos de cada acerto
 * (regra de pontuação: 10 pontos por acerto x peso da dificuldade).
 *
 * <p>Os nomes das constantes fazem parte do contrato JSON/BD e não podem mudar.
 */
public enum Difficulty {
    FACIL(1),
    MEDIO(2),
    DIFICIL(3);

    private final int weight;

    Difficulty(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
