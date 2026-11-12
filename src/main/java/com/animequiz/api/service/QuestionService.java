package com.animequiz.api.service;

import com.animequiz.api.dto.QuestionRequest;
import com.animequiz.api.dto.OptionSummaryResponse;
import com.animequiz.api.dto.QuestionResponse;
import com.animequiz.api.dto.QuestionWithOptionsResponse;
import com.animequiz.api.entity.Question;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.OptionRepository;
import com.animequiz.api.repository.OptionView;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final OptionRepository optionRepository;

    public QuestionService(QuestionRepository questionRepository, QuizRepository quizRepository,
                           OptionRepository optionRepository) {
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
        this.optionRepository = optionRepository;
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
        return questionRepository.findByQuizIdOrderByIdAsc(quizId, pageable).map(this::toResponse);
    }

    /**
     * Questões de um quiz já com as alternativas (tela de resolução do aplicativo).
     *
     * <p>Custo constante, sem N+1: uma consulta para a página de questões e UMA para as alternativas de todas
     * elas ({@code in (...)}). As alternativas saem em ordem crescente de {@code id} e sem gabarito.
     */
    @Transactional(readOnly = true)
    public Page<QuestionWithOptionsResponse> getQuestionsWithOptionsByQuizId(Long quizId, Pageable pageable) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz não encontrado com ID: " + quizId);
        }
        Page<Question> page = questionRepository.findByQuizIdOrderByIdAsc(quizId, pageable);

        List<Long> questionIds = page.getContent().stream().map(Question::getId).toList();
        Map<Long, List<OptionSummaryResponse>> optionsByQuestion = new HashMap<>();
        if (!questionIds.isEmpty()) {
            for (OptionView option : optionRepository.findViewsByQuestionIds(questionIds)) {
                optionsByQuestion
                        .computeIfAbsent(option.getQuestionId(), id -> new ArrayList<>())
                        .add(new OptionSummaryResponse(option.getId(), option.getText()));
            }
        }

        return page.map(question -> new QuestionWithOptionsResponse(
                question.getId(),
                question.getQuiz().getId(),
                question.getQuiz().getTitle(),
                question.getText(),
                optionsByQuestion.getOrDefault(question.getId(), List.of())));
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
        // Antes a FK das opções estourava como HTTP 500; agora é 409 com motivo.
        if (optionRepository.existsByQuestionId(id)) {
            throw new ConflictException("Não é possível deletar questão que possui opções de resposta");
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