package com.animequiz.api.dto;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
public record AccountSettingsRequest(
    @Size(min=2, max=50) @Schema(example="Isaac", description="Nome de exibição; não altera login, e-mail ou papel") String displayName,
    @Pattern(regexp="midnight|eclipse|ocean|forest") @Schema(example="midnight", allowableValues={"midnight","eclipse","ocean","forest"}) String theme,
    @Pattern(regexp="violet|cyan|rose|amber|mint|coral") @Schema(example="violet", allowableValues={"violet","cyan","rose","amber","mint","coral"}) String accent,
    @Pattern(regexp="pt-BR|en|device") @Schema(example="pt-BR", description="Idioma da interface e do conteúdo traduzido disponível") String locale
) {}

