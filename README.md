# Anime Quiz API

API REST do **Anime Quiz**, desenvolvida em Java e Spring Boot para atender o aplicativo de quizzes de anime. A API gerencia contas, perfis, quizzes, questões, alternativas e pontuações; a correção das respostas e o cálculo dos pontos acontecem no servidor.

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 17 | Linguagem e versão-alvo |
| Spring Boot 3.3.2 | Aplicação REST |
| Spring Web | Controllers e endpoints HTTP |
| Spring Data JPA / Hibernate | Persistência e consultas |
| H2 | Banco de dados em memória para desenvolvimento |
| Spring Security | Autenticação e autorização |
| JWT / JOSE (HS256) | Tokens Bearer |
| BCrypt | Hash de senhas |
| Spring HATEOAS | Links de navegação HAL nos recursos compatíveis |
| Springdoc OpenAPI | Documentação Swagger UI |
| Jakarta Bean Validation | Validação de entradas |
| Maven | Build e gerenciamento de dependências |

## Funcionalidades

- Cadastro e login com e-mail e senha.
- Senhas armazenadas com BCrypt; a senha original não é devolvida pela API.
- Autenticação stateless com JWT Bearer.
- Consulta da conta autenticada.
- CRUD administrativo de usuários, perfis, quizzes, questões e alternativas.
- Catálogo de quizzes com categoria, dificuldade e quantidade de questões.
- Questões e alternativas sem exposição do gabarito.
- Correção de tentativas no servidor.
- Histórico de pontuações, ranking, XP, níveis, patentes e estatísticas de perfil.
- Listagens paginadas e respostas HATEOAS quando aplicável.
- Erros padronizados no formato `APIError`.

## Requisitos

- JDK 17.
- Acesso à internet na primeira execução para baixar o Maven e as dependências.
- Maven Wrapper incluído no projeto.

Confirme a versão do Java:

```bash
java -version
```

## Configuração

A aplicação exige uma chave de assinatura JWT fornecida pelo ambiente. Não existe chave de produção versionada no repositório.

### PowerShell (Windows)

Gere uma chave aleatória de 32 bytes para a sessão atual:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Opcionalmente, configure a validade e o emissor:

```powershell
$env:JWT_EXPIRATION_SECONDS = "3600"
$env:JWT_ISSUER = "animequiz-api"
```

### Linux / macOS

Com OpenSSL instalado:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
export JWT_EXPIRATION_SECONDS=3600
export JWT_ISSUER=animequiz-api
```

`JWT_SECRET` deve conter pelo menos 32 bytes. Em ambientes compartilhados, use um gerenciador de segredos ou as variáveis protegidas da plataforma de hospedagem. Nunca coloque esse valor no aplicativo mobile ou em variáveis `EXPO_PUBLIC_*`.

## Executar e testar

### Windows (PowerShell)

Na pasta do projeto:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw clean test
./mvnw spring-boot:run
```

A API inicia, por padrão, em `http://localhost:8080`.

## Swagger / OpenAPI

Com a aplicação em execução:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Documento OpenAPI: `http://localhost:8080/api-docs`

Para testar endpoints protegidos:

1. Execute `POST /auth/register` ou `POST /auth/login`.
2. Copie o valor de `accessToken` da resposta.
3. Clique em **Authorize** no Swagger UI.
4. Informe `Bearer <accessToken>`.
5. Execute uma operação protegida.

O esquema `bearerAuth` é aplicado às operações que exigem autenticação. O Swagger não contém tokens ou senhas válidos.

## Autenticação

### Cadastro — `POST /auth/register`

```json
{
  "username": "Usuario Exemplo",
  "email": "usuario@example.com",
  "password": "SENHA_FORTE_AQUI",
  "dateOfBirth": null
}
```

`dateOfBirth` é opcional. Pode ser omitido ou enviado como `null`. Quando informado, deve usar `yyyy-MM-dd` e representar uma data passada.

