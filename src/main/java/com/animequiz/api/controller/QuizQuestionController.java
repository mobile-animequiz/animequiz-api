package com.animequiz.api.controller;

import com.animequiz.api.dto.QuestionWithOptionsResponse;
import com.animequiz.api.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Leitura das questões de um quiz já com as alternativas, para a tela de resolução do aplicativo.
 * Os endpoints {@code GET /questions/by-quiz/{quizId}} e {@code GET /options/by-question/{questionId}}
 * continuam disponíveis e inalterados.
 */
@RestController
@RequestMapping("/quizzes/{quizId}/questions")
@Tag(name = "Quizzes")
public class QuizQuestionController {

    private final QuestionService questionService;
    private final PagedResourcesAssembler<QuestionWithOptionsResponse> pagedAssembler;

    public QuizQuestionController(QuestionService questionService,
                                  PagedResourcesAssembler<QuestionWithOptionsResponse> pagedAssembler) {
        this.questionService = questionService;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    @Operation(
            summary = "Listar as questões de um quiz com suas alternativas (Paginado)",
            description = """
                    Devolve cada questão do quiz já com `options: [{ id, text }]`, pronta para a tela de resolução, \
                    em apenas uma requisição (em vez de uma por questão).

                    - **Sem gabarito:** as alternativas só têm `id` e `text`; `isCorrect` nunca é devolvido.
                    - **Ordem estável:** questões e alternativas em ordem crescente de `id`. Um `sort` informado \
                    pelo cliente é aplicado depois dessa ordem.
                    - **Paginado:** `size` padrão 20 (o aplicativo pede `size=100`).
                    - Quiz sem questões: `200` com a lista vazia. Quiz inexistente: `404`.
                    - Os endpoints `GET /questions/by-quiz/{quizId}` e `GET /options/by-question/{questionId}` \
                    continuam funcionando como antes.

                    Exemplo de item (quiz 1, trecho):

                    ```json
                    {
                      "id": 1,
                      "quizId": 1,
                      "quizTitle": "Naruto Shippuden Quiz",
                      "text": "Qual é o sonho de Naruto?",
                      "options": [
                        { "id": 1, "text": "Ser Hokage" },
                        { "id": 2, "text": "Ser Rei dos Piratas" }
                      ]
                    }
                    ```
                    """)
    public ResponseEntity<PagedModel<EntityModel<QuestionWithOptionsResponse>>> getQuizQuestions(
            @PathVariable Long quizId, @ParameterObject Pageable pageable) {
        Page<QuestionWithOptionsResponse> page = questionService.getQuestionsWithOptionsByQuizId(quizId, pageable);
        PagedModel<EntityModel<QuestionWithOptionsResponse>> model = pagedAssembler.toModel(page,
                res -> EntityModel.of(res, linkTo(methodOn(QuestionController.class).getQuestionById(res.id())).withSelfRel())
        );
        return ResponseEntity.ok(model);
    }
}
