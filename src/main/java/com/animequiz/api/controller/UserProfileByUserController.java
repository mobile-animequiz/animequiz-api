package com.animequiz.api.controller;

import com.animequiz.api.dto.UserProfileDetailsResponse;
import com.animequiz.api.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.AccessDeniedException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Perfil consultado pelo ID do usuário (contrato usado pelo aplicativo), com nível e estatísticas.
 * Os endpoints {@code /profiles/**} continuam disponíveis, inalterados.
 */
@RestController
@RequestMapping("/users/{userId}/profile")
@Tag(name = "Perfis")
public class UserProfileByUserController {

    private final UserProfileService userProfileService;

    public UserProfileByUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
        @Operation(
            summary = "Buscar o perfil de um usuário, com nível e estatísticas",
            description = """
                    Devolve os campos do perfil (`id`, `userId`, `username`, `bio`, `avatarUrl`) e, calculados na hora \
                    a partir das tentativas do usuário:

                    - **`xp`:** soma da **melhor** pontuação de cada quiz distinto. Todas as tentativas continuam no \
                    histórico, mas repetir um quiz só aumenta o XP se superar a melhor pontuação anterior.
                    - **`level`, `rankTitle`, `levelStartXp`, `nextLevelXp`:** `xpParaIniciar(L) = 50 * L * (L - 1)`, \
                    nível máximo 50. Patentes: Iniciante (1-4), Aprendiz (5-9), Explorador (10-14), Veterano (15-19), \
                    Mestre (20-29), Grão-Mestre (30-39) e Lenda (40-50). No nível 50, `nextLevelXp` vale `127500` e a \
                    barra de progresso deve ser limitada a 100%.
                    - **`completedQuizzes`:** quizzes distintos já respondidos (repetir não aumenta).
                    - **`correctAnswers` e `totalAnswers`:** acertos e respostas **efetivamente enviadas**, somados em \
                    todas as tentativas. `totalAnswers` não é o total de perguntas disponíveis: questões deixadas em \
                    branco numa tentativa parcial não contam. Taxa de acerto = `correctAnswers / totalAnswers`.

                    É somente leitura (não cria nem altera registros). Usuário sem tentativas: `xp = 0`, nível 1, \
                    `nextLevelXp = 100` e contadores zerados. Usuário inexistente: `404`.
                    """)
    public ResponseEntity<EntityModel<UserProfileDetailsResponse>> getProfileByUserId(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isAdmin = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("ADMIN");

        if (!isAdmin && !Long.valueOf(jwt.getSubject()).equals(userId)) {
            throw new AccessDeniedException("Você só pode consultar o próprio perfil");
        }

        UserProfileDetailsResponse response =
                userProfileService.getProfileDetailsByUserId(userId);

        EntityModel<UserProfileDetailsResponse> model = EntityModel.of(
                response,
                linkTo(methodOn(UserProfileByUserController.class)
                        .getProfileByUserId(userId, null)).withSelfRel(),
                linkTo(methodOn(UserProfileController.class)
                        .getUserProfileById(response.id())).withRel("profile")
        );

        return ResponseEntity.ok(model);
    }
}
