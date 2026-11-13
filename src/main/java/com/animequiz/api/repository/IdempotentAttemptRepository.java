package com.animequiz.api.repository;

import com.animequiz.api.entity.IdempotentAttempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotentAttemptRepository extends JpaRepository<IdempotentAttempt, Long> {
    Optional<IdempotentAttempt> findByUserIdAndRequestKey(Long userId, String requestKey);
}
