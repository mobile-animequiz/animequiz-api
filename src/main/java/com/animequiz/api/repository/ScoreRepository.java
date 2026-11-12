package com.animequiz.api.repository;

import com.animequiz.api.entity.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    boolean existsByUserId(Long userId);

    boolean existsByQuizId(Long quizId);

    // Usuário e quiz são lidos para montar a resposta; o EntityGraph evita 2 consultas por linha (N+1).
    @Override
    @EntityGraph(attributePaths = {"user", "quiz"})
    Page<Score> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "quiz"})
    Page<Score> findByPointsGreaterThanEqual(Integer minPoints, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "quiz"})
    Page<Score> findAllByOrderByPointsDesc(Pageable pageable);

    /**
     * Uma linha por quiz distinto que o usuário já respondeu, numa única consulta: melhor pontuação
     * (base do XP) e somas de acertos e respostas enviadas de todas as tentativas (histórico).
     * A quantidade de linhas é o número de quizzes concluídos.
     */
    @Query("select s.quiz.id as quizId, max(s.points) as bestPoints, " +
            "sum(s.correctAnswers) as correctTotal, sum(s.answeredQuestions) as answeredTotal " +
            "from Score s where s.user.id = :userId group by s.quiz.id")
    List<UserQuizStats> findQuizStatsByUserId(@Param("userId") Long userId);
}
