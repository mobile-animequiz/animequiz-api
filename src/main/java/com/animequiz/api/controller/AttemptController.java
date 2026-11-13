package com.animequiz.api.controller;

import com.animequiz.api.dto.AttemptRequest;
import com.animequiz.api.dto.AttemptResponse;
import com.animequiz.api.service.IdempotentAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/quizzes/{quizId}/attempts")
@Tag(name = "Quizzes")
public class AttemptController {

    private final IdempotentAttemptService attemptService;

    public AttemptController(IdempotentAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    @Operation(
            summary = "Enviar as respostas de um quiz e obter a pontuação",
            description = """
                    Recebe apenas as alternativas escolhidas. O servidor corrige, calcula a pontuação e registra o score.

                    **Autenticação:** exige `Authorization: Bearer <token>`. O usuário é identificado pelo subject do JWT; não envie `userId`.

                    **Idempotência opcional:** envie `X-Idempotency-Key` (8-128 caracteres). Repetições com chave e corpo iguais retornam o mesmo score (200 e `Idempotency-Replayed: true`), sem duplicar o registro. Reuso da chave com outro corpo retorna 409.

                    **Regra:** pontos = acertos x 10 x peso da dificuldade (`FACIL` = 1, `MEDIO` = 2, `DIFICIL` = 3).

                    - Questões não enviadas contam como erradas.
                    - A resposta informa quantos acertos houve, mas nunca quais questões foram acertadas nem o gabarito.
                    - `400`: questão que não pertence ao quiz, opção que não pertence à questão ou resposta duplicada.
                    - `409`: quiz sem questões.
                    """,
            requestBody = @RequestBody(content = @Content(examples = @ExampleObject(name = "Tentativa", value = """
                    {
                      "answers": [
                        { "questionId": 1, "optionId": 1 },
                        { "questionId": 3, "optionId": 5 },
                        { "questionId": 4, "optionId": 9 }
                      ]
                    }
                    """))))
    @ApiResponse(responseCode = "201", description = "Created: tentativa corrigida e score registrado",
            content = @Content(schema = @Schema(implementation = AttemptResponse.class),
                    examples = @ExampleObject(name = "Resultado", value = """
                            {
                              "scoreId": 4,
                              "userId": 1,
                              "username": "Naruto Uzumaki",
                              "quizId": 1,
                              "quizTitle": "Naruto Shippuden Quiz",
                              "difficulty": "FACIL",
                              "totalQuestions": 3,
                              "answeredQuestions": 3,
                              "correctAnswers": 2,
                              "pointsPerCorrectAnswer": 10,
                              "points": 20,
                              "_links": {
                                "score": { "href": "http://localhost:8080/scores/4" },
                                "quiz": { "href": "http://localhost:8080/quizzes/1" },
                                "ranking": { "href": "http://localhost:8080/scores/ranking" }
                              }
                            }
                            """)))
    public ResponseEntity<EntityModel<AttemptResponse>> submitAttempt(
            @PathVariable Long quizId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AttemptRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        var outcome = attemptService.submit(quizId, Long.parseLong(jwt.getSubject()), request, idempotencyKey);
        AttemptResponse response = outcome.result();
        EntityModel<AttemptResponse> model = EntityModel.of(response,
                linkTo(methodOn(ScoreController.class).getScoreById(response.scoreId())).withRel("score"),
                linkTo(methodOn(QuizController.class).getQuizById(quizId)).withRel("quiz"),
                linkTo(methodOn(ScoreController.class).getRanking(null)).withRel("ranking")
        );
        if (outcome.replayed()) {
            return ResponseEntity.ok().header("Idempotency-Replayed", "true").body(model);
        }
        return ResponseEntity.created(URI.create("/scores/" + response.scoreId())).body(model);
    }
}
