package com.animequiz.api.repository;

import com.animequiz.api.entity.ApiKey;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyHashAndRevokedFalse(String keyHash);
    Page<ApiKey> findByOwnerId(Long ownerId, Pageable pageable);
    Optional<ApiKey> findByIdAndOwnerId(Long id, Long ownerId);
}
