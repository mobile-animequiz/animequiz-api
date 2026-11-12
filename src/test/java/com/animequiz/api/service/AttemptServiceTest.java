package com.animequiz.api.service;

import com.animequiz.api.dto.AnswerRequest;
import com.animequiz.api.dto.AttemptRequest;
import com.animequiz.api.dto.AttemptResponse;
import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Cenário: quiz 1 com 3 questões (ids 1, 2 e 3).
 * Opções: 1 (correta) e 2 na questão 1; 3 (correta) e 4 na questão 2; 5 (correta) na questão 3.
 */
@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private UserRepository userRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private ScoreRepository scoreRepository;

    private AttemptService service;
    private User user;
    private Quiz quiz;
    private List<Question> questions;
    private List<Option> options;

    @BeforeEach
    void setUp() {
        service = new AttemptService(quizRepository, userRepository, questionRepository,
                optionRepository, scoreRepository, new ScoreCalculator());

        user = new User("naruto", "naruto@konoha.com");
        user.setId(1L);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        givenQuiz(Difficulty.FACIL);

        lenient().when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> {
            Score score = invocation.getArgument(0);
            score.setId(99L);
            return score;
        });
    }

    private void givenQuiz(Difficulty difficulty) {
        quiz = new Quiz("Quiz de teste", "Descrição do quiz", difficulty, Category.SHONEN);
        quiz.setId(1L);

        Question q1 = question(1L);
        Question q2 = question(2L);
        Question q3 = question(3L);
        questions = List.of(q1, q2, q3);

        options = new ArrayList<>();
        options.add(option(1L, q1, true));
        options.add(option(2L, q1, false));
        options.add(option(3L, q2, true));
        options.add(option(4L, q2, false));
        options.add(option(5L, q3, true));

        lenient().when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        lenient().when(questionRepository.findAllByQuizId(1L)).thenReturn(questions);
        lenient().when(optionRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> requested = invocation.getArgument(0);
            Set<Long> ids = new HashSet<>();
            requested.forEach(ids::add);
            return options.stream().filter(option -> ids.contains(option.getId())).toList();
        });
    }

    private Question question(Long id) {
        Question question = new Question(quiz, "Questão " + id + " de teste");
        question.setId(id);
        return question;
    }

    private Option option(Long id, Question question, boolean correct) {
        Option option = new Option(question, "Opção " + id, correct);
        option.setId(id);
        return option;
    }

    private static AttemptRequest attempt(AnswerRequest... answers) {
        return new AttemptRequest(List.of(answers));
    }

    private static AnswerRequest answer(long questionId, long optionId) {
        return new AnswerRequest(questionId, optionId);
    }

    @Test
    void allCorrectInEasyQuizGivesTenPointsPerAnswer() {
        AttemptResponse response = service.submit(1L, 1L, attempt(answer(1, 1), answer(2, 3), answer(3, 5)));

        assertThat(response.correctAnswers()).isEqualTo(3);
        assertThat(response.totalQuestions()).isEqualTo(3);
        assertThat(response.answeredQuestions()).isEqualTo(3);
        assertThat(response.pointsPerCorrectAnswer()).isEqualTo(10);
        assertThat(response.points()).isEqualTo(30);
        assertThat(response.scoreId()).isEqualTo(99L);
        assertThat(response.difficulty()).isEqualTo(Difficulty.FACIL);
    }

    @Test
    void mediumQuizDoublesThePointsPerAnswer() {
        givenQuiz(Difficulty.MEDIO);

        // acerta as questões 1 e 2, erra a 3 (não há opção errada na 3, então a deixa sem resposta)
        AttemptResponse response = service.submit(1L, 1L, attempt(answer(1, 1), answer(2, 3)));

        assertThat(response.correctAnswers()).isEqualTo(2);
        assertThat(response.pointsPerCorrectAnswer()).isEqualTo(20);
        assertThat(response.points()).isEqualTo(40);
    }

    @Test
    void hardQuizTriplesThePointsPerAnswer() {
        givenQuiz(Difficulty.DIFICIL);

        // acerta a questão 1 e erra a 2
        AttemptResponse response = service.submit(1L, 1L, attempt(answer(1, 1), answer(2, 4)));

        assertThat(response.correctAnswers()).isEqualTo(1);
        assertThat(response.pointsPerCorrectAnswer()).isEqualTo(30);
        assertThat(response.points()).isEqualTo(30);
    }

    @Test
    void unansweredQuestionsCountAsWrong() {
        AttemptResponse response = service.submit(1L, 1L, attempt(answer(1, 1)));

        assertThat(response.totalQuestions()).isEqualTo(3);
        assertThat(response.answeredQuestions()).isEqualTo(1);
        assertThat(response.correctAnswers()).isEqualTo(1);
        assertThat(response.points()).isEqualTo(10);
    }

    @Test
    void allWrongGivesZeroPoints() {
        AttemptResponse response = service.submit(1L, 1L, attempt(answer(1, 2), answer(2, 4)));

        assertThat(response.correctAnswers()).isZero();
        assertThat(response.points()).isZero();
    }

    @Test
    void persistsTheServerComputedScore() {
        service.submit(1L, 1L, attempt(answer(1, 1), answer(2, 3)));

        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreRepository).save(captor.capture());
        Score saved = captor.getValue();
        assertThat(saved.getPoints()).isEqualTo(20);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getQuiz()).isSameAs(quiz);
    }

    @Test
    void aFullAttemptStoresCorrectAnsweredAndTotalQuestions() {
        service.submit(1L, 1L, attempt(answer(1, 1), answer(2, 3), answer(3, 5)));

        Score saved = savedScore();
        assertThat(saved.getCorrectAnswers()).isEqualTo(3);
        assertThat(saved.getAnsweredQuestions()).isEqualTo(3);
        assertThat(saved.getTotalQuestions()).isEqualTo(3);
        assertThat(saved.getPoints()).isEqualTo(30);
    }

    @Test
    void aPartialAttemptStoresOnlyTheAnswersThatWereSentAndKeepsTheQuizSize() {
        service.submit(1L, 1L, attempt(answer(1, 1)));

        Score saved = savedScore();
        assertThat(saved.getCorrectAnswers()).isEqualTo(1);
        assertThat(saved.getAnsweredQuestions()).isEqualTo(1);
        assertThat(saved.getTotalQuestions()).isEqualTo(3);
    }

    @Test
    void wrongAnswersCountAsAnsweredButNotAsCorrect() {
        service.submit(1L, 1L, attempt(answer(1, 1), answer(2, 4)));

        Score saved = savedScore();
        assertThat(saved.getCorrectAnswers()).isEqualTo(1);
        assertThat(saved.getAnsweredQuestions()).isEqualTo(2);
        assertThat(saved.getTotalQuestions()).isEqualTo(3);
    }

    private Score savedScore() {
        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void optionWithoutAnswerKeyFlagCountsAsWrong() {
        options.get(0).setCorrect(null);

        AttemptResponse response = service.submit(1L, 1L, attempt(answer(1, 1)));

        assertThat(response.correctAnswers()).isZero();
        assertThat(response.points()).isZero();
    }

    @Test
    void rejectsQuestionThatDoesNotBelongToTheQuiz() {
        assertThatThrownBy(() -> service.submit(1L, 1L, attempt(answer(77, 1))))
                .isInstanceOf(BadRequestException.class);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    @Test
    void rejectsDuplicateAnswerForTheSameQuestion() {
        assertThatThrownBy(() -> service.submit(1L, 1L, attempt(answer(1, 1), answer(1, 2))))
                .isInstanceOf(BadRequestException.class);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    @Test
    void rejectsOptionFromAnotherQuestion() {
        // opção 3 pertence à questão 2, não à 1
        assertThatThrownBy(() -> service.submit(1L, 1L, attempt(answer(1, 3))))
                .isInstanceOf(BadRequestException.class);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    @Test
    void rejectsNonexistentOption() {
        assertThatThrownBy(() -> service.submit(1L, 1L, attempt(answer(1, 999))))
                .isInstanceOf(BadRequestException.class);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    @Test
    void failsWhenQuizDoesNotExist() {
        assertThatThrownBy(() -> service.submit(404L, 1L, attempt(answer(1, 1))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    @Test
    void failsWhenUserDoesNotExist() {
        AttemptRequest request = new AttemptRequest(List.of(answer(1, 1)));

        assertThatThrownBy(() -> service.submit(1L, 404L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scoreRepository, never()).save(any(Score.class));
    }

    @Test
    void failsWhenQuizHasNoQuestions() {
        lenient().when(questionRepository.findAllByQuizId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(1L, 1L, attempt(answer(1, 1))))
                .isInstanceOf(ConflictException.class);
        verify(scoreRepository, never()).save(any(Score.class));
    }
}