Resposta: `201 Created`, com `Location: /users/{id}` e os campos `accessToken`, `tokenType`, `expiresIn` e `user`.

### Login — `POST /auth/login`

```json
{
  "email": "usuario@example.com",
  "password": "SENHA_FORTE_AQUI"
}
```

Resposta: `200 OK`, no mesmo formato de autenticação do cadastro. Credenciais inválidas retornam `401 Unauthorized`.

### Conta autenticada — `GET /users/me`

Requer `Authorization: Bearer <accessToken>`. Retorna `id`, `username`, `email` e `dateOfBirth` da conta associada ao token.

## Recursos e endpoints

Todas as rotas de listagem aceitam `page`, `size` e `sort`, quando aplicável. O padrão do Spring Data é página `0` e tamanho `20`.

| Recurso | Endpoints principais |
|---|---|
| Autenticação | `POST /auth/register`, `POST /auth/login` |
| Conta atual | `GET /users/me` |
| Usuários | `GET /users`, `GET /users/{id}`, `GET /users/search?username=`, CRUD administrativo |
| Perfis | `GET /profiles`, `GET /profiles/{id}`, `GET /profiles/search?bio=`, CRUD administrativo |
| Perfil com estatísticas | `GET /users/{userId}/profile` |
| Quizzes | `GET /quizzes`, `GET /quizzes/{id}`, `GET /quizzes/search?title=`, `GET /quizzes/difficulty/{difficulty}`, CRUD administrativo |
| Questões | `GET /questions`, `GET /questions/{id}`, `GET /questions/by-quiz/{quizId}`, CRUD administrativo |
| Questões com alternativas | `GET /quizzes/{quizId}/questions` |
| Alternativas | `GET /options`, `GET /options/{id}`, `GET /options/by-question/{questionId}`, CRUD administrativo |
| Tentativas | `POST /quizzes/{quizId}/attempts` |
| Pontuações | `GET /scores`, `GET /scores/{id}`, `GET /scores/ranking`, `DELETE /scores/{id}` (ADMIN) |
| Status | `GET /api/hello` |

`POST /scores` e `PUT /scores/{id}` permanecem documentados como operações descontinuadas e retornam `410 Gone`. A pontuação é criada somente ao enviar uma tentativa.

## Enviar uma tentativa

`POST /quizzes/{quizId}/attempts` exige JWT. O corpo contém somente as respostas selecionadas:

```json
{
  "answers": [
    { "questionId": 1, "optionId": 1 },
    { "questionId": 3, "optionId": 5 }
  ]
}
```

O servidor identifica o usuário pelo `sub` do token. Um `userId` enviado pelo cliente não é utilizado.

A resposta `201 Created` contém o resultado agregado, incluindo `scoreId`, `totalQuestions`, `answeredQuestions`, `correctAnswers`, `pointsPerCorrectAnswer` e `points`.

### Regra de pontuação

`pontos = acertos x 10 x peso da dificuldade`

| Dificuldade | Peso |
|---|---:|
| `FACIL` | 1 |
| `MEDIO` | 2 |
| `DIFICIL` | 3 |

Questões não enviadas contam como incorretas. O servidor nunca devolve o gabarito nem informa quais questões foram respondidas corretamente.

## Perfil, XP e níveis

O endpoint `GET /users/{userId}/profile` retorna o perfil e as estatísticas do usuário autenticado. O acesso é permitido ao próprio usuário ou a uma conta `ADMIN`.

- XP considera a melhor pontuação de cada quiz distinto.
- Todas as tentativas permanecem no histórico.
- `completedQuizzes` conta quizzes distintos respondidos.
- `correctAnswers` e `totalAnswers` somam as respostas efetivamente enviadas em todas as tentativas.
- O nível é calculado pela regra `xpParaIniciar(L) = 50 * L * (L - 1)`, com limite no nível 50.
- As patentes são Iniciante, Aprendiz, Explorador, Veterano, Mestre, Grão-Mestre e Lenda.

## Acesso e permissões

