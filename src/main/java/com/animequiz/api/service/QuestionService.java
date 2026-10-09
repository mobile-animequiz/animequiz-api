package com.animequiz.api.service;

import com.animequiz.api.dto.QuestionRequest;
import com.animequiz.api.dto.QuestionResponse;
import com.animequiz.api.entity.Question;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;

    public QuestionService(QuestionRepository questionRepository, QuizRepository quizRepository) {
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
    }

    @Transactional
    public QuestionResponse createQuestion(QuestionRequest request) {
        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com ID: " + request.quizId()));

        Question question = new Question(quiz, request.text());
        return toResponse(questionRepository.save(question));
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questão não encontrada com ID: " + id));
        return toResponse(question);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> getAllQuestions(Pageable pageable) {
        return questionRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestionsByQuizId(Long quizId, Pageable pageable) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz não encontrado com ID: " + quizId);
        }
        return questionRepository.findByQuizId(quizId, pageable).map(this::toResponse);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long id, QuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Questão não encontrada com ID: " + id));

        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com ID: " + request.quizId()));

        question.setQuiz(quiz);
        question.setText(request.text());

        return toResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Questão não encontrada com ID: " + id);
        }
        questionRepository.deleteById(id);
    }

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuiz().getId(),
                question.getQuiz().getTitle(),
                question.getText()
        );
    }
}