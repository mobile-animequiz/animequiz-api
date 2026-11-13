package com.animequiz.api.configuration;

import com.animequiz.api.dto.APIError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuração central da documentação OpenAPI (Springdoc) da AnimeQuiz API.
 *
 * <p>Concentra, em um único lugar:
 * <ul>
 *   <li>metadados da API (título, descrição, contato, licença, servidores);</li>
 *   <li>a definição dos domínios (tags) usados em {@code @Tag(name = ...)} nos controllers;</li>
 *   <li>um {@link OpenApiCustomizer} que completa as respostas de cada endpoint (201/204 corretos, 400, 404,
 *       409, 415 e 500 com o schema {@link APIError}) <b>somente onde o código realmente as produz</b>:
 *       o 404 e o 409 são decididos operação por operação, e os endpoints removidos (410) documentam só o 410.</li>
 * </ul>
 *
 * <p>Importante: os nomes das tags abaixo precisam ser idênticos aos usados nos controllers.
 * Não declare um {@code @Bean OpenAPI} em paralelo, pois ele sobrepõe estas anotações.
 */
@Configuration
@SecuritySchemes({
        @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT"),
        @SecurityScheme(name = "apiKeyAuth", type = SecuritySchemeType.APIKEY, in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER, paramName = "X-API-Key")
})
@OpenAPIDefinition(
        info = @Info(
                title = "Anime Quiz API",
                version = "1.0.0",
                description = """
                        API REST para quizzes de anime. Gerencia contas, perfis, quizzes, questões,
                        alternativas e pontuações. O servidor corrige as tentativas e calcula os pontos.

                        **Autenticação:** JWT Bearer. Crie uma conta em `POST /auth/register` ou entre em
                        `POST /auth/login`. Rotas privadas exigem `Authorization: Bearer <token>`.
                        O usuário de uma tentativa é identificado pelo token; não envie `userId`.

                        **Permissões:** operações administrativas exigem `ROLE_ADMIN`. Leituras do catálogo
                        e do ranking são públicas; tentativas e dados pessoais exigem autenticação.

                        **Persistência:** H2 em memória, com dados de demonstração carregados na inicialização.
                        Configure `JWT_SECRET` no ambiente antes de iniciar a aplicação.

                        **Pontuação:** `acertos x 10 x peso da dificuldade` (`FACIL` = 1, `MEDIO` = 2,
                        `DIFICIL` = 3). O gabarito não é incluído nas respostas da API.
                        """
        ),
servers = {
        @Server(
                url = "https://animequiz-api-wst8.onrender.com",
                description = "Servidor de produção (Render)"
        ),
        @Server(
                url = "http://localhost:{port}",
                description = "Servidor local para execução e testes",
                variables = @ServerVariable(
                        name = "port",
                        defaultValue = "8080",
                        allowableValues = {"8080", "8081"},
                        description = "Porta HTTP da aplicação (server.port)"
                )
        )
},
        tags = {
                @Tag(name = "Usuários", description = """
                        Cadastro e gestão de usuários.

                        - `username` (3 a 50 caracteres) e `email` são únicos; duplicidade resulta em `409`.
                        - Todo usuário nasce com um perfil (vazio). Um usuário com scores ou com perfil não pode ser removido \
                        (`409`); para remover um usuário sem scores, remova antes o perfil (`DELETE /profiles/{id}`).
                        - O cadastro público de contas é realizado em `POST /auth/register`; o CRUD legado é administrativo.
                        - Consulta personalizada: `GET /users/search?username=` (busca parcial, sem diferenciar maiúsculas).
                        """),
                @Tag(name = "Perfis", description = """
                        Perfil detalhado (bio e avatar) associado 1:1 a um usuário.

                        - Todo usuário tem um perfil, criado (vazio) automaticamente em `POST /users` e nos dados de exemplo; \
                        como cada usuário tem apenas um, um `POST /profiles` para quem já tem perfil resulta em `409`.
                        - `bio` aceita até 255 caracteres e `avatarUrl` até 500.
                        - **Perfil por usuário, com nível e estatísticas:** `GET /users/{userId}/profile`. É somente leitura. \
                        O XP soma a **melhor** pontuação de cada quiz distinto, o nível segue \
                        `xpParaIniciar(L) = 50 * L * (L - 1)` (máximo 50) e as patentes são Iniciante, Aprendiz, Explorador, \
                        Veterano, Mestre, Grão-Mestre e Lenda.
                        - `GET /profiles/{id}` e as demais rotas `/profiles` continuam disponíveis, inalteradas e sem estatísticas.
                        - Consulta personalizada: `GET /profiles/search?bio=`.
                        """),
                @Tag(name = "Quizzes", description = """
                        Questionários temáticos de anime.

                        - O `title` é único, sem diferenciar maiúsculas (`409` se duplicado).
                        - `difficulty` aceita `FACIL`, `MEDIO` ou `DIFICIL`.
                        - `category` é obrigatória em `POST`/`PUT` e aceita `SHONEN`, `SEINEN`, `SHOJO`, `JOSEI` ou `KODOMO`.
                        - Toda resposta de quiz traz `questionCount`, a quantidade de questões **calculada na hora** \
                        (não é armazenada nem enviada pelo cliente).
                        - Leitura para resolver o quiz: `GET /quizzes/{quizId}/questions` (questões com alternativas, sem gabarito).
                        - Consultas personalizadas: `GET /quizzes/search?title=` e `GET /quizzes/difficulty/{difficulty}`.
                        - Resolução do quiz: `POST /quizzes/{quizId}/attempts` corrige as respostas e registra a pontuação \
                        (10 pontos por acerto x peso da dificuldade).
                        - Um quiz com questões, scores ou favoritos não pode ser removido (`409`).
                        """),
                @Tag(name = "Questões", description = """
                        Perguntas que compõem um quiz.

                        - Toda questão pertence a um quiz existente (`quizId`); caso contrário, `404`.
                        - `text` tem de 5 a 255 caracteres.
                        - Uma questão com opções não pode ser removida (`409`).
                        - Consulta personalizada: `GET /questions/by-quiz/{quizId}`.
                        """),
                @Tag(name = "Opções", description = """
                        Alternativas de resposta de uma questão.

                        - Toda opção pertence a uma questão existente (`questionId`); caso contrário, `404`.
                        - `text` tem no máximo 150 caracteres.
                        - `isCorrect` (gabarito) é **somente de entrada**: é gravado em `POST`/`PUT`, mas nenhuma resposta \
                        da API o devolve. A correção acontece no servidor, em `POST /quizzes/{quizId}/attempts`.
                        - Consulta personalizada: `GET /options/by-question/{questionId}`.
                        """),
                @Tag(name = "Scores e Rankings", description = """
                        Pontuações dos usuários nos quizzes e ranking geral.

                        - `POST /scores` e `PUT /scores/{id}` estão **desativados** (`410 Gone`) e aparecem marcados como \
                        obsoletos: o cliente não informa mais a própria pontuação.

                        - A pontuação é calculada no servidor (10 pontos por acerto x peso da dificuldade) e criada por \
                        `POST /quizzes/{quizId}/attempts`.
                        - Cada score guarda os números exatos da tentativa: `correctAnswers` (acertos), `answeredQuestions` \
                        (respostas enviadas; a tentativa pode ser parcial) e `totalQuestions` (questões do quiz na ocasião).
                        - Consulta personalizada: `GET /scores/ranking` (ordem decrescente de pontos).
                        """),
                @Tag(name = "Status", description = "Verificação simples de que a API está no ar")
        },
        externalDocs = @ExternalDocumentation(
                description = "Repositório e instruções de execução",
                url = "https://github.com/mobile-animequiz/animequiz-api"
        )
)
public class OpenApiConfig {

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/APIError";
    private static final String JSON = "application/json";
    private static final String HELLO_PATH = "/api/hello";
    private static final String ATTEMPTS_PATH = "/quizzes/{quizId}/attempts";
    private static final String QUIZ_QUESTIONS_PATH = "/quizzes/{quizId}/questions";
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{(\\w+)}");

