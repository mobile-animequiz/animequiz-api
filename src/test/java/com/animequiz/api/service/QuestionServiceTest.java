package com.animequiz.api.service;

import com.animequiz.api.dto.OptionSummaryResponse;
import com.animequiz.api.dto.QuestionWithOptionsResponse;
import com.animequiz.api.entity.Category;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.entity.Question;
import com.animequiz.api.entity.Quiz;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.OptionRepository;
import com.animequiz.api.repository.OptionView;
import com.animequiz.api.repository.QuestionRepository;
import com.animequiz.api.repository.QuizRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** D3: questões de um quiz com as alternativas, sem N+1 e sem gabarito. */
@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private OptionRepository optionRepository;

    private QuestionService service;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        service = new QuestionService(questionRepository, quizRepository, optionRepository);
        quiz = new Quiz("Quiz de teste", "Descrição do quiz", Difficulty.FACIL, Category.SHONEN);
        quiz.setId(1L);
    }

    private Question question(long id, String statement) {
        Question question = new Question(quiz, statement);
        question.setId(id);
        return question;
    }

    private static OptionView option(long id, String text, long questionId) {
        OptionView view = mock(OptionView.class);
        lenient().when(view.getId()).thenReturn(id);
        lenient().when(view.getText()).thenReturn(text);
        lenient().when(view.getQuestionId()).thenReturn(questionId);
        return view;
    }

    @Test
    void optionsOfAllQuestionsAreLoadedWithOneQueryAndGroupedByQuestion() {
        Page<Question> page = new PageImpl<>(
                List.of(question(1, "Primeira pergunta"), question(2, "Segunda pergunta"), question(3, "Terceira pergunta")),
                PageRequest.of(0, 20), 3);
        when(quizRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findByQuizIdOrderByIdAsc(eq(1L), any(Pageable.class))).thenReturn(page);
        // a consulta já devolve as alternativas ordenadas por ID; a questão 3 não tem alternativas
        when(optionRepository.findViewsByQuestionIds(List.of(1L, 2L, 3L))).thenReturn(List.of(
                option(10, "A", 1), option(11, "B", 1), option(12, "C", 2), option(13, "D", 2), option(14, "E", 2)));

        List<QuestionWithOptionsResponse> content =
                service.getQuestionsWithOptionsByQuizId(1L, PageRequest.of(0, 20)).getContent();

        assertThat(content).extracting(QuestionWithOptionsResponse::id).containsExactly(1L, 2L, 3L);
        assertThat(content.get(0).options()).extracting(OptionSummaryResponse::id).containsExactly(10L, 11L);
        assertThat(content.get(0).options()).extracting(OptionSummaryResponse::text).containsExactly("A", "B");
        assertThat(content.get(1).options()).extracting(OptionSummaryResponse::id).containsExactly(12L, 13L, 14L);
        assertThat(content.get(2).options()).isEmpty();

        assertThat(content.get(0).quizId()).isEqualTo(1L);
        assertThat(content.get(0).quizTitle()).isEqualTo("Quiz de teste");
        assertThat(content.get(0).text()).isEqualTo("Primeira pergunta");

        // uma única consulta de alternativas para a página inteira (sem N+1)
        verify(optionRepository, times(1)).findViewsByQuestionIds(any());
        verify(optionRepository, never()).findByQuestionIdOrderByIdAsc(any(), any());
        verify(optionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void anUnknownQuizIsNotFoundAndNothingElseIsQueried() {
        when(quizRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.getQuestionsWithOptionsByQuizId(9L, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Quiz não encontrado com ID: 9");

        verifyNoInteractions(questionRepository, optionRepository);
    }

    @Test
    void aQuizWithoutQuestionsDoesNotQueryOptions() {
        when(quizRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.findByQuizIdOrderByIdAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        assertThat(service.getQuestionsWithOptionsByQuizId(1L, PageRequest.of(0, 20)).getContent()).isEmpty();

        verifyNoInteractions(optionRepository);
    }
}
