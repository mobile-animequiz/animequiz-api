package com.animequiz.api.repository;

import com.animequiz.api.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);
    Page<UserProfile> findByBioContainingIgnoreCase(String bio, Pageable pageable);
}