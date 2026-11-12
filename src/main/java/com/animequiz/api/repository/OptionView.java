package com.animequiz.api.repository;

/**
 * Leitura enxuta de uma alternativa: só {@code id}, {@code text} e o ID da questão.
 *
 * <p>O gabarito ({@code correct}) nunca é selecionado por esta projeção: a coluna nem sai do banco
 * quando as alternativas são carregadas para o aplicativo.
 */
public interface OptionView {

    Long getId();

    String getText();

    Long getQuestionId();
}
