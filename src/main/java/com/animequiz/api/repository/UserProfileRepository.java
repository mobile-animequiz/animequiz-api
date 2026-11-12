package com.animequiz.api.repository;

import com.animequiz.api.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // O usuário é lido para montar a resposta (username); o EntityGraph evita uma consulta por perfil (N+1).
    @Override
    @EntityGraph(attributePaths = "user")
    Page<UserProfile> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<UserProfile> findByBioContainingIgnoreCase(String bio, Pageable pageable);
}
