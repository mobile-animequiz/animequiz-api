package com.animequiz.api.controller;

import com.animequiz.api.dto.QuizRequest;
import com.animequiz.api.dto.QuizResponse;
import com.animequiz.api.entity.Difficulty;
import com.animequiz.api.service.QuizService;
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
@RequestMapping("/quizzes")
@Tag(name = "Quizzes", description = "Gerenciamento de quizzes")
public class QuizController {

    private final QuizService quizService;
    private final PagedResourcesAssembler<QuizResponse> pagedAssembler;

    public QuizController(QuizService quizService, PagedResourcesAssembler<QuizResponse> pagedAssembler) {
        this.quizService = quizService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Criar quiz")
    public ResponseEntity<EntityModel<QuizResponse>> createQuiz(@Valid @RequestBody QuizRequest request) {
        QuizResponse response = quizService.createQuiz(request);
        EntityModel<QuizResponse> model = EntityModel.of(response,
                linkTo(methodOn(QuizController.class).getQuizById(response.id())).withSelfRel(),
                linkTo(methodOn(QuizController.class).getAllQuizzes(null)).withRel("all-quizzes")
        );
        return ResponseEntity.created(URI.create("/quizzes/" + response.id())).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar quiz por ID")
    public ResponseEntity<EntityModel<QuizResponse>> getQuizById(@PathVariable Long id) {
        QuizResponse response = quizService.getQuizById(id);
        EntityModel<QuizResponse> model = EntityModel.of(response,
                linkTo(methodOn(QuizController.class).getQuizById(id)).withSelfRel(),
                linkTo(methodOn(QuizController.class).getAllQuizzes(null)).withRel("all-quizzes"),
                linkTo(methodOn(QuizController.class).updateQuiz(id, null)).withRel("update"),
                linkTo(methodOn(QuizController.class).deleteQuiz(id)).withRel("delete")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping
    @Operation(summary = "Listar quizzes (paginado)")
    public ResponseEntity<PagedModel<EntityModel<QuizResponse>>> getAllQuizzes(@ParameterObject Pageable pageable) {
        Page<QuizResponse> page = quizService.getAllQuizzes(pageable);
        PagedModel<EntityModel<QuizResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(QuizController.class).getQuizById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/search")
    @Operation(summary = "Consulta personalizada: Pesquisar quizzes por título")
    public ResponseEntity<PagedModel<EntityModel<QuizResponse>>> searchByTitle(
            @RequestParam String title, @ParameterObject Pageable pageable) {
        Page<QuizResponse> page = quizService.searchByTitle(title, pageable);
        PagedModel<EntityModel<QuizResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(QuizController.class).getQuizById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/difficulty/{difficulty}")
    @Operation(summary = "Consulta personalizada: Listar quizzes por dificuldade")
    public ResponseEntity<PagedModel<EntityModel<QuizResponse>>> searchByDifficulty(
            @PathVariable Difficulty difficulty, @ParameterObject Pageable pageable) {
        Page<QuizResponse> page = quizService.searchByDifficulty(difficulty, pageable);
        PagedModel<EntityModel<QuizResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(QuizController.class).getQuizById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar quiz")
    public ResponseEntity<EntityModel<QuizResponse>> updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        QuizResponse response = quizService.updateQuiz(id, request);
        EntityModel<QuizResponse> model = EntityModel.of(response,
                linkTo(methodOn(QuizController.class).getQuizById(id)).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir quiz")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }
}