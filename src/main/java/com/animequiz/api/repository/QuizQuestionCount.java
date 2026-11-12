package com.animequiz.api.repository;

/** Linha de uma consulta agrupada: quantidade de questões de um quiz. */
public interface QuizQuestionCount {

    Long getQuizId();

    Long getTotal();
}
