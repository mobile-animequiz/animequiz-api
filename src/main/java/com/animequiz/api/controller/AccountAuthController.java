package com.animequiz.api.controller;
import com.animequiz.api.dto.AuthResponse;
import com.animequiz.api.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/auth")
@Tag(name="Autenticação",description="Sessão própria JWT, Google verificado e recuperação por e-mail")
@ApiResponse(responseCode="429",description="Limite por origem excedido; aguarde Retry-After")
public class AccountAuthController {
    private final GoogleAuthService google;
    private final PasswordResetService passwords;
    public AccountAuthController(GoogleAuthService google,PasswordResetService passwords) { this.google=google; this.passwords=passwords; }
    public record GoogleRequest(@NotBlank @Size(max=16384) @Schema(description="ID token Google assinado; não envie apenas e-mail ou ID de perfil") String idToken) {}
    public record ForgotRequest(@NotBlank @Email @Size(max=254) @Schema(example="jogador@example.com") String email) {}
    public record ResetRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
        @NotBlank @Size(min=8,max=72) @Schema(description="Mínimo 8 caracteres e máximo 72 bytes UTF-8") String password) {}
    @PostMapping("/google")
    @Operation(summary="Entrar com Google",description="Verifica assinatura RS256 por JWKS Google, issuer, audience configurado, expiração, subject e email_verified. Emite JWT próprio. Não vincula automaticamente conta existente pelo e-mail.")
    @ApiResponses({@ApiResponse(responseCode="200",description="JWT próprio e usuário"),@ApiResponse(responseCode="401",description="Identidade inválida"),
        @ApiResponse(responseCode="409",description="Conta existente: autentique por senha e vincule nas configurações"),
        @ApiResponse(responseCode="503",description="Integração não configurada")})
    public AuthResponse google(@Valid @RequestBody GoogleRequest request) { return google.login(request.idToken()); }
    @PostMapping("/google/link") @ResponseStatus(HttpStatus.NO_CONTENT) @SecurityRequirement(name="bearerAuth")
    @Operation(summary="Vincular Google à conta autenticada",description="Requer JWT próprio válido e ID token Google verificado com o mesmo e-mail. Subject não pode pertencer a outra conta.")
    public void link(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody GoogleRequest request) { google.link(Long.valueOf(jwt.getSubject()),request.idToken()); }
    @PostMapping("/forgot-password")
    @Operation(summary="Solicitar link de redefinição",description="Resposta 202 idêntica para e-mails cadastrados e desconhecidos. Token de 256 bits, hash no banco, validade 15 minutos e uso único. Até 3 solicitações por e-mail/hora com resposta genérica; proteção por IP adicional. O envio usa remetente Resend verificado e só acontece após commit.")
    @ApiResponses({@ApiResponse(responseCode="202",description="Solicitação recebida, sem confirmar existência da conta"),@ApiResponse(responseCode="503",description="Serviço indisponível para todas as contas por falta de configuração")})
    public ResponseEntity<Map<String,String>> forgot(@Valid @RequestBody ForgotRequest request) {
        passwords.request(request.email());
        return ResponseEntity.accepted().body(Map.of("message","Se houver uma conta com esse e-mail, você receberá as instruções para redefinir sua senha."));
    }
    @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Definir nova senha",description="Consome o token uma única vez sob bloqueio transacional, salva BCrypt e revoga sessões JWT anteriores. Não autentica automaticamente.")
    @ApiResponses({@ApiResponse(responseCode="204",description="Senha alterada"),@ApiResponse(responseCode="400",description="Link inválido, expirado, reutilizado ou dados inválidos")})
    public void reset(@Valid @RequestBody ResetRequest request) { passwords.reset(request.token(),request.password()); }
}

