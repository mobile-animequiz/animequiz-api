package com.animequiz.api.repository;

import com.animequiz.api.entity.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {

    // A questão é lida para montar a resposta (questionId); o EntityGraph evita uma consulta por opção (N+1).
    @Override
    @EntityGraph(attributePaths = "question")
    Page<Option> findAll(Pageable pageable);

    // Ordenado por ID: o aplicativo rotula as alternativas (A, B, C...) pela ordem recebida, que precisa ser estável.
    @EntityGraph(attributePaths = "question")
    Page<Option> findByQuestionIdOrderByIdAsc(Long questionId, Pageable pageable);

    boolean existsByQuestionId(Long questionId);

    /**
     * Alternativas de várias questões em UMA consulta, ordenadas por ID e SEM o gabarito
     * (a projeção não seleciona a coluna {@code correct}). Usada por {@code GET /quizzes/{quizId}/questions}.
     */
    @Query("select o.id as id, o.text as text, o.question.id as questionId from Option o " +
            "where o.question.id in :questionIds order by o.id asc")
    List<OptionView> findViewsByQuestionIds(@Param("questionIds") Collection<Long> questionIds);
}
