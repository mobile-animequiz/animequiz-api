package com.animequiz.api.service;

import com.animequiz.api.exception.BadRequestException;

/** Shared language selection, independent of the V1/V2 transport DTOs. */
public final class ContentLanguage {
    private ContentLanguage() { }
    public static String validate(String language) {
        if (language == null || language.equals("pt-BR")) return "pt-BR";
        if (language.equals("en")) return "en";
        throw new BadRequestException("lang deve ser pt-BR ou en");
    }
    public static String text(String original, String english, String language) {
        return "en".equals(language) && english != null && !english.isBlank() ? english : original;
    }
}
