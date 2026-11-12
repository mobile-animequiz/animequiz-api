package com.animequiz.api.service;

import com.animequiz.api.dto.QuizRequest;
import com.animequiz.api.dto.QuizResponse;
import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizQuestionCount;
import com.animequiz.api.repository.QuizRepository;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** D1 e D2: categoria e {@code questionCount} calculado na resposta, sem N+1. */
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private UserRepository userRepository;

    private QuizService service;

    @BeforeEach
    void setUp() {
        service = new QuizService(quizRepository, questionRepository, scoreRepository, userRepository);
    }

    private static Quiz quiz(long id, String title, Category category) {
        Quiz quiz = new Quiz(title, "Descrição do quiz", Difficulty.FACIL, category);
        quiz.setId(id);
        return quiz;
    }

    private static QuizQuestionCount count(long quizId, long total) {
        QuizQuestionCount row = mock(QuizQuestionCount.class);
        lenient().when(row.getQuizId()).thenReturn(quizId);
        lenient().when(row.getTotal()).thenReturn(total);
        return row;
    }

    @Test
    void aListingCountsTheQuestionsOfTheWholePageWithOneGroupedQuery() {
        Page<Quiz> page = new PageImpl<>(
                List.of(quiz(1, "Quiz um", Category.SHONEN), quiz(2, "Quiz dois", Category.SEINEN),
                        quiz(3, "Quiz três", Category.KODOMO)),
                PageRequest.of(0, 20), 3);
        when(quizRepository.findAll(any(Pageable.class))).thenReturn(page);
        // o quiz 3 não tem questões: não aparece na consulta agrupada
        when(questionRepository.countQuestionsByQuizIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(count(1, 4), count(2, 1)));

        List<QuizResponse> content = service.getAllQuizzes(PageRequest.of(0, 20)).getContent();

        assertThat(content).extracting(QuizResponse::questionCount).containsExactly(4L, 1L, 0L);
        assertThat(content).extracting(QuizResponse::category)
                .containsExactly(Category.SHONEN, Category.SEINEN, Category.KODOMO);
        verify(questionRepository, times(1)).countQuestionsByQuizIds(any());
        verify(questionRepository, never()).countByQuizId(anyLong());
    }

    @Test
    void anEmptyPageDoesNotRunTheCountQuery() {
        when(quizRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThat(service.getAllQuizzes(PageRequest.of(0, 20)).getContent()).isEmpty();

        verify(questionRepository, never()).countQuestionsByQuizIds(any());
    }

    @Test
    void titleSearchAlsoCountsWithOneGroupedQuery() {
        Page<Quiz> page = new PageImpl<>(List.of(quiz(1, "Naruto Quiz", Category.SHONEN)), PageRequest.of(0, 20), 1);
        when(quizRepository.findByTitleContainingIgnoreCase(eq("naruto"), any(Pageable.class))).thenReturn(page);
        when(questionRepository.countQuestionsByQuizIds(List.of(1L))).thenReturn(List.of(count(1, 3)));

        List<QuizResponse> content = service.searchByTitle("naruto", PageRequest.of(0, 20)).getContent();

        assertThat(content).extracting(QuizResponse::questionCount).containsExactly(3L);
        verify(questionRepository, never()).countByQuizId(anyLong());
    }

    @Test
    void difficultySearchAlsoCountsWithOneGroupedQuery() {
        Page<Quiz> page = new PageImpl<>(List.of(quiz(2, "One Piece Quiz", Category.SHONEN)), PageRequest.of(0, 20), 1);
        when(quizRepository.findByDifficulty(eq(Difficulty.FACIL), any(Pageable.class))).thenReturn(page);
        when(questionRepository.countQuestionsByQuizIds(List.of(2L))).thenReturn(List.of(count(2, 5)));

        assertThat(service.searchByDifficulty(Difficulty.FACIL, PageRequest.of(0, 20)).getContent())
                .extracting(QuizResponse::questionCount).containsExactly(5L);
    }

    @Test
    void readingOneQuizCountsItsQuestionsWithASingleQuery() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz(1, "Quiz um", Category.JOSEI)));
        when(questionRepository.countByQuizId(1L)).thenReturn(3L);

        QuizResponse response = service.getQuizById(1L);

        assertThat(response.questionCount()).isEqualTo(3L);
        assertThat(response.category()).isEqualTo(Category.JOSEI);
        verify(questionRepository, never()).countQuestionsByQuizIds(any());
    }

    @Test
    void aNewQuizHasNoQuestionsAndDoesNotQueryTheCount() {
        when(quizRepository.existsByTitleIgnoreCase("Quiz novo")).thenReturn(false);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        QuizResponse response = service.createQuiz(
                new QuizRequest("Quiz novo", "Descrição do quiz", Difficulty.DIFICIL, Category.SHOJO));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo(Category.SHOJO);
        assertThat(response.questionCount()).isZero();
        verify(questionRepository, never()).countByQuizId(anyLong());
    }

    @Test
    void updatingAQuizChangesTheCategoryAndKeepsTheRealQuestionCount() {
        Quiz existing = quiz(1, "Quiz um", Category.SHONEN);
        when(quizRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.countByQuizId(1L)).thenReturn(3L);

        QuizResponse response = service.updateQuiz(1L,
                new QuizRequest("Quiz um", "Nova descrição", Difficulty.MEDIO, Category.SEINEN));

        assertThat(response.category()).isEqualTo(Category.SEINEN);
        assertThat(response.difficulty()).isEqualTo(Difficulty.MEDIO);
        assertThat(response.questionCount()).isEqualTo(3L);
        assertThat(existing.getCategory()).isEqualTo(Category.SEINEN);
    }
}
