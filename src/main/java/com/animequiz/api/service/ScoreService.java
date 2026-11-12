package com.animequiz.api.service;

import com.animequiz.api.dto.ScoreResponse;
import com.animequiz.api.entity.Score;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.ScoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta e remoção de pontuações. A criação não é feita aqui: o score nasce da correção de uma
 * tentativa no servidor (ver {@link AttemptService}), nunca de um valor enviado pelo cliente.
 */
@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;

    public ScoreService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @Transactional(readOnly = true)
    public ScoreResponse getScoreById(Long id) {
        Score score = scoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score não encontrado com ID: " + id));
        return toResponse(score);
    }

    @Transactional(readOnly = true)
    public Page<ScoreResponse> getAllScores(Pageable pageable) {
        return scoreRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ScoreResponse> getRanking(Pageable pageable) {
        return scoreRepository.findAllByOrderByPointsDesc(pageable).map(this::toResponse);
    }

    @Transactional
    public void deleteScore(Long id) {
        if (!scoreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Score não encontrado com ID: " + id);
        }
        scoreRepository.deleteById(id);
    }

    private ScoreResponse toResponse(Score score) {
        return new ScoreResponse(
                score.getId(),
                score.getUser().getId(),
                score.getUser().getUsername(),
                score.getQuiz().getId(),
                score.getQuiz().getTitle(),
                score.getPoints(),
                score.getCorrectAnswers(),
                score.getAnsweredQuestions(),
                score.getTotalQuestions()
        );
    }
}
