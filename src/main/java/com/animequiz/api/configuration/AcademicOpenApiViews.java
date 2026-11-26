package com.animequiz.api.configuration;

import com.animequiz.api.security.AcademicVersionPolicy;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.*;

/** Curates the public teaching documents; authorization remains enforced in SecurityConfig. */
public final class AcademicOpenApiViews {
    private AcademicOpenApiViews() { }
    private static final Set<String> PUBLIC_SESSION = Set.of("/auth/register", "/auth/login");
    private static final Set<String> SHARED_READS = Set.of("/api/hello", "/api/versioned/status");

    private static boolean visible(String version, String path, PathItem.HttpMethod method) {
        if (AcademicVersionPolicy.V1_PATHS.contains(path) && method == PathItem.HttpMethod.GET) return true;
        if (version.equals("1")) return false;
        if (path.equals("/scores/ranking")) return method == PathItem.HttpMethod.GET;
        if (path.equals("/scores/{id}")) return method == PathItem.HttpMethod.GET;
        if (PUBLIC_SESSION.contains(path)) return method == PathItem.HttpMethod.POST;
        if (path.equals("/users/me") || path.equals("/users/{userId}/profile")) return method == PathItem.HttpMethod.GET;
        if (path.equals("/quizzes/{quizId}/attempts")) return method == PathItem.HttpMethod.POST;
        if (path.equals("/users/me/settings")) return Set.of(PathItem.HttpMethod.GET, PathItem.HttpMethod.PATCH).contains(method);
        if (path.equals("/users/me/avatar")) return Set.of(PathItem.HttpMethod.GET, PathItem.HttpMethod.PUT, PathItem.HttpMethod.DELETE).contains(method);
        if (path.equals("/users/me/favorites")) return method == PathItem.HttpMethod.GET;
        if (path.equals("/users/me/favorites/{quizId}")) return Set.of(PathItem.HttpMethod.PUT, PathItem.HttpMethod.DELETE).contains(method);
        return false;
    }

