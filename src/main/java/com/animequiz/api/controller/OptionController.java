package com.animequiz.api.controller;

import com.animequiz.api.entity.Option;
import com.animequiz.api.repository.OptionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/options")
public class OptionController {

    @Autowired
    private OptionRepository optionRepository;

    @PostMapping
    public ResponseEntity<Option> create(@RequestBody @Valid Option option) {
        Option saved = optionRepository.save(option);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<Page<Option>> listAll(Pageable pageable) {
        return ResponseEntity.ok(optionRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Option> findById(@PathVariable Long id) {
        Optional<Option> option = optionRepository.findById(id);
        return option.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!optionRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        optionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}