    /**
     * Conflitos (409) por operação, no formato "MÉTODO caminho" -> mensagem de exemplo.
     * Só as operações listadas aqui documentam 409: é exatamente onde o serviço lança ConflictException.
     */
    private static final Map<String, String> CONFLICTS = Map.ofEntries(
            Map.entry("POST /users", "Já existe um usuário cadastrado com o e-mail informado"),
            Map.entry("PUT /users/{id}", "O nome de usuário já está em uso"),
            Map.entry("DELETE /users/{id}", "Não é possível deletar usuário que possui pontuações (scores) registradas"),
            Map.entry("POST /profiles", "Já existe um perfil associado a este utilizador."),
            Map.entry("POST /quizzes", "Já existe um quiz cadastrado com o título informado"),
            Map.entry("PUT /quizzes/{id}", "Já existe outro quiz cadastrado com o título informado"),
            Map.entry("DELETE /quizzes/{id}", "Não é possível deletar quiz que possui questões"),
            Map.entry("DELETE /questions/{id}", "Não é possível deletar questão que possui opções de resposta"),
            Map.entry("POST " + ATTEMPTS_PATH, "O quiz 7 não possui questões e não pode ser respondido")
    );

    /**
     * Padroniza as respostas de todos os endpoints sem sobrescrever o que já foi documentado
     * com {@code @ApiResponse} nos controllers (usa sempre {@code putIfAbsent}).
     */
    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
            openApi.setComponents(components);
            components.addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"));

