package com.animequiz.api.entity;

/**
 * Categoria (demografia) de um quiz, usada pelo aplicativo nos filtros e na etiqueta do card da Home.
 *
 * <p>Os nomes das constantes fazem parte do contrato JSON/BD e não podem mudar: o aplicativo converte
 * {@code SHONEN} em {@code shonen} e exibe "Shonen". São gravados como texto ({@code EnumType.STRING}).
 */
public enum Category {
    SHONEN,
    SEINEN,
    SHOJO,
    JOSEI,
    KODOMO
}
