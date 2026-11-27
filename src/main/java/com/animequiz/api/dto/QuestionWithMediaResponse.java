package com.animequiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Questão V2. Preserva os campos V1 e acrescenta mídia e tempo sugerido, sem gabarito.")
public record QuestionWithMediaResponse(
        Long id, Long quizId, String quizTitle, String text,
        List<OptionSummaryResponse> options,
        @Schema(description = "URL HTTPS opcional da imagem relacionada ao anime; pode ser null.") String imageUrl,
        @Schema(description = "Descrição acessível da imagem, sem revelar a resposta.") String imageAltText,
        @Schema(description = "Tempo sugerido no app, em segundos. Não é validado como prazo pelo servidor.", example = "30")
        int timeLimitSeconds,
        @Schema(description = "Actual content language; untranslated custom questions fall back as a whole to pt-BR")
        String contentLanguage
) {
    public QuestionWithMediaResponse(Long id, Long quizId, String quizTitle, String text,
            List<OptionSummaryResponse> options, String imageUrl, String imageAltText, int seconds) {
        this(id, quizId, quizTitle, text, options, imageUrl, imageAltText, seconds, "pt-BR");
    }
}
