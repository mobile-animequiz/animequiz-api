package com.animequiz.api.repository;

import com.animequiz.api.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Page<Question> findByQuizId(Long quizId, Pageable pageable);
}