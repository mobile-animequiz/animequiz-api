package com.animequiz.api.service;

import com.animequiz.api.dto.ScoreRequest;
import com.animequiz.api.dto.ScoreResponse;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.entity.Score;
import com.animequiz.api.entity.User;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.QuizRepository;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;

    public ScoreService(ScoreRepository scoreRepository, UserRepository userRepository, QuizRepository quizRepository) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
    }

    @Transactional
    public ScoreResponse createScore(ScoreRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com ID: " + request.userId()));
        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com ID: " + request.quizId()));

        Score score = new Score(user, quiz, request.points());
        return toResponse(scoreRepository.save(score));
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
    public ScoreResponse updateScore(Long id, ScoreRequest request) {
        Score score = scoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Score não encontrado com ID: " + id));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com ID: " + request.userId()));
        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com ID: " + request.quizId()));

        score.setUser(user);
        score.setQuiz(quiz);
        score.setPoints(request.points());

        return toResponse(scoreRepository.save(score));
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
                score.getPoints()
        );
    }
}