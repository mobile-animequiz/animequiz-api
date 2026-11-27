package com.animequiz.api.dto;

import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;

/** V1 remains independent of these presentation fields. */
public record QuizV2Response(Long id, String title, String description, Difficulty difficulty,
        Category category, long questionCount, String imageUrl, String imageAltText,
        int timeLimitSeconds, String contentLanguage) { }
