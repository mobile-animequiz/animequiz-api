package com.animequiz.api.service;

import com.animequiz.api.dto.OptionRequest;
import com.animequiz.api.dto.OptionResponse;
import com.animequiz.api.entity.Option;
import com.animequiz.api.entity.Question;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.OptionRepository;
import com.animequiz.api.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptionService {

    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;

    public OptionService(OptionRepository optionRepository, QuestionRepository questionRepository) {
        this.optionRepository = optionRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public OptionResponse createOption(OptionRequest request) {
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Questão não encontrada com ID: " + request.questionId()));

        Option option = new Option(question, request.text(), request.isCorrect());
        return toResponse(optionRepository.save(option));
    }

    @Transactional(readOnly = true)
    public OptionResponse getOptionById(Long id) {
        Option option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opção não encontrada com ID: " + id));
        return toResponse(option);
    }

    @Transactional(readOnly = true)
    public Page<OptionResponse> getAllOptions(Pageable pageable) {
        return optionRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OptionResponse> getOptionsByQuestionId(Long questionId, Pageable pageable) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Questão não encontrada com ID: " + questionId);
        }
        return optionRepository.findByQuestionId(questionId, pageable).map(this::toResponse);
    }

    @Transactional
    public OptionResponse updateOption(Long id, OptionRequest request) {
        Option option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opção não encontrada com ID: " + id));

        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Questão não encontrada com ID: " + request.questionId()));

        option.setQuestion(question);
        option.setText(request.text());
        option.setIsCorrect(request.isCorrect());

        return toResponse(optionRepository.save(option));
    }

    @Transactional
    public void deleteOption(Long id) {
        if (!optionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Opção não encontrada com ID: " + id);
        }
        optionRepository.deleteById(id);
    }

    private OptionResponse toResponse(Option option) {
        return new OptionResponse(
                option.getId(),
                option.getQuestion().getId(),
                option.getText(),
                option.getIsCorrect()
        );
    }
}