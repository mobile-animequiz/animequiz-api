package com.animequiz.api.controller;

import com.animequiz.api.dto.ScoreRequest;
import com.animequiz.api.dto.ScoreResponse;
import com.animequiz.api.service.ScoreService;
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
@RequestMapping("/scores")
@Tag(name = "Scores e Rankings", description = "Endpoints para gestão de pontuações e ranking")
public class ScoreController {

    private final ScoreService scoreService;
    private final PagedResourcesAssembler<ScoreResponse> pagedAssembler;

    public ScoreController(ScoreService scoreService, PagedResourcesAssembler<ScoreResponse> pagedAssembler) {
        this.scoreService = scoreService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Registar nova pontuação")
    public ResponseEntity<EntityModel<ScoreResponse>> createScore(@Valid @RequestBody ScoreRequest request) {
        ScoreResponse response = scoreService.createScore(request);
        EntityModel<ScoreResponse> model = EntityModel.of(response,
                linkTo(methodOn(ScoreController.class).getScoreById(response.id())).withSelfRel(),
                linkTo(methodOn(ScoreController.class).getAllScores(null)).withRel("all-scores")
        );
        return ResponseEntity.created(URI.create("/scores/" + response.id())).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pontuação por ID")
    public ResponseEntity<EntityModel<ScoreResponse>> getScoreById(@PathVariable Long id) {
        ScoreResponse response = scoreService.getScoreById(id);
        EntityModel<ScoreResponse> model = EntityModel.of(response,
                linkTo(methodOn(ScoreController.class).getScoreById(id)).withSelfRel(),
                linkTo(methodOn(ScoreController.class).getAllScores(null)).withRel("all-scores")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping
    @Operation(summary = "Listar todas as pontuações (Paginado)")
    public ResponseEntity<PagedModel<EntityModel<ScoreResponse>>> getAllScores(@ParameterObject Pageable pageable) {
        Page<ScoreResponse> page = scoreService.getAllScores(pageable);
        PagedModel<EntityModel<ScoreResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(ScoreController.class).getScoreById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/ranking")
    @Operation(summary = "Consulta personalizada: Obter ranking ordenado por pontuação")
    public ResponseEntity<PagedModel<EntityModel<ScoreResponse>>> getRanking(@ParameterObject Pageable pageable) {
        Page<ScoreResponse> page = scoreService.getRanking(pageable);
        PagedModel<EntityModel<ScoreResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(ScoreController.class).getScoreById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pontuação")
    public ResponseEntity<EntityModel<ScoreResponse>> updateScore(@PathVariable Long id, @Valid @RequestBody ScoreRequest request) {
        ScoreResponse response = scoreService.updateScore(id, request);
        EntityModel<ScoreResponse> model = EntityModel.of(response,
                linkTo(methodOn(ScoreController.class).getScoreById(id)).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar pontuação")
    public ResponseEntity<Void> deleteScore(@PathVariable Long id) {
        scoreService.deleteScore(id);
        return ResponseEntity.noContent().build();
    }
}