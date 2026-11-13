# Anime Quiz API — Parte 2: recursos avançados

> Implementação de estudo para execução **em uma única instância** com banco H2 em memória. Esta versão não substitui validação em ambiente real. As rotas do mobile com JWT foram mantidas.

## Como executar

1. Configure `JWT_SECRET` com uma string segura de no mínimo 32 bytes (não publique a chave no Git).
2. No Windows, na raiz do projeto, execute `mvnw.cmd spring-boot:run`. No Linux/macOS, `./mvnw spring-boot:run`.
3. Abra `http://localhost:8080/swagger-ui/index.html` e `http://localhost:8080/api-docs`.
4. Testes: `mvnw.cmd test` (Windows) ou `./mvnw test`.

## Demonstrações — conceito, implementação e resposta

### 1. Versionamento — `X-API-Version`

- `GET /api/versioned/status` sem header: versão `1` (compatibilidade padrão).
- `GET /api/versioned/status` com `X-API-Version: 2`: versão `2`, com campos adicionais.
- Com versão `3`: `400`. Uma mesma rota tem contratos distintos controlados pelo header.

### 2. CORS — origens confiáveis

- `CorsConfig` aplica `CorsConfigurationSource` no Spring Security.
- `CORS_ALLOWED_ORIGINS` recebe URLs separadas por vírgula; padrão apenas para desenvolvimento: `http://localhost:19006,http://localhost:8081`.
- Métodos `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` e headers necessários (`Authorization`, `X-API-Key`, `X-Idempotency-Key`, `X-API-Version`).
- O Expo/React Native **nativo** não depende das regras CORS do navegador; o navegador web, sim.

### 3. Rate limiting — HTTP 429

- `RateLimitFilter`: janela fixa por endereço IP remoto, por instância.
- Padrão `120` requisições por `60` segundos; altere por `RATE_LIMIT_REQUESTS` e `RATE_LIMIT_WINDOW_SECONDS`.
- Headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`; ao estourar, `429` e `Retry-After`.
- Para demonstrar rapidamente, defina `RATE_LIMIT_REQUESTS=3` e execute 4 GETs em `/api/hello` em um minuto. Não use valores baixos durante testes do mobile.
- Limitação: na hospedagem atrás de proxy, `getRemoteAddr` pode representar o proxy; não confiar indiscriminadamente em `X-Forwarded-For`. Para produção distribuída, usar gateway/Redis.

### 4. API Key — `X-API-Key`

- Autentique por JWT no Swagger (Authorize, Bearer).
- `POST /api-keys`: cria chave aleatória (32 bytes), retorna `apiKey` **uma única vez**; salva apenas SHA-256 no H2.
- `GET /api-keys?page=0&size=10`: lista chaves do próprio usuário sem revelar seu valor.
- `GET /api-keys/verify` com `X-API-Key`: exemplo de recurso protegido pela chave. Sem chave/revogada: `401`.
- `DELETE /api-keys/{id}`: revoga a chave (JWT do proprietário obrigatório), retorno `204`.
- JWT e API Key são **mecanismos diferentes**: JWT autentica a conta no app; a API Key demonstra acesso a um recurso dedicado. Não cole a chave em repositórios.

### 5. Idempotência — `X-Idempotency-Key`

- Primeiro, faça login; `POST /quizzes/{quizId}/attempts` com JWT, JSON de `answers` válido e header `X-Idempotency-Key: teste-tentativa-0001`.
- Primeira requisição: `201`, score gravado.
- Repita **a mesma requisição**, com mesmo usuário, mesmo header e JSON: `200`, `Idempotency-Replayed: true`, **mesmo `scoreId`**.
- Repita a chave com respostas diferentes: `409 Conflict`.
- Sem header: permanece o comportamento antigo, com nova tentativa e retorno `201` (compatibilidade com mobile).
- Registro de idempotência é persistido no H2 em memória e sobrevive a requisições repetidas, mas não ao reinício da base.
- A implementação usa sincronização por processo e transação completa: para múltiplas instâncias seria necessário armazenamento distribuído/locking distribuído.

## Dúvidas do professor — respostas curtas

- **Por que JWT e API Key?** JWT identifica o usuário logado e expira; API Key identifica uma credencial revogável para consumir um endpoint específico.
- **Idempotência é igual a não duplicar toda requisição?** Não: aqui o `POST` protegido por chave devolve o resultado anterior em repetição. Outros POSTs ainda não têm essa proteção.
- **Onde está o rate limit?** `RateLimitFilter`, que conta requisições por IP e retorna 429 com `Retry-After`.
- **O que é CORS?** Política aplicada pelo navegador que autoriza origens, métodos e headers; não substitui autenticação.
- **Como versiona?** `X-API-Version` na mesma URL; versão 1 e 2 têm contratos diferentes.
- **Quem calcula a pontuação?** `AttemptService`/`ScoreCalculator` no servidor. O cliente só envia a alternativa escolhida.
- **Por que H2?** Simplifica a avaliação. É banco em memória: dados podem desaparecer após reiniciar.

## Atenção antes de publicar

- Não sobrescreva o deploy do Render sem executar os testes no IntelliJ e validar o fluxo do aplicativo.
- API keys e idempotência são demonstrativos de instância única (H2 e memória de processo).
- Ainda não foi produzida cobertura para concorrência entre múltiplas instâncias nem Postgres/Redis.
- CORS de produção deve ser ajustado a uma origem web real; não use `*` como atalho.
