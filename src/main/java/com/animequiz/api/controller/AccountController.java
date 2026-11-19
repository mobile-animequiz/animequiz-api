package com.animequiz.api.controller;

import com.animequiz.api.dto.*;
import com.animequiz.api.service.AccountService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.data.domain.Page;

@RestController @Validated
@RequestMapping("/users/me")
@Tag(name="Minha conta",description="Preferências, foto e favoritos do titular do JWT; não aceita ID de outro usuário")
@SecurityRequirement(name="bearerAuth")
@ApiResponses({@ApiResponse(responseCode="401",description="Sessão ausente, expirada ou revogada"),
 @ApiResponse(responseCode="400",description="Dados inválidos"),@ApiResponse(responseCode="429",description="Limite de requisições excedido; respeite Retry-After")})
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) { this.accounts=accounts; }
    private Long id(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
    @GetMapping("/settings")
    @Operation(summary="Ler preferências",description="Padrão: pt-BR, midnight e violet. Nome de exibição não muda o login. Dados persistidos no banco configurado.")
    public AccountSettingsResponse read(@AuthenticationPrincipal Jwt jwt) { return accounts.read(id(jwt)); }
    @PatchMapping("/settings")
    @Operation(summary="Alterar nome de exibição, tema, destaque e idioma",description="PATCH parcial. Campos omitidos são preservados; tema midnight/eclipse, destaque violet/cyan/rose, idioma pt-BR/en/device.")
    public AccountSettingsResponse update(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AccountSettingsRequest request) {
        return accounts.update(id(jwt),request);
    }
    @PutMapping(value="/avatar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary="Enviar foto da galeria",description="Campo multipart file: JPEG/PNG, até 2 MiB, dimensões até 4096 e até 8 milhões de pixels. Conteúdo validado no servidor, independente do nome/MIME informado.")
    @ApiResponses({@ApiResponse(responseCode="200",description="Foto salva e versão incrementada"),@ApiResponse(responseCode="413",description="Arquivo excede limite multipart")})
    public AccountSettingsResponse upload(@AuthenticationPrincipal Jwt jwt,@RequestPart("file") MultipartFile file) {
        return accounts.upload(id(jwt),file);
    }
    @GetMapping("/avatar")
    @Operation(summary="Baixar a própria foto",description="Requer Bearer também no carregamento da imagem; Cache-Control private/no-store e ETag por versão. Não inclui token na URL.")
    @ApiResponse(responseCode="404",description="Ainda não há foto")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal Jwt jwt) {
        var avatar=accounts.avatar(id(jwt));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(avatar.type()))
            .cacheControl(CacheControl.noStore()).header("Cache-Control","private, no-store")
            .eTag(Long.toString(avatar.version())).body(avatar.bytes());
    }
    @DeleteMapping("/avatar") @Operation(summary="Remover a própria foto")
    public AccountSettingsResponse deleteAvatar(@AuthenticationPrincipal Jwt jwt) { return accounts.deleteAvatar(id(jwt)); }
    @GetMapping("/favorites")
    @Operation(summary="Listar meus favoritos",description="Página zero-based, size 1–50; ordenação estável por ID. Lista apenas favoritos do titular do JWT.")
    public FavoritePage favorites(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(50) int size) {
        return FavoritePage.of(accounts.favorites(id(jwt),page,size));
    }
    @PutMapping("/favorites/{quizId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Favoritar quiz",description="Idempotente: repetir a mesma operação não duplica o favorito.")
    @ApiResponse(responseCode="404",description="Quiz inexistente")
    public void favorite(@AuthenticationPrincipal Jwt jwt,@PathVariable @Positive Long quizId) { accounts.favorite(id(jwt),quizId,true); }
    @DeleteMapping("/favorites/{quizId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary="Desfavoritar quiz",description="Idempotente para quiz existente; favoritos de outras contas não são alterados.")
    public void unfavorite(@AuthenticationPrincipal Jwt jwt,@PathVariable @Positive Long quizId) { accounts.favorite(id(jwt),quizId,false); }
}

