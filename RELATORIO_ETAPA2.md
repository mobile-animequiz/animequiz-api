# Anime Quiz API 

## Escopo

Implementação de cadastro e login com Spring Security, BCrypt e JWT, proteção das rotas, identificação do usuário autenticado nas tentativas e atualização da documentação OpenAPI. O trabalho foi realizado somente na cópia `animequiz-api-etapa2/animequiz-api-main` extraída do ZIP original.

O banco continua em H2. Não houve migração para PostgreSQL. Nenhum arquivo do aplicativo mobile foi alterado.

## Implementações

- Cadastro público em `POST /auth/register`.
- Login em `POST /auth/login`.
- Consulta da conta autenticada em `GET /users/me`.
- Hash de senha com BCrypt (custo 12); a senha em texto puro não é persistida nem devolvida.
- JWT assinado com HS256, emissor validado, `sub` numérico com o ID do usuário, claim `roles` e expiração.
- `POST /quizzes/{quizId}/attempts` usa o `sub` do JWT. `AttemptRequest` não possui mais `userId`; campos desconhecidos são ignorados para que clientes antigos não consigam controlar a identidade.
- Perfil detalhado em `/users/{userId}/profile` limitado ao próprio usuário ou a `ADMIN`.
- Operações administrativas de usuários, perfis, quizzes, questões, opções e scores protegidas por `ROLE_ADMIN`.
- Leituras de catálogo e ranking permanecem públicas; consulta de scores individuais exige autenticação.
- Erros de autenticação/autorização retornam `APIError` com HTTP 401/403.
- OpenAPI declara o esquema Bearer JWT e as exigências de autenticação por operação.
- `dateOfBirth` é `LocalDate` opcional: ausente ou `null` permanece nulo; quando informado deve ser uma data passada no formato ISO `yyyy-MM-dd`.
- Usuários de demonstração em `data.sql` continuam sem senha e não recebem senha ou data fictícia.

A lógica de correção, pontuação, XP, níveis, estatísticas, histórico e ranking não foi reescrita. A alteração funcional em `AttemptService` limita-se a receber o ID autenticado como argumento separado do DTO.

## Contratos HTTP

### `POST /auth/register`

Request:

```json
{
  "username": "Usuario Exemplo",
  "email": "usuario@example.com",
  "password": "SENHA_FORTE_AQUI",
  "dateOfBirth": "2005-04-20"
}
```

`dateOfBirth` pode ser omitido ou `null`.

