package com.animequiz.api.configuration;

import com.animequiz.api.dto.APIError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.ServerVariable;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
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
import java.util.Set;

/**
 * Configuração central da documentação OpenAPI (Springdoc) da AnimeQuiz API.
 *
 * <p>Concentra, em um único lugar:
 * <ul>
 *   <li>metadados da API (título, descrição, contato, licença, servidores);</li>
 *   <li>a definição dos 6 domínios (tags) usados em {@code @Tag(name = ...)} nos controllers;</li>
 *   <li>um {@link OpenApiCustomizer} que padroniza os códigos de resposta de todos os endpoints
 *       (201/204 corretos, 400, 404, 409 e 500 com o schema {@link APIError}).</li>
 * </ul>
 *
 * <p>Importante: os nomes das tags abaixo precisam ser idênticos aos usados nos controllers.
 * Não declare um {@code @Bean OpenAPI} em paralelo, pois ele sobrepõe estas anotações.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AnimeQuiz RESTful API",
                version = "1.0.0",
                description = """
                        API RESTful para gestão de **quizzes de anime**: usuários e perfis, quizzes, questões, \
                        alternativas de resposta e pontuações com ranking. Construída com Spring Boot, Spring Data JPA \
                        (H2), Bean Validation, Spring HATEOAS e Springdoc OpenAPI.

                        ## Visão geral

                        - **Estilo:** REST com hipermídia (HATEOAS / HAL). Navegue pelos links em `_links` em vez de montar URLs à mão.
                        - **Formato:** JSON em UTF-8.
                        - **Autenticação:** nenhuma nesta versão (escopo acadêmico, uso local).
                        - **Persistência:** H2 em memória. Os dados de exemplo são recarregados a cada reinício.
                        - **Identificadores:** inteiros longos (`Long`) gerados pelo banco.
                        - **Enum `Difficulty`:** `FACIL`, `MEDIO` ou `DIFICIL`.

                        ## Modelo de domínio

                        | Relação | Tipo | Descrição |
                        |---|---|---|
                        | Usuário ↔ Perfil | One-to-One | Cada usuário possui no máximo um perfil |
                        | Quiz → Questão | One-to-Many | Um quiz agrupa várias questões |
                        | Questão → Opção | One-to-Many | Cada questão tem várias alternativas |
                        | Usuário → Score | One-to-Many | Um usuário acumula várias pontuações |
                        | Quiz → Score | One-to-Many | Cada pontuação é registrada em um quiz |
                        | Usuário ↔ Quiz | Many-to-Many | Quizzes favoritos do usuário |

                        ## Recursos

                        | Recurso | Caminho base | Consulta personalizada |
                        |---|---|---|
                        | Usuários | `/users` | `GET /users/search?username=` |
                        | Perfis | `/profiles` | `GET /profiles/search?bio=` |
                        | Quizzes | `/quizzes` | `GET /quizzes/search?title=` e `GET /quizzes/difficulty/{difficulty}` |
                        | Questões | `/questions` | `GET /questions/by-quiz/{quizId}` |
                        | Opções | `/options` | `GET /options/by-question/{questionId}` |
                        | Scores | `/scores` | `GET /scores/ranking` |

                        Cada recurso oferece o CRUD completo: `POST`, `GET /{id}`, `GET` (paginado), `PUT /{id}` e `DELETE /{id}`.

                        ## Paginação e ordenação

                        Toda listagem é paginada com os parâmetros de query abaixo.

                        | Parâmetro | Padrão | Descrição |
                        |---|---|---|
                        | `page` | `0` | Índice da página (base zero) |
                        | `size` | `20` | Quantidade de itens por página |
                        | `sort` | n/a | `campo,asc` ou `campo,desc` (pode ser repetido) |

                        A resposta paginada traz os itens em `_embedded`, os links de navegação em `_links` \
                        (`first`, `prev`, `self`, `next`, `last`) e os metadados em `page` \
                        (`size`, `totalElements`, `totalPages`, `number`).

                        ## Hipermídia (HATEOAS)

                        Toda resposta de sucesso com corpo inclui `_links`. Conforme o recurso, você encontra `self`, \
                        `update`, `delete` e `all-<recurso>`, que levam às demais operações disponíveis.

                        ## Erros

                        Todos os erros seguem o mesmo formato (schema `APIError`):

                        ```json
                        {
                          "status": 404,
                          "error": "Not Found",
                          "message": "Quiz não encontrado com ID: 99",
                          "timestamp": "2026-10-09T10:15:30.123"
                        }
                        ```

                        Em falhas de validação, `message` lista os campos inválidos no formato `campo: mensagem`, \
                        separados por `;`.

                        ## Códigos de status HTTP

                        | Código | Significado | Quando ocorre |
                        |---|---|---|
                        | `200 OK` | Sucesso | Consultas e atualizações |
                        | `201 Created` | Criado | Criação; o cabeçalho `Location` aponta para o novo recurso |
                        | `204 No Content` | Sem conteúdo | Exclusão concluída |
                        | `400 Bad Request` | Requisição inválida | Falha de validação dos dados enviados |
                        | `404 Not Found` | Não encontrado | Recurso ou vínculo (por exemplo, `quizId`) inexistente |
                        | `409 Conflict` | Conflito | Duplicidade (e-mail, username, título, perfil) ou exclusão de recurso com dependentes |
                        | `500 Internal Server Error` | Erro inesperado | Falha interna não prevista |
                        """,
                contact = @Contact(
                        name = "Equipe de Desenvolvimento AnimeQuiz",
                        email = "suporte@animequiz.api",
                        url = "https://github.com/mobile-animequiz/animequiz-api"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:{port}",
                        description = "Desenvolvimento local (H2 em memória; console em /h2-console)",
                        variables = @ServerVariable(
                                name = "port",
                                defaultValue = "8080",
                                allowableValues = {"8080", "8081"},
                                description = "Porta HTTP da aplicação (server.port)"
                        )
                ),
                @Server(
                        url = "https://api.animequiz.example",
                        description = "Homologação/Produção (placeholder: substitua pela URL real ou remova)"
                )
        },
        tags = {
                @Tag(name = "Usuários", description = """
                        Cadastro e gestão de usuários.

                        - `username` (3 a 50 caracteres) e `email` são únicos; duplicidade resulta em `409`.
                        - Um usuário com scores registrados não pode ser removido (`409`).
                        - Consulta personalizada: `GET /users/search?username=` (busca parcial, sem diferenciar maiúsculas).
                        """),
                @Tag(name = "Perfis de Utilizador", description = """
                        Perfil detalhado (bio e avatar) associado 1:1 a um usuário.

                        - Cada usuário pode ter apenas um perfil; um segundo `POST` resulta em `409`.
                        - `bio` aceita até 255 caracteres e `avatarUrl` até 500.
                        - Consulta personalizada: `GET /profiles/search?bio=`.
                        """),
                @Tag(name = "Quizzes", description = """
                        Questionários temáticos de anime.

                        - O `title` é único, sem diferenciar maiúsculas (`409` se duplicado).
                        - `difficulty` aceita `FACIL`, `MEDIO` ou `DIFICIL`.
                        - Consultas personalizadas: `GET /quizzes/search?title=` e `GET /quizzes/difficulty/{difficulty}`.
                        """),
                @Tag(name = "Questões", description = """
                        Perguntas que compõem um quiz.

                        - Toda questão pertence a um quiz existente (`quizId`); caso contrário, `404`.
                        - Consulta personalizada: `GET /questions/by-quiz/{quizId}`.
                        """),
                @Tag(name = "Opções", description = """
                        Alternativas de resposta de uma questão.

                        - Toda opção pertence a uma questão existente (`questionId`); caso contrário, `404`.
                        - O campo booleano indica se a alternativa é a correta.
                        - Consulta personalizada: `GET /options/by-question/{questionId}`.
                        """),
                @Tag(name = "Scores e Rankings", description = """
                        Pontuações dos usuários nos quizzes e ranking geral.

                        - `points` não pode ser negativo; `userId` e `quizId` precisam existir (`404`).
                        - Consulta personalizada: `GET /scores/ranking` (ordem decrescente de pontos).
                        """)
        },
        externalDocs = @ExternalDocumentation(
                description = "Repositório e instruções de execução",
                url = "https://github.com/mobile-animequiz/animequiz-api"
        )
)
public class OpenApiConfig {

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/APIError";
    private static final String JSON = "application/json";

    /** Criações que não consultam outro recurso, portanto nunca respondem 404. */
    private static final Set<String> CREATE_WITHOUT_LOOKUP = Set.of("/users", "/quizzes");

    /**
     * Padroniza as respostas de todos os endpoints sem sobrescrever o que já foi documentado
     * com {@code @ApiResponse} nos controllers (usa sempre {@code putIfAbsent}).
     */
    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
            openApi.setComponents(components);

            // O APIError não é referenciado por nenhum controller, então precisa ser registrado à mão.
            ModelConverters.getInstance().readAll(APIError.class).forEach(components::addSchemas);

            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, item) ->
                        item.readOperationsMap().forEach((method, operation) -> enrich(path, method, operation)));
            }
        };
    }

    private static void enrich(String path, PathItem.HttpMethod method, Operation operation) {
        ApiResponses responses = operation.getResponses() != null ? operation.getResponses() : new ApiResponses();
        operation.setResponses(responses);

        boolean create = method == PathItem.HttpMethod.POST;
        boolean update = method == PathItem.HttpMethod.PUT;
        boolean delete = method == PathItem.HttpMethod.DELETE;

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
        if (create || update) {
            responses.putIfAbsent("400", error(400, "Bad Request",
                    "Bad Request: dados inválidos (Bean Validation)",
                    "title: O título é obrigatório"));
        }
        if (path.contains("{") || ((create || update) && !CREATE_WITHOUT_LOOKUP.contains(path))) {
            responses.putIfAbsent("404", error(404, "Not Found",
                    "Not Found: recurso ou vínculo informado não existe",
                    "Quiz não encontrado com ID: 99"));
        }
        if (create || update || delete) {
            responses.putIfAbsent("409", error(409, "Conflict",
                    "Conflict: duplicidade ou recurso com dependentes",
                    "Já existe um quiz cadastrado com o título: Naruto Shippuden Quiz"));
        }
        responses.putIfAbsent("500", error(500, "Internal Server Error",
                "Internal Server Error: erro inesperado no servidor",
                "Ocorreu um erro interno no servidor"));
    }

    private static ApiResponse error(int status, String reason, String description, String message) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("status", status);
        example.put("error", reason);
        example.put("message", message);
        example.put("timestamp", "2026-10-09T10:15:30.123");

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON, new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))
                        .example(example)));
    }
}