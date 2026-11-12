package com.animequiz.api.service;

import com.animequiz.api.dto.QuizRequest;
import com.animequiz.api.dto.QuizResponse;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizQuestionCount;
import com.animequiz.api.repository.QuizRepository;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository,
                       ScoreRepository scoreRepository, UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        if (quizRepository.existsByTitleIgnoreCase(request.title())) {
            throw new ConflictException("Já existe um quiz cadastrado com o título: " + request.title());
        }

        Quiz quiz = new Quiz(request.title(), request.description(), request.difficulty(), request.category());
        // Um quiz recém-criado ainda não tem questões.
        return toResponse(quizRepository.save(quiz), 0L);
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizById(Long id) {
        Quiz quiz = findQuizOrThrow(id);
        return toResponse(quiz, questionRepository.countByQuizId(id));
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> getAllQuizzes(Pageable pageable) {
        return withQuestionCounts(quizRepository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> searchByTitle(String title, Pageable pageable) {
        return withQuestionCounts(quizRepository.findByTitleContainingIgnoreCase(title, pageable));
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> searchByDifficulty(Difficulty difficulty, Pageable pageable) {
        return withQuestionCounts(quizRepository.findByDifficulty(difficulty, pageable));
    }

    @Transactional
    public QuizResponse updateQuiz(Long id, QuizRequest request) {
        Quiz quiz = findQuizOrThrow(id);

        if (!quiz.getTitle().equalsIgnoreCase(request.title()) && quizRepository.existsByTitleIgnoreCase(request.title())) {
            throw new ConflictException("Já existe outro quiz cadastrado com o título: " + request.title());
        }

        quiz.setTitle(request.title());
        quiz.setDescription(request.description());
        quiz.setDifficulty(request.difficulty());
        quiz.setCategory(request.category());
        Quiz saved = quizRepository.save(quiz);
        return toResponse(saved, questionRepository.countByQuizId(saved.getId()));
    }

    @Transactional
    public void deleteQuiz(Long id) {
        if (!quizRepository.existsById(id)) {
            throw new ResourceNotFoundException("Quiz não encontrado com ID: " + id);
        }
        // Antes as FKs de questões, scores e favoritos estouravam como HTTP 500; agora é 409 com motivo.
        if (questionRepository.existsByQuizId(id)) {
            throw new ConflictException("Não é possível deletar quiz que possui questões");
        }
        if (scoreRepository.existsByQuizId(id)) {
            throw new ConflictException("Não é possível deletar quiz que possui pontuações (scores) registradas");
        }
        if (userRepository.isQuizFavorited(id)) {
            throw new ConflictException("Não é possível deletar quiz que está nos favoritos de algum usuário");
        }
        quizRepository.deleteById(id);
    }

    private Quiz findQuizOrThrow(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com ID: " + id));
    }

    /**
     * Converte uma página de quizzes calculando a quantidade de questões de todos eles com UMA consulta
     * agrupada (sem N+1): o custo não cresce com o tamanho da página.
     */
    private Page<QuizResponse> withQuestionCounts(Page<Quiz> page) {
        Map<Long, Long> counts = questionCounts(page.getContent());
        return page.map(quiz -> toResponse(quiz, counts.getOrDefault(quiz.getId(), 0L)));
    }

    private Map<Long, Long> questionCounts(List<Quiz> quizzes) {
        if (quizzes.isEmpty()) {
            return Map.of();
        }
        List<Long> quizIds = quizzes.stream().map(Quiz::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (QuizQuestionCount row : questionRepository.countQuestionsByQuizIds(quizIds)) {
            counts.put(row.getQuizId(), row.getTotal());
        }
        return counts;
    }

    private QuizResponse toResponse(Quiz quiz, long questionCount) {
        return new QuizResponse(quiz.getId(), quiz.getTitle(), quiz.getDescription(),
                quiz.getDifficulty(), quiz.getCategory(), questionCount);
    }
}