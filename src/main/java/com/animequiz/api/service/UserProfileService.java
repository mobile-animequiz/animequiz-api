package com.animequiz.api.service;

import com.animequiz.api.dto.UserProfileRequest;
import com.animequiz.api.dto.UserProfileResponse;
import com.animequiz.api.entity.User;
import com.animequiz.api.entity.UserProfile;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.UserProfileRepository;
import com.animequiz.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository userProfileRepository, UserRepository userRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
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