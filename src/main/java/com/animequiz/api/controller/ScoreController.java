package com.animequiz.api.controller;

import com.animequiz.api.entity.Score;
import com.animequiz.api.repository.ScoreRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/scores")
public class ScoreController {

    @Autowired
    private ScoreRepository scoreRepository;

    @PostMapping
    public ResponseEntity<Score> create(@RequestBody @Valid Score score) {
        Score saved = scoreRepository.save(score);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<Score>> listAll(Pageable pageable) {
        return ResponseEntity.ok(scoreRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Score> findById(@PathVariable Long id) {
        Optional<Score> score = scoreRepository.findById(id);
        return score.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!scoreRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        scoreRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}