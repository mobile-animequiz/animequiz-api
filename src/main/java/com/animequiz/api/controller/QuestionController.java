package com.animequiz.api.controller;

import com.animequiz.api.dto.QuestionRequest;
import com.animequiz.api.dto.QuestionResponse;
import com.animequiz.api.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/questions")
@Tag(name = "Questões", description = "Endpoints para gestão de perguntas dos Quizzes")
public class QuestionController {

    private final QuestionService questionService;
    private final PagedResourcesAssembler<QuestionResponse> pagedAssembler;

    public QuestionController(QuestionService questionService, PagedResourcesAssembler<QuestionResponse> pagedAssembler) {
        this.questionService = questionService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Criar uma nova questão")
    public ResponseEntity<EntityModel<QuestionResponse>> createQuestion(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = questionService.createQuestion(request);
        EntityModel<QuestionResponse> model = EntityModel.of(response,
                linkTo(methodOn(QuestionController.class).getQuestionById(response.id())).withSelfRel(),
                linkTo(methodOn(QuestionController.class).getAllQuestions(null)).withRel("all-questions")
        );
        return ResponseEntity.created(URI.create("/questions/" + response.id())).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar questão por ID")
    public ResponseEntity<EntityModel<QuestionResponse>> getQuestionById(@PathVariable Long id) {
        QuestionResponse response = questionService.getQuestionById(id);
        EntityModel<QuestionResponse> model = EntityModel.of(response,
                linkTo(methodOn(QuestionController.class).getQuestionById(id)).withSelfRel(),
                linkTo(methodOn(QuestionController.class).getAllQuestions(null)).withRel("all-questions")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping
    @Operation(summary = "Listar todas as questões (Paginado)")
    public ResponseEntity<PagedModel<EntityModel<QuestionResponse>>> getAllQuestions(@ParameterObject Pageable pageable) {
        Page<QuestionResponse> page = questionService.getAllQuestions(pageable);
        PagedModel<EntityModel<QuestionResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(QuestionController.class).getQuestionById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/by-quiz/{quizId}")
    @Operation(summary = "Consulta personalizada: Listar questões por Quiz ID",
            description = "Questões do quiz em ordem crescente de `id`, sem gabarito e sem alternativas " +
                    "(as alternativas vêm de `GET /options/by-question/{questionId}`). Paginado: `size` padrão 20.")
    public ResponseEntity<PagedModel<EntityModel<QuestionResponse>>> getQuestionsByQuizId(
            @PathVariable Long quizId, @ParameterObject Pageable pageable) {
        Page<QuestionResponse> page = questionService.getQuestionsByQuizId(quizId, pageable);
        PagedModel<EntityModel<QuestionResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(QuestionController.class).getQuestionById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar questão")
    public ResponseEntity<EntityModel<QuestionResponse>> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = questionService.updateQuestion(id, request);
        EntityModel<QuestionResponse> model = EntityModel.of(response,
                linkTo(methodOn(QuestionController.class).getQuestionById(id)).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir questão")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}