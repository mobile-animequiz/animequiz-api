package com.animequiz.api.service;

import com.animequiz.api.dto.QuizRequest;
import com.animequiz.api.dto.QuizResponse;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.QuizRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final QuizRepository quizRepository;

    public QuizService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        // VALIDAÇÃO DE CONFLITO: Impede a criação de quizzes com o mesmo título
        if (quizRepository.existsByTitleIgnoreCase(request.title())) {
            throw new ConflictException("Já existe um quiz cadastrado com o título: " + request.title());
        }

        Quiz quiz = new Quiz(request.title(), request.description(), request.difficulty());
        return toResponse(quizRepository.save(quiz));
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizById(Long id) {
        return toResponse(findQuizOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> getAllQuizzes(Pageable pageable) {
        return quizRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> searchByTitle(String title, Pageable pageable) {
        return quizRepository.findByTitleContainingIgnoreCase(title, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> searchByDifficulty(Difficulty difficulty, Pageable pageable) {
        return quizRepository.findByDifficulty(difficulty, pageable).map(this::toResponse);
    }

    @Transactional
    public QuizResponse updateQuiz(Long id, QuizRequest request) {
        Quiz quiz = findQuizOrThrow(id);

        // Opcional: Se quiseres validar duplicado também no update, podes colocar aqui
        if (!quiz.getTitle().equalsIgnoreCase(request.title()) && quizRepository.existsByTitleIgnoreCase(request.title())) {
            throw new ConflictException("Já existe outro quiz cadastrado com o título: " + request.title());
        }

        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setDifficulty(request.difficulty());
        return toResponse(quizRepository.save(quiz));
    }

    @Transactional
    public void deleteQuiz(Long id) {
        if (!quizRepository.existsById(id)) {
            throw new ResourceNotFoundException("Quiz não encontrado com ID: " + id);
        }
        quizRepository.deleteById(id);
    }

    private Quiz findQuizOrThrow(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com ID: " + id));
    }

    private QuizResponse toResponse(Quiz quiz) {
        return new QuizResponse(quiz.getId(), quiz.getTitle(), quiz.getDescription(), quiz.getDifficulty());
    }
}