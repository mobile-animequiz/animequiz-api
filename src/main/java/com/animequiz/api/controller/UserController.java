package com.animequiz.api.controller;

import com.animequiz.api.dto.UserRequest;
import com.animequiz.api.dto.UserResponse;
import com.animequiz.api.dto.AuthUserResponse;
import com.animequiz.api.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.animequiz.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Endpoints responsáveis pela gestão de usuários do AnimeQuiz")
public class UserController {

    private final UserService userService;
    private final PagedResourcesAssembler<UserResponse> pagedAssembler;
    private final AuthService authService;

    public UserController(UserService userService, PagedResourcesAssembler<UserResponse> pagedAssembler, AuthService authService) {
        this.userService = userService;
        this.pagedAssembler = pagedAssembler;
        this.authService = authService;
    }


    @GetMapping("/me")
        @Operation(summary = "Consultar usuário autenticado", description = "Retorna os dados da conta identificada pelo subject do JWT.")
    public ResponseEntity<AuthUserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.currentUser(Long.parseLong(jwt.getSubject())));
    }

    @PostMapping
    @Operation(summary = "Criar usuário", description = "Valida os dados e cadastra um novo usuário no sistema. " +
            "Operação administrativa legada; o cadastro de contas deve usar `POST /auth/register`.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail ou nome de usuário já cadastrado")
    })
    public ResponseEntity<EntityModel<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        EntityModel<UserResponse> model = EntityModel.of(response,
                linkTo(methodOn(UserController.class).getUserById(response.id())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers(null)).withRel("all-users")
        );
        return ResponseEntity.created(URI.create("/users/" + response.id())).body(model);
    }

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Retorna uma lista paginada de usuários com HATEOAS.")
    @ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso")
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> getAllUsers(@ParameterObject Pageable pageable) {
        Page<UserResponse> page = userService.getAllUsers(pageable);
        PagedModel<EntityModel<UserResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(UserController.class).getUserById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados de um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<EntityModel<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        EntityModel<UserResponse> model = EntityModel.of(response,
                linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers(null)).withRel("all-users"),
                linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"),
                linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete")
        );
        return ResponseEntity.ok(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail ou nome de usuário já usado por outro usuário")
    })
    public ResponseEntity<EntityModel<UserResponse>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UserRequest request) {
        UserResponse response = userService.updateUser(id, request);

        EntityModel<UserResponse> model = EntityModel.of(response,
                linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers(null)).withRel("all-users")
        );
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário", description = "Remove um usuário que não possui scores nem perfil.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Usuário possui pontuações ou perfil vinculados")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Consulta personalizada: Pesquisar usuários por nome de usuário")
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> searchByUsername(
            @RequestParam String username, @ParameterObject Pageable pageable) {
        Page<UserResponse> page = userService.searchByUsername(username, pageable);
        PagedModel<EntityModel<UserResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(UserController.class).getUserById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }
}