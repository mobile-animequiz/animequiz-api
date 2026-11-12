package com.animequiz.api.controller;

import com.animequiz.api.dto.AuthResponse;
import com.animequiz.api.dto.LoginRequest;
import com.animequiz.api.dto.RegisterRequest;
import com.animequiz.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e emissão de token JWT")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Conta criada e token emitido"), @ApiResponse(responseCode = "400", description = "Dados inválidos"), @ApiResponse(responseCode = "409", description = "E-mail ou username já cadastrado")})
    @Operation(summary = "Cadastrar conta e autenticar", description = "Cria usuário comum, armazena a senha com BCrypt e retorna JWT. dateOfBirth é opcional (ISO yyyy-MM-dd).")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.created(URI.create("/users/" + response.user().id())).body(response);
    }

    @PostMapping("/login")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Login realizado"), @ApiResponse(responseCode = "400", description = "Dados inválidos"), @ApiResponse(responseCode = "401", description = "Credenciais inválidas")})
    @Operation(summary = "Entrar com e-mail e senha", description = "Valida as credenciais e retorna um JWT Bearer com expiração configurada.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

}
