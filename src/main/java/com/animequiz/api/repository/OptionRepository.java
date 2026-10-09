package com.animequiz.api.repository;

import com.animequiz.api.entity.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<Option, Long> {
    Page<Option> findByQuestionId(Long questionId, Pageable pageable);
}