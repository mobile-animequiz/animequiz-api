package com.animequiz.api.controller;

import com.animequiz.api.dto.UserProfileRequest;
import com.animequiz.api.dto.UserProfileResponse;
import com.animequiz.api.service.UserProfileService;
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
@RequestMapping("/profiles")
@Tag(name = "Perfis", description = "Endpoints para gestão dos perfis detalhados")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final PagedResourcesAssembler<UserProfileResponse> pagedAssembler;

    public UserProfileController(UserProfileService userProfileService, PagedResourcesAssembler<UserProfileResponse> pagedAssembler) {
        this.userProfileService = userProfileService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Criar perfil de usuário")
    public ResponseEntity<EntityModel<UserProfileResponse>> createUserProfile(@Valid @RequestBody UserProfileRequest request) {
        UserProfileResponse response = userProfileService.createUserProfile(request);
        EntityModel<UserProfileResponse> model = EntityModel.of(response,
                linkTo(methodOn(UserProfileController.class).getUserProfileById(response.id())).withSelfRel(),
                linkTo(methodOn(UserProfileController.class).getAllUserProfiles(null)).withRel("all-profiles")
        );
        return ResponseEntity.created(URI.create("/profiles/" + response.id())).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar perfil por ID")
    public ResponseEntity<EntityModel<UserProfileResponse>> getUserProfileById(@PathVariable Long id) {
        UserProfileResponse response = userProfileService.getUserProfileById(id);
        EntityModel<UserProfileResponse> model = EntityModel.of(response,
                linkTo(methodOn(UserProfileController.class).getUserProfileById(id)).withSelfRel(),
                linkTo(methodOn(UserProfileController.class).getAllUserProfiles(null)).withRel("all-profiles")
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping
    @Operation(summary = "Listar todos os perfis (Paginado)")
    public ResponseEntity<PagedModel<EntityModel<UserProfileResponse>>> getAllUserProfiles(@ParameterObject Pageable pageable) {
        Page<UserProfileResponse> page = userProfileService.getAllUserProfiles(pageable);
        PagedModel<EntityModel<UserProfileResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(UserProfileController.class).getUserProfileById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/search")
    @Operation(summary = "Consulta personalizada: Pesquisar perfis por bio")
    public ResponseEntity<PagedModel<EntityModel<UserProfileResponse>>> searchProfilesByBio(
            @RequestParam String bio, @ParameterObject Pageable pageable) {
        Page<UserProfileResponse> page = userProfileService.searchProfilesByBio(bio, pageable);
        PagedModel<EntityModel<UserProfileResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(UserProfileController.class).getUserProfileById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar perfil de usuário")
    public ResponseEntity<EntityModel<UserProfileResponse>> updateUserProfile(@PathVariable Long id, @Valid @RequestBody UserProfileRequest request) {
        UserProfileResponse response = userProfileService.updateUserProfile(id, request);
        EntityModel<UserProfileResponse> model = EntityModel.of(response,
                linkTo(methodOn(UserProfileController.class).getUserProfileById(id)).withSelfRel()
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir perfil de usuário")
    public ResponseEntity<Void> deleteUserProfile(@PathVariable Long id) {
        userProfileService.deleteUserProfile(id);
        return ResponseEntity.noContent().build();
    }
}