package com.animequiz.api.dto;
import java.util.List;
import org.springframework.data.domain.Page;
public record FavoritePage(List<QuizResponse> content,long totalElements,int totalPages,int number,int size) {
    public static FavoritePage of(Page<QuizResponse> page) {
        return new FavoritePage(page.getContent(),page.getTotalElements(),page.getTotalPages(),page.getNumber(),page.getSize());
    }
}

