package com.animequiz.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/versioned/status")
@Tag(name = "Recursos avancados", description = "Versionamento por cabecalho e protecao por chave de API")
public class VersionController {
    @GetMapping
    @Operation(summary = "Demonstrar duas versoes de um endpoint",
            description = "Envie X-API-Version: 1 ou X-API-Version: 2. Sem cabecalho usa a v1.")
    @ApiResponse(responseCode = "400", description = "Versao nao suportada")
    public ResponseEntity<?> status(
            @Parameter(description = "Versao da API: 1 ou 2", example = "2")
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String version) {
        return switch (version) {
            case "1" -> ResponseEntity.ok().header("X-API-Version", "1")
                    .body(Map.of("status", "ok", "version", "1"));
            case "2" -> ResponseEntity.ok().header("X-API-Version", "2")
                    .body(Map.of("status", "ok", "version", "2", "service", "animequiz-api", "features", "advanced-api"));
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "X-API-Version deve ser 1 ou 2"));
        };
    }
}