Success: `201 Created`, com `Location: /users/{id}` e corpo:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 4,
    "username": "Usuario Exemplo",
    "email": "usuario@example.com",
    "dateOfBirth": "2005-04-20"
  }
}
```

Erros principais: `400` validação; `409` e-mail ou username duplicado.

### `POST /auth/login`

Request:

```json
{
  "email": "usuario@example.com",
  "password": "SENHA_FORTE_AQUI"
}
```

Success: `200 OK`, com o mesmo formato de resposta de cadastro. Credenciais inválidas: `401 Unauthorized`.

### `GET /users/me`

Header:

```http
Authorization: Bearer <accessToken>
```

Success: `200 OK`:

```json
{
  "id": 4,
  "username": "Usuario Exemplo",
  "email": "usuario@example.com",
  "dateOfBirth": "2005-04-20"
}
```

Token ausente, inválido ou expirado: `401 Unauthorized`.

### `POST /quizzes/{quizId}/attempts`

Exige Bearer JWT. O corpo contém somente:

```json
{
  "answers": [
    { "questionId": 1, "optionId": 1 },
    { "questionId": 3, "optionId": 5 }
  ]
}
```

O usuário é obtido do `sub` do JWT. `userId` enviado no JSON não participa da associação da tentativa.

## Segurança e permissões

| Recurso/operação | Acesso |
|---|---|
| `POST /auth/register`, `POST /auth/login` | Público |
| `GET /quizzes/**`, `GET /questions/**`, `GET /options/**` | Público |
| `GET /scores/ranking` | Público |
| `POST /quizzes/{id}/attempts` | Usuário autenticado |
| `GET /users/me` | Usuário autenticado |
| `GET /users/{id}/profile` | Próprio usuário ou ADMIN |
| `GET /scores`, `GET /scores/{id}` | Usuário autenticado |
| CRUD administrativo de usuários/perfis/quizzes/questões/opções/scores | ADMIN |
| Swagger/OpenAPI e `/api/hello` | Público |

Novas contas recebem `USER`. Não há endpoint público para promover usuários. Para criar um administrador local: registre a conta, execute no H2 `UPDATE tb_users SET user_role = 'ADMIN' WHERE email = 'admin@example.com';` e faça login novamente para emitir um token com a role atualizada.

Os usuários antigos de demonstração não têm `password_hash`; eles não podem fazer login até que um administrador atribua uma senha por um procedimento confiável. Não foi gerada senha padrão.

## Dependências adicionadas

| Dependência | Escopo | Finalidade |
|---|---|---|
| `spring-boot-starter-security` | Produção | Spring Security, cadeia de filtros, autorização e BCrypt |
| `spring-boot-starter-oauth2-resource-server` | Produção | Validação de JWT Bearer e suporte JOSE/Nimbus para assinatura HS256 |
| `spring-security-test` | Teste | Post-processors JWT e integração dos testes MockMvc com Spring Security |

Não foram adicionadas bibliotecas JWT de terceiros separadas. O starter Resource Server já fornece o suporte JOSE necessário.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `JWT_SECRET` | Obrigatória | Chave HS256 com pelo menos 32 bytes, fornecida pelo ambiente |
| `JWT_EXPIRATION_SECONDS` | `3600` | Expiração do token em segundos; deve ser maior que zero |
| `JWT_ISSUER` | `animequiz-api` | Emissor validado pelo decoder |
| `H2_CONSOLE_ENABLED` | `false` | Console H2 desabilitado por padrão; habilite apenas no desenvolvimento local |

Nunca coloque `JWT_SECRET` no aplicativo mobile ou em variáveis `EXPO_PUBLIC_*`. Não há segredo padrão no repositório. A aplicação exige `JWT_SECRET` no ambiente.

## Arquivos alterados

### Produção

- `pom.xml`
- `README.md`
- `src/main/resources/application.properties`
- `src/main/resources/data.sql`
- `src/main/java/com/animequiz/api/configuration/OpenApiConfig.java`
- `src/main/java/com/animequiz/api/controller/AttemptController.java`
- `src/main/java/com/animequiz/api/controller/AuthController.java` (novo)
- `src/main/java/com/animequiz/api/controller/UserController.java`
- `src/main/java/com/animequiz/api/controller/UserProfileByUserController.java`
- `src/main/java/com/animequiz/api/dto/AttemptRequest.java`
- `src/main/java/com/animequiz/api/dto/AuthResponse.java` (novo)
- `src/main/java/com/animequiz/api/dto/AuthUserResponse.java` (novo)
- `src/main/java/com/animequiz/api/dto/LoginRequest.java` (novo)
- `src/main/java/com/animequiz/api/dto/RegisterRequest.java` (novo)
- `src/main/java/com/animequiz/api/entity/User.java`
- `src/main/java/com/animequiz/api/exception/GlobalExceptionHandler.java`
- `src/main/java/com/animequiz/api/repository/UserRepository.java`
- `src/main/java/com/animequiz/api/service/AttemptService.java`
- `src/main/java/com/animequiz/api/service/AuthService.java` (novo)
- `src/main/java/com/animequiz/api/security/AnimeQuizUserDetailsService.java` (novo)
- `src/main/java/com/animequiz/api/security/JwtService.java` (novo)
- `src/main/java/com/animequiz/api/security/SecurityConfig.java` (novo)

### Testes

- `src/test/java/com/animequiz/api/ApiContractIntegrationTest.java`
- `src/test/java/com/animequiz/api/AuthSecurityIntegrationTest.java` (novo)
- `src/test/java/com/animequiz/api/DtoContractTest.java`
- `src/test/java/com/animequiz/api/OpenApiContractTest.java`
- `src/test/java/com/animequiz/api/QuizContractIntegrationTest.java`
- `src/test/java/com/animequiz/api/UserProfileStatsIntegrationTest.java`
- `src/test/java/com/animequiz/api/service/AttemptServiceTest.java`

## Resultado da execução dos testes

**Não foi possível executar a suíte neste ambiente.** Foi tentado:

```bash
bash mvnw test -q
```

O Maven Wrapper tentou baixar Maven 3.9.16 de `repo.maven.apache.org`, mas o download falhou (`wget: Failed to fetch`). Também não há instalação global de `mvn` nem dependências Maven previamente disponíveis no cache local. O Java disponível no ambiente de trabalho é 21.0.12, não Java 17.

Portanto, nenhum teste é declarado aprovado. Os testes existentes foram adaptados para os contratos JWT e foi adicionado `AuthSecurityIntegrationTest`, cobrindo cadastro sem data, data nula, data preenchida, hash BCrypt, duplicidade, login, `/users/me`, token inválido/expirado, tentativa sem autenticação, spoof de `userId`, acesso ao próprio perfil e autorização administrativa. A execução real permanece pendente em ambiente com Java 17 e acesso ao Maven Central.

## Como executar no Windows

No PowerShell, dentro da pasta do projeto:

```powershell
java -version
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

O ambiente deve utilizar Java 17. Para testar no Swagger, abra `http://localhost:8080/swagger-ui.html`. Cadastre uma conta em `/auth/register`, copie `accessToken`, clique em **Authorize** e informe `Bearer <token>` para testar as rotas protegidas.

## Limitações deliberadas / próxima etapa

- H2 permanece em memória; os dados reiniciam com a aplicação. PostgreSQL e persistência de produção ficam para a etapa de deploy.
- Não foi configurado CORS nesta etapa. Android nativo não é bloqueado pela política CORS do navegador; Expo Web exigirá origens explícitas.
- Validação de e-mail verifica formato, não comprova posse da caixa postal. Verificação por e-mail, recuperação de senha, refresh token e rate limiting não foram adicionados.
- A integração mobile não foi iniciada. O frontend ainda precisa ajustar o cadastro/login para os contratos desta etapa e enviar o Bearer token nas chamadas privadas.
- A suíte precisa ser executada antes de considerar a etapa aprovada para integração.
