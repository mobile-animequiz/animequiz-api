package com.animequiz.api.repository;

import com.animequiz.api.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // O quiz é lido para montar a resposta (título); o EntityGraph evita uma consulta por questão (N+1).
    @Override
    @EntityGraph(attributePaths = "quiz")
    Page<Question> findAll(Pageable pageable);

    // Ordenado por ID: o aplicativo mostra as questões na ordem recebida, que precisa ser estável entre requisições.
    @EntityGraph(attributePaths = "quiz")
    Page<Question> findByQuizIdOrderByIdAsc(Long quizId, Pageable pageable);

    /** Todas as questões do quiz, sem paginação (usado na correção de tentativas). */
    List<Question> findAllByQuizId(Long quizId);

    boolean existsByQuizId(Long quizId);

    /** Quantidade de questões de um único quiz (respostas de um quiz só: criar, ler por ID, atualizar). */
    long countByQuizId(Long quizId);

    /**
     * Quantidade de questões de vários quizzes em UMA consulta agrupada (listagens e buscas).
     * Quizzes sem questões não aparecem no resultado; quem chama assume 0 para eles.
     */
    @Query("select q.quiz.id as quizId, count(q) as total from Question q " +
            "where q.quiz.id in :quizIds group by q.quiz.id")
    List<QuizQuestionCount> countQuestionsByQuizIds(@Param("quizIds") Collection<Long> quizIds);
}