    public static void apply(OpenAPI api, String version) {
        boolean basic = version.equals("1");
        if (api.getInfo() != null) {
            api.getInfo().setTitle(basic ? "AnimeQuiz API — V1 básica" : "AnimeQuiz API — V2 do jogador");
            api.getInfo().setVersion(version + ".0.0");
            api.getInfo().setDescription(basic
                ? "Consultas públicas de quizzes, perguntas e alternativas. Sem autenticação e sem rate limit nessas consultas. "
                  + "Use X-API-Version: 1 (padrão). Contas, tentativas e administração não fazem parte desta apresentação."
                : """
                    API do jogador. Use `X-API-Version: 2`. Crie sua conta em `POST /auth/register` ou entre em
                    `POST /auth/login`; copie apenas `accessToken` para **Authorize**. O único esquema é JWT Bearer.

                    Consulte o catálogo, jogue, veja o ranking e cuide do próprio perfil, foto e favoritos.
                    O servidor calcula os pontos; não aceite `points`, gabarito ou `userId` fornecidos pelo cliente.
                    Uma tentativa nova retorna 201; repetir a mesma `X-Idempotency-Key` e corpo retorna o mesmo score
                    com 200 e `Idempotency-Replayed: true`. Reusar a chave com outro corpo retorna 409.

                    Usuário comum não pode criar/alterar/excluir quizzes, perguntas, alternativas, contas de terceiros
                    ou pontuações. Esses controles são aplicados na API, mesmo que alguém chame a URL manualmente
                    ou troque o cabeçalho para V1. A criação da própria conta e o envio de respostas são ações do jogador.

                    Rate limit por IP, por instância: padrão global 120 requisições/60 segundos (configurável).
                    Cadastro: 10/900 segundos; login: 20/60 segundos. Excesso retorna 429, Retry-After em segundos,
                    X-RateLimit-* e, nos fluxos de sessão, X-AuthRateLimit-*. Reset é Unix em segundos.
                    Documentação e OPTIONS não consomem o limite global; contadores não são compartilhados entre réplicas.

                    Paginação começa em page=0. Favoritos aceitam size de 1 a 50. JPEG/PNG de até 2 MiB podem ser
                    enviados em multipart/form-data no campo file; o download da foto exige o Bearer da própria conta.
                    As perguntas V2 acrescentam imageUrl, imageAltText e timeLimitSeconds quando disponíveis.
                    Google, recuperação de senha, chaves de API e CRUD administrativo foram ocultados desta apresentação.
                    """);
        }
        api.setSecurity(new ArrayList<>());
        if (api.getComponents() != null) {
            api.getComponents().setSecuritySchemes(basic ? new LinkedHashMap<>() : new LinkedHashMap<>(Map.of(
                "bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                    .description("Informe somente accessToken, sem escrever o prefixo Bearer."))));
        }
        if (api.getPaths() == null) return;
        for (String path : new ArrayList<>(api.getPaths().keySet())) {
            PathItem item = api.getPaths().get(path);
            for (var entry : new ArrayList<>(item.readOperationsMap().entrySet())) {
                if (!visible(version, path, entry.getKey())) item.operation(entry.getKey(), null);
                else customize(entry.getValue(), path, basic, version);
            }
            if (item.readOperations().isEmpty()) api.getPaths().remove(path);
        }
        Set<String> tags = new HashSet<>();
        api.getPaths().values().forEach(item -> item.readOperations().forEach(op -> { if (op.getTags()!=null) tags.addAll(op.getTags()); }));
        if (api.getTags() != null) api.getTags().removeIf(tag -> !tags.contains(tag.getName()));
        if (basic && api.getTags()!=null) api.getTags().forEach(tag -> tag.setDescription(null));
        pruneSchemas(api);
    }

    private static void customize(Operation op, String path, boolean basic, String version) {
        op.setSecurity(new ArrayList<>());
        if (!basic && !PUBLIC_SESSION.contains(path) && !SHARED_READS.contains(path)) {
            op.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
            op.getResponses().putIfAbsent("401", new ApiResponse().description("Token ausente, inválido ou expirado")
                .content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/APIError"))
                    .example(Map.of("status",401,"error","Unauthorized","message","Autenticação necessária ou token inválido/expirado","timestamp","2026-11-25T12:00:00")))));
        }
        if (!basic && (path.equals("/scores/{id}") || path.equals("/users/{userId}/profile"))) {
            op.getResponses().putIfAbsent("403", new ApiResponse().description("Este recurso pertence a outra conta")
                .content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType()
                    .schema(new Schema<>().$ref("#/components/schemas/APIError"))
                    .example(Map.of("status",403,"error","Forbidden","message","Você só pode consultar os próprios dados","timestamp","2026-11-25T12:00:00")))));
        }
        if (!basic && path.equals("/quizzes/{quizId}/attempts") && op.getResponses().containsKey("201")) {
            op.getResponses().putIfAbsent("200", new ApiResponse().description("Tentativa repetida: mesmo resultado, sem duplicar o score")
                .content(op.getResponses().get("201").getContent())
                .addHeaderObject("Idempotency-Replayed", new io.swagger.v3.oas.models.headers.Header()
                    .description("true quando o resultado anterior foi reaproveitado")
                    .schema(new io.swagger.v3.oas.models.media.BooleanSchema().example(true))));
        }
        if (op.getParameters() != null) op.getParameters().removeIf(p ->
            Set.of("Authorization", "X-API-Key", "X-API-Version").contains(p.getName())
            || (basic && "X-Idempotency-Key".equals(p.getName())));
        op.addParametersItem(new Parameter().name("X-API-Version").in("header")
            .description("Versão desta documentação")
            .schema(new StringSchema()._default(version)._enum(List.of(version))));
        if (basic) {
            op.setDescription(null);
            op.getResponses().keySet().removeAll(Set.of("401", "403", "429"));
            op.getResponses().values().forEach(response -> {
                if (response.getHeaders()!=null) response.getHeaders().keySet().removeIf(AcademicOpenApiViews::securityHeader);
                if (response.getContent()!=null) response.getContent().values().forEach(media -> { media.setExample(null); media.setExamples(null); });
            });
        }
    }

    private static boolean securityHeader(String name) {
        return name.startsWith("X-RateLimit-") || name.startsWith("X-AuthRateLimit-")
            || name.equals("Retry-After") || name.equals("WWW-Authenticate");
    }

    /** Keep only schemas reachable from the operations actually shown, including nested references. */
    @SuppressWarnings("unchecked")
    private static void pruneSchemas(OpenAPI api) {
        var components = api.getComponents();
        if (components == null || components.getSchemas() == null) return;
        Map<String,Object> all = Json.mapper().convertValue(components, Map.class);
        Set<String> refs = new LinkedHashSet<>();
        collectRefs(Json.mapper().convertValue(api.getPaths(), Map.class), refs);
        Queue<String> pending = new ArrayDeque<>(refs);
        while (!pending.isEmpty()) {
            String[] pieces = pending.remove().split("/", 4);
            if (pieces.length != 4 || !pieces[1].equals("components")) continue;
            Object group = all.get(pieces[2]);
            if (!(group instanceof Map<?,?> map)) continue;
            Object value = map.get(pieces[3].replace("~1","/").replace("~0","~"));
            Set<String> nested = new LinkedHashSet<>(); collectRefs(value, nested);
            for (String ref : nested) if (refs.add(ref)) pending.add(ref);
        }
        components.getSchemas().keySet().removeIf(name -> !refs.contains("#/components/schemas/" + name));
    }

    private static void collectRefs(Object value, Set<String> refs) {
        if (value instanceof Map<?,?> map) {
            Object ref = map.get("$ref");
            if (ref instanceof String text && text.startsWith("#/components/")) refs.add(text);
            map.values().forEach(item -> collectRefs(item, refs));
        } else if (value instanceof Iterable<?> list) list.forEach(item -> collectRefs(item, refs));
    }
}
