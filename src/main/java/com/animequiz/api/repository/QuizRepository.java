package com.animequiz.api.repository;

import com.animequiz.api.entity.Quiz;
import com.animequiz.api.entity.Difficulty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // Adiciona esta linha para verificar se já existe um quiz com este título
    boolean existsByTitleIgnoreCase(String title);

    Page<Quiz> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Quiz> findByDifficulty(Difficulty difficulty, Pageable pageable);
}