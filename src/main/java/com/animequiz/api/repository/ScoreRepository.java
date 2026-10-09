package com.animequiz.api.repository;

import com.animequiz.api.entity.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    boolean existsByUserId(Long userId);
    Page<Score> findByPointsGreaterThanEqual(Integer minPoints, Pageable pageable);
    Page<Score> findAllByOrderByPointsDesc(Pageable pageable);
}