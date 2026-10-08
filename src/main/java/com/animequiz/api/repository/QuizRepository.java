package com.animequiz.api.repository;

import com.animequiz.api.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // Busca quizzes cujo título contenha o texto informado (ignorando maiúsculas/minúsculas)
    List<Quiz> findByTitleContainingIgnoreCase(String title);
}