            components.addSecuritySchemes("apiKeyAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                    .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                    .name("X-API-Key"));

            // O APIError não é referenciado por nenhum controller, então precisa ser registrado à mão.
            ModelConverters.getInstance().readAll(APIError.class).forEach(components::addSchemas);

            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, item) ->
                        item.readOperationsMap().forEach((method, operation) -> {
                            boolean keyEndpoint = path.equals("/api-keys/verify");
                            boolean admin = requiresAdmin(path, method);
                            boolean secured = admin || requiresAuthentication(path, method);
                            boolean alreadyHasBearer = operation.getSecurity() != null
                                    && operation.getSecurity().stream().anyMatch(requirement -> requirement.containsKey("bearerAuth"));
                            if (keyEndpoint) {
                                operation.addSecurityItem(new SecurityRequirement().addList("apiKeyAuth"));
                            }
                            if (secured && !alreadyHasBearer) {
                                operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                            }
                            enrich(path, method, operation, secured, admin);
                        }));
            }
        };
    }

    private static void enrich(String path, PathItem.HttpMethod method, Operation operation, boolean secured, boolean admin) {
        ApiResponses responses = operation.getResponses() != null ? operation.getResponses() : new ApiResponses();
        operation.setResponses(responses);

        // Endpoint de verificação: só devolve texto, não há erro de cliente a documentar.
        if (HELLO_PATH.equals(path)) {
            return;
        }

        // Endpoint removido (410): documenta só o 410 e o 500, sem sugerir sucesso, validação ou conflito.
        if (responses.containsKey("410")) {
            responses.remove("200");
            responses.putIfAbsent("500", internalError());
            return;
        }

        boolean create = method == PathItem.HttpMethod.POST && !path.equals("/auth/login");
        boolean delete = method == PathItem.HttpMethod.DELETE;
        boolean hasBody = operation.getRequestBody() != null;
        String key = method.name() + " " + path;

        if (create) {
            // O Springdoc documenta o POST como 200; o controller devolve 201 com cabeçalho Location.
            ApiResponse ok = responses.remove("200");
            if (ok != null) {
                ok.setDescription("Created: recurso criado com sucesso");
                responses.putIfAbsent("201", ok);
            }
            ApiResponse created = responses.get("201");
            if (created != null && (created.getHeaders() == null || !created.getHeaders().containsKey("Location"))) {
                created.addHeaderObject("Location", new Header()
                        .description("URI do recurso recém-criado")
                        .schema(new StringSchema()));
            }
        }
        if (delete) {
            responses.remove("200");
            responses.putIfAbsent("204", new ApiResponse().description("No Content: recurso removido com sucesso"));
        }

        // 400: toda operação pode receber valor inválido (corpo, parâmetro de rota/query ou ordenação).
        responses.putIfAbsent("400", hasBody
                ? error(400, "Bad Request",
                        "Bad Request: corpo ausente, malformado ou com campos inválidos (Bean Validation)",
                        validationExample(path))
                : error(400, "Bad Request",
                        "Bad Request: parâmetro de rota/query inválido ou ausente, ou ordenação (`sort`) por propriedade inexistente",
                        parameterExample(path)));

        String notFound = notFoundExample(method, path);
        if (notFound != null) {
            responses.putIfAbsent("404", error(404, "Not Found",
                    "Not Found: recurso ou vínculo informado não existe", notFound));
        }

        String conflict = CONFLICTS.get(key);
        if (conflict != null) {
            responses.putIfAbsent("409", error(409, "Conflict",
                    delete ? "Conflict: o recurso possui dependentes e não pode ser removido"
                            : "Conflict: duplicidade de valor único ou estado que impede a operação",
                    conflict));
        }

        if (secured) {
            responses.putIfAbsent("401", error(401, "Unauthorized",
                    "Unauthorized: token ausente, inválido ou expirado", "Autenticação necessária ou token inválido/expirado"));
        }
        if (admin || path.equals("/users/{userId}/profile")) {
            responses.putIfAbsent("403", error(403, "Forbidden",
                    "Forbidden: usuário autenticado sem permissão para este recurso", "Você não possui permissão para executar esta operação"));
        }

        if (hasBody) {
            responses.putIfAbsent("415", error(415, "Unsupported Media Type",
                    "Unsupported Media Type: o corpo deve ser enviado como application/json",
                    "Tipo de conteúdo não suportado"));
        }
        responses.putIfAbsent("429", error(429, "Too Many Requests", "Rate limit por IP excedido; consultar Retry-After", "Limite de requisicoes excedido"));
        responses.putIfAbsent("500", internalError());
    }

    private static boolean requiresAdmin(String path, PathItem.HttpMethod method) {
        if (path.startsWith("/auth/") || path.equals(ATTEMPTS_PATH) || path.startsWith("/api-keys")) return false;
        if (path.startsWith("/profiles")) return true;
        if (path.startsWith("/users")) {
            return !path.equals("/users/me") && !path.equals("/users/{userId}/profile");
        }
        return method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT || method == PathItem.HttpMethod.DELETE;
    }

    private static boolean requiresAuthentication(String path, PathItem.HttpMethod method) {
        if ((path.startsWith("/api-keys") && !path.equals("/api-keys/verify")) || path.equals("/users/me") || path.equals("/users/{userId}/profile")
                || path.equals("/quizzes/{quizId}/attempts")
                || (path.startsWith("/scores/") && !path.equals("/scores/ranking"))
                || path.equals("/scores")) return true;
        return path.startsWith("/users") || path.startsWith("/profiles");
    }

    /** Mensagem de 404 da operação, ou {@code null} quando ela nunca consulta um recurso por ID. */
    private static String notFoundExample(PathItem.HttpMethod method, String path) {
        if (ATTEMPTS_PATH.equals(path) || QUIZ_QUESTIONS_PATH.equals(path) || path.equals("/questions/by-quiz/{quizId}")) {
            return "Quiz não encontrado com ID: 99";
        }
        if (path.equals("/options/by-question/{questionId}")) {
            return "Questão não encontrada com ID: 99";
        }
        if (path.equals("/users/{userId}/profile")) {
            return "Usuário não encontrado com ID: 99";
        }
        if (path.endsWith("/{id}")) {
            return switch (firstSegment(path)) {
                case "users" -> "Usuário não encontrado com ID: 99";
                case "profiles" -> "Perfil não encontrado com ID: 99";
                case "quizzes" -> "Quiz não encontrado com ID: 99";
                case "questions" -> "Questão não encontrada com ID: 99";
                case "options" -> "Opção não encontrada com ID: 99";
                case "scores" -> "Score não encontrado com ID: 99";
                default -> null;
            };
        }
        // Criações que referenciam outro recurso pelo corpo (quizId, questionId, userId).
        if (method == PathItem.HttpMethod.POST) {
            return switch (path) {
                case "/questions" -> "Quiz não encontrado com ID: 99";
                case "/options" -> "Questão não encontrada com ID: 99";
                case "/profiles" -> "Utilizador não encontrado com ID: 99";
                default -> null;
            };
        }
        return null;
    }

    private static String validationExample(String path) {
        if (ATTEMPTS_PATH.equals(path)) {
            return "answers: É obrigatório informar ao menos uma resposta";
        }
        return switch (firstSegment(path)) {
            case "users" -> "username: O nome de usuário é obrigatório";
            case "profiles" -> "userId: O ID do utilizador é obrigatório";
            case "questions" -> "text: O texto da questão deve ter entre 5 e 255 caracteres";
            case "options" -> "text: O texto da opção é obrigatório";
            default -> "title: O título é obrigatório";
        };
    }

    private static String parameterExample(String path) {
        if (path.contains("/search")) {
            return "Parâmetro obrigatório ausente: '" + (path.startsWith("/users") ? "username"
                    : path.startsWith("/profiles") ? "bio" : "title") + "'";
        }
        Matcher variable = PATH_VARIABLE.matcher(path);
        if (variable.find()) {
            return "Valor inválido para o parâmetro '" + variable.group(1) + "'";
        }
        return "Propriedade de ordenação inválida: 'xyz'";
    }

    private static String firstSegment(String path) {
        String[] parts = path.split("/");
        return parts.length > 1 ? parts[1] : "";
    }

    private static ApiResponse internalError() {
        return error(500, "Internal Server Error",
                "Internal Server Error: erro inesperado no servidor",
                "Ocorreu um erro interno no servidor");
    }

    private static ApiResponse error(int status, String reason, String description, String message) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("status", status);
        example.put("error", reason);
        example.put("message", message);
        example.put("timestamp", "2026-01-01T12:00:00");

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON, new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .example(example)));
    }
}
