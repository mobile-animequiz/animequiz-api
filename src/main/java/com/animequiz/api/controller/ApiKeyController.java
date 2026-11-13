package com.animequiz.api.controller;

import com.animequiz.api.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api-keys")
@Tag(name = "Recursos avancados", description = "Gerenciamento de chaves de API")
public class ApiKeyController {
    private final ApiKeyService service;
    public ApiKeyController(ApiKeyService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Gerar chave de API", description = "Exige JWT; retorna chave secreta apenas uma vez. O banco armazena apenas SHA-256.")
    public ResponseEntity<ApiKeyService.CreatedKey> create(@AuthenticationPrincipal Jwt jwt) {
        var created = service.create(Long.parseLong(jwt.getSubject()));
        return ResponseEntity.created(URI.create("/api-keys/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar minhas chaves de forma paginada", description = "Exige JWT; nunca exibe o segredo da chave.")
    public Page<ApiKeyService.KeyInfo> list(@AuthenticationPrincipal Jwt jwt, @ParameterObject Pageable pageable) {
        return service.list(Long.parseLong(jwt.getSubject()), pageable);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revogar minha chave", description = "Exige JWT; apos a revogacao, a chave nao autentica mais.")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.revoke(Long.parseLong(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    @Operation(summary = "Endpoint protegido por X-API-Key", description = "Envie uma chave gerada no header X-API-Key; nao requer JWT.")
    public ApiKeyService.KeyInfo verify(@RequestHeader(value = "X-API-Key", required = false) String raw) {
        return service.verify(raw);
    }
}
