package com.animequiz.api.controller;

import com.animequiz.api.dto.OptionRequest;
import com.animequiz.api.dto.OptionResponse;
import com.animequiz.api.service.OptionService;
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
@RequestMapping("/options")
@Tag(name = "Opções", description = "Endpoints para gestão das alternativas de resposta")
public class OptionController {

    private final OptionService optionService;
    private final PagedResourcesAssembler<OptionResponse> pagedAssembler;

    public OptionController(OptionService optionService, PagedResourcesAssembler<OptionResponse> pagedAssembler) {
        this.optionService = optionService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Criar nova opção de resposta")
    public ResponseEntity<EntityModel<OptionResponse>> createOption(@Valid @RequestBody OptionRequest request) {
        OptionResponse response = optionService.createOption(request);
        EntityModel<OptionResponse> model = EntityModel.of(response,
                linkTo(methodOn(OptionController.class).getOptionById(response.id())).withSelfRel(),
                linkTo(methodOn(OptionController.class).getAllOptions(null)).withRel("all-options")
        );
        return ResponseEntity.created(URI.create("/options/" + response.id())).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar opção por ID")
    public ResponseEntity<EntityModel<OptionResponse>> getOptionById(@PathVariable Long id) {
        OptionResponse response = optionService.getOptionById(id);
        EntityModel<OptionResponse> model = EntityModel.of(response,
                linkTo(methodOn(OptionController.class).getOptionById(id)).withSelfRel(),
                linkTo(methodOn(OptionController.class).getAllOptions(null)).withRel("all-options")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping
    @Operation(summary = "Listar todas as opções (Paginado)")
    public ResponseEntity<PagedModel<EntityModel<OptionResponse>>> getAllOptions(@ParameterObject Pageable pageable) {
        Page<OptionResponse> page = optionService.getAllOptions(pageable);
        PagedModel<EntityModel<OptionResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(OptionController.class).getOptionById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/by-question/{questionId}")
    @Operation(summary = "Consulta personalizada: Listar opções por Questão ID",
            description = "Alternativas da questão em ordem crescente de `id`. Devolve só `id`, `questionId` e `text`: " +
                    "o gabarito nunca é retornado. Paginado: `size` padrão 20.")
    public ResponseEntity<PagedModel<EntityModel<OptionResponse>>> getOptionsByQuestionId(
            @PathVariable Long questionId, @ParameterObject Pageable pageable) {
        Page<OptionResponse> page = optionService.getOptionsByQuestionId(questionId, pageable);
        PagedModel<EntityModel<OptionResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(OptionController.class).getOptionById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar opção")
    public ResponseEntity<EntityModel<OptionResponse>> updateOption(@PathVariable Long id, @Valid @RequestBody OptionRequest request) {
        OptionResponse response = optionService.updateOption(id, request);
        EntityModel<OptionResponse> model = EntityModel.of(response,
                linkTo(methodOn(OptionController.class).getOptionById(id)).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir opção")
    public ResponseEntity<Void> deleteOption(@PathVariable Long id) {
        optionService.deleteOption(id);
        return ResponseEntity.noContent().build();
    }
}