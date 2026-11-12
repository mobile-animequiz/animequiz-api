package com.animequiz.api.service;

import com.animequiz.api.dto.AnswerRequest;
import com.animequiz.api.dto.AttemptRequest;
import com.animequiz.api.dto.AttemptResponse;
import com.animequiz.api.entity.Option;
import com.animequiz.api.entity.Question;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.entity.Score;
import com.animequiz.api.entity.User;
import com.animequiz.api.exception.BadRequestException;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.OptionRepository;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizRepository;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Corrige uma tentativa de quiz no servidor e registra o score resultante.
 * O cliente envia só as escolhas; o gabarito e a pontuação nunca passam pelo aplicativo.
 */
@Service
public class AttemptService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final ScoreRepository scoreRepository;
    private final ScoreCalculator scoreCalculator;

    public AttemptService(QuizRepository quizRepository,
                          UserRepository userRepository,
                          QuestionRepository questionRepository,
                          OptionRepository optionRepository,
                          ScoreRepository scoreRepository,
                          ScoreCalculator scoreCalculator) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.scoreRepository = scoreRepository;
        this.scoreCalculator = scoreCalculator;
    }

    @Transactional
    public AttemptResponse submit(Long quizId, Long authenticatedUserId, AttemptRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz não encontrado com ID: " + quizId));
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));

        List<Question> questions = questionRepository.findAllByQuizId(quizId);
        if (questions.isEmpty()) {
            throw new ConflictException("O quiz " + quizId + " não possui questões e não pode ser respondido");
        }
        Set<Long> quizQuestionIds = new HashSet<>();
        questions.forEach(question -> quizQuestionIds.add(question.getId()));

        // questionId -> optionId escolhido, validando pertencimento ao quiz e ausência de duplicatas
        Map<Long, Long> chosenByQuestion = new LinkedHashMap<>();
        for (AnswerRequest answer : request.answers()) {
            if (!quizQuestionIds.contains(answer.questionId())) {
                throw new BadRequestException(
                        "A questão " + answer.questionId() + " não pertence ao quiz " + quizId);
            }
            if (chosenByQuestion.putIfAbsent(answer.questionId(), answer.optionId()) != null) {
                throw new BadRequestException(
                        "Mais de uma resposta enviada para a questão " + answer.questionId());
            }
        }

        Map<Long, Option> optionsById = optionRepository.findAllById(chosenByQuestion.values()).stream()
                .collect(Collectors.toMap(Option::getId, Function.identity()));

        int correctAnswers = 0;
        for (Map.Entry<Long, Long> chosen : chosenByQuestion.entrySet()) {
            Option option = optionsById.get(chosen.getValue());
            if (option == null || !option.getQuestion().getId().equals(chosen.getKey())) {
                throw new BadRequestException(
                        "A opção " + chosen.getValue() + " não existe ou não pertence à questão " + chosen.getKey());
            }
            if (Boolean.TRUE.equals(option.getCorrect())) {
                correctAnswers++;
            }
        }

        int points = scoreCalculator.calculate(correctAnswers, quiz.getDifficulty());
        Score score = scoreRepository.save(new Score(user, quiz, points, correctAnswers, chosenByQuestion.size(), questions.size()));

        return new AttemptResponse(
                score.getId(),
                user.getId(),
                user.getUsername(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDifficulty(),
                questions.size(),
                chosenByQuestion.size(),
                correctAnswers,
                scoreCalculator.pointsPerCorrectAnswer(quiz.getDifficulty()),
                points
        );
    }
}
