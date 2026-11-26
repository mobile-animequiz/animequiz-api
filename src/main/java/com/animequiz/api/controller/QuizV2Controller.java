package com.animequiz.api.controller;

import com.animequiz.api.dto.*;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.service.QuizService;
import com.animequiz.api.service.QuizPresentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.web.bind.annotation.*;

/** Thin transport adapter; V1 keeps its original DTO and the same business service. */
@RestController
@RequestMapping(value = "/quizzes", headers = "X-API-Version=2")
@Tag(name = "Quizzes")
public class QuizV2Controller {
    private final QuizService quizzes;
    private final QuizPresentationService presentation;
    private final PagedResourcesAssembler<QuizV2Response> pages;
    public QuizV2Controller(QuizService quizzes, QuizPresentationService presentation,
            PagedResourcesAssembler<QuizV2Response> pages) {
        this.quizzes = quizzes; this.presentation = presentation; this.pages = pages;
    }
    private PagedModel<EntityModel<QuizV2Response>> page(Page<QuizResponse> base, String lang) {
        return pages.toModel(presentation.page(base, lang));
    }
    @GetMapping @Operation(summary = "Catálogo com capas, idioma e regras de tempo")
    public PagedModel<EntityModel<QuizV2Response>> list(@ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "pt-BR") String lang) {
        return page(quizzes.getAllQuizzes(pageable), lang);
    }
    @GetMapping("/{id}") @Operation(summary = "Detalhes do quiz para o jogador")
    public EntityModel<QuizV2Response> one(@PathVariable Long id,
            @RequestParam(defaultValue = "pt-BR") String lang) {
        return EntityModel.of(presentation.one(quizzes.getQuizById(id), lang));
    }
    @GetMapping("/search") @Operation(summary = "Buscar pelo título original")
    public PagedModel<EntityModel<QuizV2Response>> search(@RequestParam String title,
            @ParameterObject Pageable pageable, @RequestParam(defaultValue = "pt-BR") String lang) {
        return page(quizzes.searchByTitle(title, pageable), lang);
    }
    @GetMapping("/difficulty/{difficulty}") @Operation(summary = "Filtrar por dificuldade")
    public PagedModel<EntityModel<QuizV2Response>> difficulty(@PathVariable Difficulty difficulty,
            @ParameterObject Pageable pageable, @RequestParam(defaultValue = "pt-BR") String lang) {
        return page(quizzes.searchByDifficulty(difficulty, pageable), lang);
    }
}
