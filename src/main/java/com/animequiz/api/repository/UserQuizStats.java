package com.animequiz.api.repository;

/**
 * Resumo das tentativas de um usuário em UM quiz: a melhor pontuação (única que conta para o XP) e a
 * soma de acertos e de respostas enviadas em todas as tentativas (histórico).
 */
public interface UserQuizStats {

    Long getQuizId();

    Integer getBestPoints();

    Long getCorrectTotal();

    Long getAnsweredTotal();
}
