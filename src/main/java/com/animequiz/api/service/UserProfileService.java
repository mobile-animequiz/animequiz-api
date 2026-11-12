package com.animequiz.api.service;

import com.animequiz.api.dto.UserProfileDetailsResponse;
import com.animequiz.api.dto.UserProfileRequest;
import com.animequiz.api.dto.UserProfileResponse;
import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.ScoreRepository;
import com.animequiz.api.repository.UserProfileRepository;
import com.animequiz.api.repository.UserQuizStats;
import com.animequiz.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final ScoreRepository scoreRepository;
    private final LevelCalculator levelCalculator;

    public UserProfileService(UserProfileRepository userProfileRepository, UserRepository userRepository,
                              ScoreRepository scoreRepository, LevelCalculator levelCalculator) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.scoreRepository = scoreRepository;
        this.levelCalculator = levelCalculator;
    }

    @Transactional
    public UserProfileResponse createUserProfile(UserProfileRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado com ID: " + request.userId()));

        if (userProfileRepository.findByUserId(request.userId()).isPresent()) {
            throw new ConflictException("Já existe um perfil associado a este utilizador.");
        }

        UserProfile profile = new UserProfile(user, request.bio(), request.avatarUrl());
        return toResponse(userProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileById(Long id) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado com ID: " + id));
        return toResponse(profile);
    }

    /**
     * Perfil de um usuário com nível e estatísticas. É somente leitura: nunca cria nem altera registros.
     * Todo usuário tem perfil (criado no cadastro e no seed), então um perfil ausente indica dado inconsistente
     * e é reportado como 404, sem ser criado aqui.
     *
     * <p>Regras (uma consulta agregada sobre os scores do usuário, uma linha por quiz distinto):
     * <ul>
     *   <li>{@code xp}: soma da MELHOR pontuação de cada quiz; todas as tentativas continuam no histórico;</li>
     *   <li>{@code completedQuizzes}: quizzes distintos já respondidos;</li>
     *   <li>{@code correctAnswers} e {@code totalAnswers}: somas de todas as tentativas (respostas enviadas).</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public UserProfileDetailsResponse getProfileDetailsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado para o usuário com ID: " + userId));

        List<UserQuizStats> quizStats = scoreRepository.findQuizStatsByUserId(userId);
        long xp = 0;
        long correctAnswers = 0;
        long totalAnswers = 0;
        for (UserQuizStats stats : quizStats) {
            xp += stats.getBestPoints();
            correctAnswers += stats.getCorrectTotal();
            totalAnswers += stats.getAnsweredTotal();
        }
        LevelCalculator.LevelProgress progress = levelCalculator.progressFor(xp);

        return new UserProfileDetailsResponse(
                profile.getId(),
                user.getId(),
                user.getUsername(),
                profile.getBio(),
                profile.getAvatarUrl(),
                progress.level(),
                progress.rankTitle(),
                xp,
                progress.levelStartXp(),
                progress.nextLevelXp(),
                quizStats.size(),
                correctAnswers,
                totalAnswers
        );
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUserProfiles(Pageable pageable) {
        return userProfileRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> searchProfilesByBio(String bio, Pageable pageable) {
        return userProfileRepository.findByBioContainingIgnoreCase(bio, pageable).map(this::toResponse);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(Long id, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado com ID: " + id));

        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());

        return toResponse(userProfileRepository.save(profile));
    }

    @Transactional
    public void deleteUserProfile(Long id) {
        if (!userProfileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Perfil não encontrado com ID: " + id);
        }
        userProfileRepository.deleteById(id);
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getUsername(),
                profile.getBio(),
                profile.getAvatarUrl()
        );
    }
}