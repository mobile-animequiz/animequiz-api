package com.animequiz.api.controller;

import com.animequiz.api.entity.Quiz;
import com.animequiz.api.repository.QuizRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/quizzes")
public class QuizController {

    @Autowired
    private QuizRepository quizRepository;

    public ResponseEntity<Quiz> create(@RequestBody @Valid Quiz quiz) {
        Quiz savedQuiz = quizRepository.save(quiz);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedQuiz);
    }

    @GetMapping
    public ResponseEntity<Page<Quiz>> listAll(Pageable pageable) {
        Page<Quiz> quizzes = quizRepository.findAll(pageable);
        return ResponseEntity.ok(quizzes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quiz> findById(@PathVariable Long id) {
        Optional<Quiz> quiz = quizRepository.findById(id);
        return quiz.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!quizRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        quizRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}