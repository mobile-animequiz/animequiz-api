package com.animequiz.api.controller;

import com.animequiz.api.dto.ScoreResponse;
import com.animequiz.api.exception.GoneException;
import com.animequiz.api.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Removido: o cliente não pode mais informar a pontuação. O score é criado pelo servidor ao corrigir
     * uma tentativa em {@code POST /quizzes/{quizId}/attempts}.
     */
    @PostMapping
    @Deprecated
    @Operation(
            summary = "[Removido] Registar nova pontuação",
            description = "Removido na Fase 2: a pontuação é calculada no servidor. Use `POST /quizzes/{quizId}/attempts`.",
            deprecated = true)
    @ApiResponse(responseCode = "410", description = "Gone: endpoint removido; use POST /quizzes/{quizId}/attempts")
    public ResponseEntity<Void> createScore() {
        throw new GoneException("Endpoint removido: a pontuação é calculada no servidor. Use POST /quizzes/{quizId}/attempts");
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

    /** Removido: reescrever pontos permitiria forjar o ranking. */
    @PutMapping("/{id}")
    @Deprecated
    @Operation(
            summary = "[Removido] Atualizar pontuação",
            description = "Removido na Fase 2: a pontuação é calculada no servidor e não pode ser editada.",
            deprecated = true)
    @ApiResponse(responseCode = "410", description = "Gone: endpoint removido; a pontuação não pode ser editada")
    public ResponseEntity<Void> updateScore(@PathVariable Long id) {
        throw new GoneException("Endpoint removido: a pontuação é calculada no servidor e não pode ser editada");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir pontuação")
    public ResponseEntity<Void> deleteScore(@PathVariable Long id) {
        scoreService.deleteScore(id);
        return ResponseEntity.noContent().build();
    }
}