| Operação | Acesso |
|---|---|
| Cadastro e login | Público |
| Leitura de quizzes, questões e alternativas | Público |
| Ranking | Público |
| Tentativas e `GET /users/me` | Usuário autenticado |
| `GET /scores` e `GET /scores/{id}` | Usuário autenticado |
| `GET /users/{userId}/profile` | Próprio usuário ou ADMIN |
| CRUD de usuários, perfis, quizzes, questões, alternativas e exclusão de scores | ADMIN |
| Swagger UI, OpenAPI e `/api/hello` | Público |

Contas criadas pelo cadastro público recebem a role `USER`. Não existe endpoint público para promover uma conta a `ADMIN`. Os usuários inseridos por `data.sql` são dados de demonstração e não possuem senha; portanto, não podem autenticar. Para testes locais, uma conta pode ser promovida por um operador com acesso direto ao H2 após o cadastro; habilite o console apenas localmente e faça login novamente para emitir um token com a role atualizada. Não exponha o console em ambientes publicados.

## Configurações

| Variável | Padrão | Finalidade |
|---|---|---|
| `JWT_SECRET` | Obrigatória | Segredo de assinatura HS256 (mínimo de 32 bytes) |
| `JWT_EXPIRATION_SECONDS` | `3600` | Validade do token em segundos |
| `JWT_ISSUER` | `animequiz-api` | Emissor esperado na validação do JWT |
| `H2_CONSOLE_ENABLED` | `false` | Habilita o console H2 apenas quando explicitamente solicitado |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |

### Console H2

O console está desabilitado por padrão. Para habilitá-lo durante desenvolvimento local, configure `H2_CONSOLE_ENABLED=true` antes de iniciar a aplicação. O console permite consultar diretamente as tabelas, inclusive a coluna que armazena o gabarito; não o habilite em ambientes publicados.

O banco H2 é mantido em memória. Os dados são recriados a cada reinicialização. A migração para um banco persistente pertence a uma etapa separada.

## Estrutura do código

```text
src/main/java/com/animequiz/api
├── configuration/  Configuração do OpenAPI
├── controller/     Endpoints REST
├── dto/             Contratos de entrada e saída
├── entity/          Entidades JPA e enums
├── exception/       Exceções e tratamento global
├── repository/      Repositórios Spring Data
├── security/        Configuração e suporte JWT
└── service/         Regras de negócio

src/main/resources
├── application.properties
└── data.sql

src/test/java
└── Testes unitários e de integração
```

## Limitações atuais

- H2 está em memória; os dados não sobrevivem a reinicializações.
- Não há migrações de banco com Flyway.
- Não há verificação de posse do e-mail, recuperação de senha, refresh token ou login social.
- Não há rate limiting, idempotência por chave, API Key ou versionamento por header.
- CORS não foi configurado nesta etapa. Aplicativos Android nativos não dependem de CORS do navegador; clientes web precisam de origens permitidas explícitas.
- A integração do aplicativo mobile e o deploy não fazem parte deste repositório/etapa.

## Histórico

Consulte [CHANGELOG.md](CHANGELOG.md) para o histórico de alterações e decisões técnicas.

## Parte 2 — recursos avançados (entrega acadêmica)

Os exemplos demonstráveis de `X-API-Version`, `X-API-Key`, `X-Idempotency-Key`, rate limiting e CORS estão descritos no arquivo [GUIA_RECURSOS_AVANCADOS.md](GUIA_RECURSOS_AVANCADOS.md).

Importe a coleção [POSTMAN_PARTE2_ANIME_QUIZ.json](POSTMAN_PARTE2_ANIME_QUIZ.json) no Postman e preencha as variáveis `baseUrl`, `token` e `apiKey` conforme o cenário.

**Compatibilidade:** o JWT e os endpoints usados pelo aplicativo mobile permanecem ativos; `X-Idempotency-Key` é opcional, não exigido pelos clientes antigos. As novas garantias de idempotência e rate limiting são adequadas a uma instância com H2 em memória e não substituem um mecanismo distribuído.
