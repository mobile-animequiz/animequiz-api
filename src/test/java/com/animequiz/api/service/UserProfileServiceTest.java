package com.animequiz.api.service;

import com.animequiz.api.dto.UserProfileDetailsResponse;
import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserProfileRepository;
import com.animequiz.api.repository.UserQuizStats;
import com.animequiz.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** D5: XP (melhor pontuação por quiz), quizzes concluídos, acertos/respostas, nível e patente; leitura sem escrita. */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ScoreRepository scoreRepository;

    private UserProfileService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(userProfileRepository, userRepository, scoreRepository, new LevelCalculator());

        user = new User("Naruto Uzumaki", "naruto@konoha.com");
        user.setId(1L);
        UserProfile profile = new UserProfile(user, "Futuro Hokage", "https://exemplo.com/naruto.png");
        profile.setId(5L);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
    }

    private static UserQuizStats quiz(long quizId, int bestPoints, long correctTotal, long answeredTotal) {
        UserQuizStats stats = mock(UserQuizStats.class);
        lenient().when(stats.getQuizId()).thenReturn(quizId);
        lenient().when(stats.getBestPoints()).thenReturn(bestPoints);
        lenient().when(stats.getCorrectTotal()).thenReturn(correctTotal);
        lenient().when(stats.getAnsweredTotal()).thenReturn(answeredTotal);
        return stats;
    }

    private UserProfileDetailsResponse read() {
        return service.getProfileDetailsByUserId(1L);
    }

    @Test
    void theProfileFieldsAreCopiedToTheResponse() {
        when(scoreRepository.findQuizStatsByUserId(1L)).thenReturn(List.of());

        UserProfileDetailsResponse response = read();

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("Naruto Uzumaki");
        assertThat(response.bio()).isEqualTo("Futuro Hokage");
        assertThat(response.avatarUrl()).isEqualTo("https://exemplo.com/naruto.png");
    }

    @Test
    void aUserWithoutAttemptsStartsAtLevelOneWithZeroedStats() {
        when(scoreRepository.findQuizStatsByUserId(1L)).thenReturn(List.of());

        UserProfileDetailsResponse response = read();

        assertThat(response.xp()).isZero();
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.rankTitle()).isEqualTo("Iniciante");
        assertThat(response.levelStartXp()).isZero();
        assertThat(response.nextLevelXp()).isEqualTo(100);
        assertThat(response.completedQuizzes()).isZero();
        assertThat(response.correctAnswers()).isZero();
        assertThat(response.totalAnswers()).isZero();
    }

    @Test
    void xpSumsOnlyTheBestScoreOfEachDistinctQuiz() {
        // quiz 1: várias tentativas, melhor = 30; quiz 2: melhor = 60. O histórico (acertos/respostas) soma tudo.
        when(scoreRepository.findQuizStatsByUserId(1L))
                .thenReturn(List.of(quiz(1, 30, 5, 9), quiz(2, 60, 3, 3)));

        UserProfileDetailsResponse response = read();

        assertThat(response.xp()).isEqualTo(90);
        assertThat(response.completedQuizzes()).isEqualTo(2);
        assertThat(response.correctAnswers()).isEqualTo(8);
        assertThat(response.totalAnswers()).isEqualTo(12);
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.nextLevelXp()).isEqualTo(100);
    }

    @Test
    void levelAndRankComeFromTheApprovedFormula() {
        when(scoreRepository.findQuizStatsByUserId(1L)).thenReturn(List.of(quiz(1, 2450, 3, 3)));

        UserProfileDetailsResponse response = read();

        assertThat(response.level()).isEqualTo(7);
        assertThat(response.rankTitle()).isEqualTo("Aprendiz");
        assertThat(response.xp()).isEqualTo(2450);
        assertThat(response.levelStartXp()).isEqualTo(2100);
        assertThat(response.nextLevelXp()).isEqualTo(2800);
    }

    @Test
    void atTheLevelCapTheXpKeepsAccumulatingButTheLevelStays() {
        when(scoreRepository.findQuizStatsByUserId(1L))
                .thenReturn(List.of(quiz(1, 150_000, 10, 10), quiz(2, 50_000, 10, 10)));

        UserProfileDetailsResponse response = read();

        assertThat(response.xp()).isEqualTo(200_000);
        assertThat(response.level()).isEqualTo(50);
        assertThat(response.rankTitle()).isEqualTo("Lenda");
        assertThat(response.levelStartXp()).isEqualTo(122_500);
        assertThat(response.nextLevelXp()).isEqualTo(127_500);
    }

    @Test
    void readingAProfileNeverWritesAnything() {
        when(scoreRepository.findQuizStatsByUserId(1L)).thenReturn(List.of(quiz(1, 30, 3, 3)));

        read();

        verify(userProfileRepository, never()).save(any());
        verify(userProfileRepository, never()).saveAndFlush(any());
        verify(scoreRepository, times(1)).findQuizStatsByUserId(1L);
    }

    @Test
    void anUnknownUserIsNotFoundAndNothingElseIsQueried() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileDetailsByUserId(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado com ID: 9");

        verifyNoInteractions(userProfileRepository, scoreRepository);
    }

    @Test
    void aUserWithoutProfileIsNotFoundAndTheProfileIsNotCreated() {
        User orphan = new User("Sem Perfil", "sem@perfil.com");
        orphan.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(orphan));
        when(userProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileDetailsByUserId(2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Perfil não encontrado para o usuário com ID: 2");

        verify(userProfileRepository, never()).save(any());
        verifyNoInteractions(scoreRepository);
    }
}
