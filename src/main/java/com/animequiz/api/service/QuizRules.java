package com.animequiz.api.service;

import com.animequiz.api.entity.Difficulty;

/** One difficulty policy; the app receives these values instead of inventing its own. */
public final class QuizRules {
    private QuizRules() { }
    public static int seconds(Difficulty difficulty) {
        return switch (difficulty) { case FACIL -> 30; case MEDIO -> 20; case DIFICIL -> 12; };
    }
}
