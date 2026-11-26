# AnimeQuiz API

API REST em **Java 17 e Spring Boot** para consultar quizzes de anime, responder perguntas e acompanhar a evolução do jogador. A API corrige as respostas e calcula os pontos no servidor; o aplicativo nunca recebe o gabarito.

Esta revisão organiza o projeto para a apresentação acadêmica: **V1 básica e pública** e **V2 do jogador, com JWT, limites e permissões**. O CSS e as páginas visuais das duas documentações foram preservados.

| Acesso | Endereço |
|---|---|
| API publicada | <https://animequiz-api-wst8.onrender.com> |
| API local | `http://localhost:8080` |
| Swagger V1, aparência padrão | `http://localhost:8080/swagger-ui.html` ou `/docs/v1` |
| Swagger V2, aparência original | `http://localhost:8080/docs/v2` |
| OpenAPI V1 | `http://localhost:8080/api-docs/v1` |
| OpenAPI V2 | `http://localhost:8080/api-docs/v2` |
| Verificação de disponibilidade | `GET /api/versioned/status` |

> Os endereços de produção só refletem esta revisão depois de publicar o código no servidor. Alterar o ZIP local não atualiza automaticamente o Render.

## Índice

- [O que mudou nesta revisão](#o-que-mudou-nesta-revisão)
- [Diferenças entre V1 e V2](#diferenças-entre-v1-e-v2)
- [Primeiros passos](#primeiros-passos)
- [Versionamento e cabeçalhos](#versionamento-e-cabeçalhos)
- [Autenticação e permissões](#autenticação-e-permissões)
- [Referência da V1](#referência-da-v1)
- [Referência da V2](#referência-da-v2)
- [Objetos e paginação](#objetos-e-paginação)
- [Tentativas e idempotência](#tentativas-e-idempotência)
- [Pontuação, XP e nível](#pontuação-xp-e-nível)
- [Rate limiting](#rate-limiting)
- [CORS](#cors)
- [Erros HTTP](#erros-http)
- [Banco e catálogo](#banco-e-catálogo)
- [Executar e configurar](#executar-e-configurar)
- [Arquitetura](#arquitetura)
- [Testes e demonstração](#testes-e-demonstração)
- [Compatibilidade e recursos ocultados](#compatibilidade-e-recursos-ocultados)

## O que mudou nesta revisão

- A V1 mostra somente consultas de catálogo e status. Não tem `apiKeyAuth`, `bearerAuth`, resposta 429 nem cabeçalhos de rate limit nessas consultas.
- A V2 usa **um único esquema: JWT Bearer**. Cadastro e login são públicos para permitir a obtenção do token; o catálogo V2 e as ações privadas exigem autenticação.
- CRUD administrativo, gestão de chaves, Google e recuperação de senha foram retirados da apresentação pública do Swagger. Os schemas sem uso também são removidos.
- Usuário comum não cria, altera ou exclui conteúdo do catálogo, contas de terceiros nem pontuações. Trocar o cabeçalho para V1 não libera essas operações.
- Um jogador pode consultar seu próprio resultado, mas não a pontuação privada de outra conta nem a listagem administrativa de todos os scores.
- Links HATEOAS de alteração/exclusão e de administração não são oferecidos ao jogador. Eles também não aparecem nas consultas básicas da V1.
- Nenhum CSS, vídeo de fundo, HTML visual ou script de troca da apresentação foi alterado.

## Diferenças entre V1 e V2

| Recurso | V1 básica | V2 do jogador |
|---|---|---|
| Objetivo | Demonstrar consultas simples de quizzes, perguntas e alternativas | Demonstrar o fluxo autenticado do jogador |
| Cabeçalho | `X-API-Version: 1`, ou omitir | `X-API-Version: 2` |
| Autenticação no catálogo | Nenhuma | JWT Bearer |
| Botão Authorize | Sem esquemas de segurança | Somente `bearerAuth` |
| Rate limit do catálogo | Não aplicado | Aplicado por IP |
| Perguntas de um quiz | Dados básicos e alternativas | Também `imageUrl`, `imageAltText` e `timeLimitSeconds` |
| Conta, perfil e favoritos no Swagger | Ocultos | Disponíveis ao titular do JWT |
| Enviar respostas e consultar resultado próprio | Fora da apresentação básica | Disponível ao jogador autenticado |
| CRUD de catálogo para usuário comum | Bloqueado | Bloqueado |
| Gabarito nas respostas | Não | Não |
| Detalhamento do Swagger | Resumido | Validações, exemplos, permissões e erros |

A V1 é mais simples por ter menos funcionalidades expostas, não por conter erros de propósito. Ela consulta os mesmos quizzes e usa a representação básica já consumida pelo aplicativo. A apresentação da V2 mostra a evolução do contrato e os controles de acesso.

**Importante:** autenticação, dados pessoais e comandos administrativos são operações compartilhadas e protegidas, mesmo quando um cliente antigo envia `X-API-Version: 1`. Isso preserva o mobile existente e impede que a troca de versão seja usada para escapar da segurança. Essas operações não fazem parte da V1 pública documentada.

## Primeiros passos

Os exemplos abaixo usam Bash. No PowerShell, use `curl.exe` ou a alternativa indicada na seção de execução.

### 1. Consultar a V1 sem conta

```bash
curl 'http://localhost:8080/quizzes?page=0&size=5' \
  -H 'Accept: application/json'
```

Resposta esperada: `200`, catálogo paginado, `X-API-Version: 1` e ausência de `X-RateLimit-*`. Nenhuma chave ou token é necessário.

### 2. Criar a própria conta

```bash
curl -X POST 'http://localhost:8080/auth/register' \
  -H 'X-API-Version: 2' \
  -H 'Content-Type: application/json' \
  -d '{"username":"Jogador de teste","email":"aluno@example.test","password":"SenhaLocal123!"}'
```

Use credenciais próprias para a demonstração; os valores são apenas exemplos. A API retorna `201` com `accessToken`, `tokenType`, `expiresIn` e `user`. A conta criada recebe o papel `USER`; o cadastro não permite solicitar `ADMIN`.

### 3. Entrar em uma conta existente

```bash
curl -X POST 'http://localhost:8080/auth/login' \
  -H 'X-API-Version: 2' \
  -H 'Content-Type: application/json' \
  -d '{"email":"aluno@example.test","password":"SenhaLocal123!"}'
```

Resposta esperada: `200` com um JWT próprio da API. O e-mail é comparado de forma normalizada. Senha incorreta retorna `401`.

### 4. Usar a V2

```bash
TOKEN='cole_aqui_o_accessToken_retornado'
curl 'http://localhost:8080/quizzes?page=0&size=5' \
  -H 'X-API-Version: 2' \
  -H "Authorization: Bearer $TOKEN"
```

No Swagger V2, clique em **Authorize** e cole **somente o valor de `accessToken`**. O Swagger acrescenta o prefixo `Bearer`. Não há campo `X-API-Key` nesta apresentação.

## Versionamento e cabeçalhos

A versão é selecionada por requisição, sem renomear as URLs dos recursos.

| Valor de `X-API-Version` | Resultado |
|---|---|
| Ausente | Contrato 1 |
| `1` | Contrato 1 |
| `2` | Contrato 2 |
| Outro valor, inclusive `3`, `v2` ou vazio | `400` em `APIError` |

As respostas de recursos incluem `X-API-Version` e `Vary: X-API-Version`. Documentação e preflight CORS não fazem negociação de versão.

| Cabeçalho | Quando usar |
|---|---|
| `Accept: application/json` | Consultas e comandos JSON |
| `Content-Type: application/json` | Corpos JSON de cadastro, login, preferências e tentativa |
| `Authorization: Bearer <token>` | Catálogo V2, tentativas e dados privados |
| `X-API-Version: 2` | Selecionar o contrato V2 |
| `X-Idempotency-Key` | Opcional no envio de tentativa; 8 a 128 caracteres |
| `Content-Type: multipart/form-data; boundary=...` | Upload de foto; deixe o cliente gerar o boundary |

Não envie token na URL. Não coloque `JWT_SECRET` no mobile. A API não usa cookie de sessão para autenticar essas chamadas.

## Autenticação e permissões

As senhas são armazenadas com BCrypt. O JWT usa assinatura HS256 e contém a identificação do usuário, validade e papéis. A API valida a assinatura, o emissor, a expiração e a versão de credenciais da conta.

| Ação | Visitante | Jogador `USER` | Operador `ADMIN` |
|---|---|---|---|
| Consultar catálogo V1 | Sim | Sim | Sim |
| Cadastrar a própria conta / fazer login | Sim | Sim | Sim |
| Consultar catálogo V2 | Não | Sim | Sim |
| Enviar respostas | Não | Para a própria conta | Para a própria conta |
| Alterar preferências, foto e favoritos em `/users/me` | Não | Para a própria conta | Para a própria conta |
| Consultar perfil detalhado por ID de usuário | Não | Apenas o próprio | Permitido |
| Consultar score por ID | Não | Apenas o próprio | Permitido |
| Listar todas as contas, perfis ou scores | Não | Não | Operação interna |
| Criar/alterar/excluir catálogo ou conta de terceiros | Não | Não | Operação interna |
| Usar `/api-keys/**` | Não | Não | Desabilitado nesta revisão |

Os comandos internos administrativos são mantidos no código e não aparecem no Swagger público. Não há credencial administrativa de demonstração nem endpoint de promoção de papel.

**Criar a própria conta e enviar respostas são ações normais do jogador.** Isso é diferente de criar um quiz, cadastrar perguntas ou atribuir pontos manualmente.

## Referência da V1

Todas as operações desta tabela são `GET`, públicas e sem rate limit do catálogo.

| Rota | Finalidade |
|---|---|
| `/api/hello` | Mensagem de disponibilidade |
| `/api/versioned/status` | Versão selecionada e status |
| `/quizzes` | Listar quizzes paginados |
| `/quizzes/{id}` | Consultar um quiz |
| `/quizzes/search?title=Naruto` | Pesquisar por título |
| `/quizzes/difficulty/FACIL` | Filtrar dificuldade: `FACIL`, `MEDIO` ou `DIFICIL` |
| `/quizzes/{quizId}/questions` | Perguntas do quiz com alternativas, sem gabarito |
| `/questions` | Listar perguntas |
| `/questions/{id}` | Consultar pergunta |
| `/questions/by-quiz/{quizId}` | Perguntas de um quiz |
| `/options` | Listar alternativas |
| `/options/{id}` | Consultar alternativa |
| `/options/by-question/{questionId}` | Alternativas de uma pergunta |

O Swagger V1 não exibe cadastro, login, perfil privado, envio de tentativas, gestão de chaves ou operações administrativas.

## Referência da V2

O catálogo acima também existe na V2, com JWT. `/api/hello` e `/api/versioned/status` continuam públicos como verificações de disponibilidade.

| Método e rota | Acesso / finalidade | Sucesso |
|---|---|---|
| `POST /auth/register` | Público: criar a própria conta | 201 |
| `POST /auth/login` | Público: obter o token | 200 |
| `GET /users/me` | Conta do titular do JWT | 200 |
| `GET /users/{userId}/profile` | Perfil, estatísticas e XP próprios | 200 |
| `POST /quizzes/{quizId}/attempts` | Enviar escolhas para correção | 201 ou 200 na repetição |
| `GET /scores/{id}` | Consultar o próprio resultado | 200 |
| `GET /scores/ranking` | Consultar ranking | 200 |
| `GET /users/me/settings` | Consultar preferências | 200 |
| `PATCH /users/me/settings` | Alterar campos próprios | 200 |
| `PUT /users/me/avatar` | Enviar JPEG/PNG no campo `file` | 200 |
| `GET /users/me/avatar` | Baixar foto privada | 200 |
| `DELETE /users/me/avatar` | Remover a própria foto | 200 |
| `GET /users/me/favorites` | Favoritos paginados da própria conta | 200 |
| `PUT /users/me/favorites/{quizId}` | Favoritar sem duplicar | 204 |
| `DELETE /users/me/favorites/{quizId}` | Remover favorito | 204 |

### Preferências

```json
{
  "displayName": "Isaac",
  "theme": "midnight",
  "accent": "violet",
  "locale": "pt-BR"
}
```

O PATCH preserva os campos omitidos. Nome: 2–50 caracteres, sem controles. Temas: `midnight`/`eclipse`; destaques: `violet`/`cyan`/`rose`; idiomas: `pt-BR`/`en`/`device`. O idioma da interface não traduz o catálogo de perguntas.

### Foto

JPEG/PNG de até **2 MiB**; máximo de 4.096 pixels por dimensão e 8 milhões de pixels. O servidor verifica o conteúdo real, não apenas a extensão. Arquivo inválido gera 400; excesso do multipart, 413; formato HTTP incorreto, 415.

```bash
curl -X PUT 'http://localhost:8080/users/me/avatar' \
  -H 'X-API-Version: 2' \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@avatar.jpg'
```

O download exige Bearer e retorna `ETag` e `Cache-Control: private, no-store`. A ausência de foto retorna 404.

## Objetos e paginação

Exemplo de item de quiz, sem o envelope de paginação:

```json
{
  "id": 1,
  "title": "Quiz de anime",
  "description": "Descrição do quiz",
  "difficulty": "FACIL",
  "category": "SHONEN",
  "questionCount": 15
}
```

Exemplo de item de pergunta V1:

```json
{
  "id": 1,
  "quizId": 1,
  "quizTitle": "Quiz de anime",
  "text": "Texto da pergunta",
  "options": [
    {"id": 10, "text": "Alternativa A"},
    {"id": 11, "text": "Alternativa B"}
  ]
}
```

V2 acrescenta `imageUrl`, `imageAltText` e `timeLimitSeconds`. Imagens podem ser `null`; isso não invalida uma pergunta. Nenhuma representação retorna `correct` ou `isCorrect` nas alternativas.

Listagens do catálogo usam HAL: recursos em `_embedded`, metadados em `page` e navegação em `_links`. A chave da coleção em `_embedded` depende do tipo de recurso. Favoritos usam `content`, `totalElements`, `totalPages`, `number` e `size`.

| Parâmetro | Significado |
|---|---|
| `page=0` | Primeira página |
| `size=20` | Quantidade de itens, conforme o endpoint |
| `sort=title,asc` | Ordenação de listagens que usam Pageable |

Favoritos aceitam `page >= 0`, `size` entre 1 e 50 e ordenação estável por ID. Links HAL de alteração/exclusão não são oferecidos a jogadores; a ausência de um link não substitui a autorização no servidor.

## Tentativas e idempotência

Primeiro consulte as perguntas e use IDs realmente devolvidos por sua base. O cliente envia somente as escolhas:

```json
{
  "answers": [
    {"questionId": 1, "optionId": 10},
    {"questionId": 2, "optionId": 15}
  ]
}
```

O servidor rejeita pergunta fora do quiz, alternativa de outra pergunta e pergunta duplicada. Perguntas não enviadas não geram acerto. O usuário é identificado pelo JWT.

| Envio | Resultado |
|---|---|
| Primeira tentativa válida | 201 e um score novo |
| Mesma chave e mesmo corpo | 200, mesmo score e `Idempotency-Replayed: true` |
| Mesma chave com corpo diferente | 409 |
| Sem chave | Correção normal; repetir a chamada pode registrar outra tentativa |

`X-Idempotency-Key` é opcional e aceita 8–128 caracteres. Não é uma credencial. O controle de repetição é persistido no banco; não se deve confundir idempotência com impedir que o jogador jogue novamente usando uma chave nova.

## Pontuação, XP e nível

- Pontos da tentativa: **acertos × 10 × peso**.
- Pesos: FACIL = 1, MEDIO = 2, DIFICIL = 3.
- XP do perfil: soma da **melhor pontuação de cada quiz distinto**.
- Todas as tentativas continuam no histórico. Uma tentativa pior não aumenta o XP.
- `completedQuizzes` conta quizzes distintos; a taxa de acertos considera as respostas enviadas em todas as tentativas.
- XP inicial do nível L: `50 × L × (L − 1)`. Níveis 1, 2 e 3 começam em 0, 100 e 300 XP.
- Nível máximo: 50. Não há bônus de pontuação por velocidade.

Exemplo: melhores resultados de 70 e 30 em dois quizzes geram 100 XP. Melhorar o primeiro para 90 eleva o XP para 120, e não para 190.

## Rate limiting

**As consultas públicas documentadas na V1 não consomem o limite.** Na V2, o limite global usa IP remoto e janela fixa, por instância. Operações de conta e comandos protegidos mantêm seus controles também para clientes antigos que enviam V1.

| Escopo mostrado na V2 | Padrão |
|---|---|
| Global | 120 requisições / 60 segundos |
| `POST /auth/login` | 20 / 60 segundos |
| `POST /auth/register` | 10 / 900 segundos |

Excesso retorna HTTP 429 com `Retry-After` em segundos. Cabeçalhos `X-RateLimit-Limit`, `Remaining` e `Reset` descrevem a janela global; `X-AuthRateLimit-*` descrevem o fluxo de autenticação. Reset é Unix em segundos.

Documentação e OPTIONS são isentos. A segurança roda antes dos contadores: chamadas barradas por ela podem não consumir o limite. Os mapas são locais ao processo; reinícios zeram os contadores e réplicas não compartilham o orçamento. Atrás de proxy, vários usuários podem compartilhar o IP observado. Não se confia em X-Forwarded-For enviado livremente pelo cliente.

## CORS

Origens web são definidas por `CORS_ALLOWED_ORIGINS`, separadas por vírgulas. Os padrões locais são `http://localhost:19006` e `http://localhost:8081`. Android nativo não depende de CORS como um navegador.

- Métodos permitidos: GET, POST, PUT, PATCH, DELETE e OPTIONS.
- Cabeçalhos: Authorization, Content-Type, X-API-Version e X-Idempotency-Key.
- Respostas expõem Location, versão, limites, Retry-After e Idempotency-Replayed.
- Cookies de autenticação não são habilitados.
- Permitir uma origem não concede papel ADMIN nem acesso a dados de outras contas.

## Erros HTTP

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Você só pode consultar a própria pontuação",
  "timestamp": "2026-11-25T12:00:00"
}
```

| Status | Quando ocorre |
|---|---|
| 400 | JSON, versão, parâmetros ou vínculo pergunta/alternativa inválidos |
| 401 | Token ausente, inválido, expirado ou credencial incorreta |
| 403 | Usuário comum tentando administrar ou ler recurso privado de outro usuário |
| 404 | Recurso consultado não existe |
| 409 | E-mail/título duplicado, chave de idempotência reutilizada com outro corpo ou estado incompatível |
| 413 | Foto maior que o limite multipart |
| 415 | Formato de corpo HTTP incompatível |
| 429 | Limite de requisições excedido nas operações limitadas |
| 500 | Falha interna inesperada, registrada no servidor |

Respostas 401 usam `WWW-Authenticate: Bearer`. Nem todo endpoint devolve todos esses erros; o Swagger V2 documenta os aplicáveis. Na V1, não são anunciados 401/403/429 para suas consultas públicas.

## Banco e catálogo

O H2 padrão grava em `./data/animequizdb`. O catálogo inicial completa **8 quizzes com 15 perguntas cada**, sem duplicar os registros já existentes. O seed preserva IDs e o histórico de scores; não cria uma conta ADMIN de demonstração.

Para persistir entre deploys, configure `DB_URL` em um volume durável. H2 em arquivo é destinado a uma instância, não a várias réplicas compartilhando o arquivo. O schema usa Hibernate update, sem migrações Flyway. Faça backup antes de trocar o datasource. Uma base antiga em memória não é automaticamente transferida para arquivo.

## Executar e configurar

Requisitos: **JDK 17 completo**, acesso às dependências na primeira execução e Maven Wrapper incluído. JDKs de IDE podem servir para desenvolvimento, mas o alvo e as imagens Docker continuam Java 17.

### Windows

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .\Run-Api.ps1
```

O launcher seleciona um JDK completo e, se não houver JWT_SECRET no processo, gera um segredo efêmero para a sessão. Para manter sessões válidas após reiniciar o serviço, configure um segredo estável no ambiente.

```powershell
# Testar a API pelo launcher:
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .\Run-Api.ps1 -Test
```

### Linux / macOS

```bash
chmod +x mvnw
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run
```

### Variáveis principais

| Variável | Finalidade / padrão |
|---|---|
| `JWT_SECRET` | Segredo privado de assinatura, pelo menos 32 bytes; obrigatório |
| `JWT_EXPIRATION_SECONDS` | Duração do token; padrão 3600 |
| `JWT_ISSUER` | Emissor; padrão animequiz-api |
| `PORT` | Porta; padrão 8080 |
| `DB_URL` | JDBC; padrão local em arquivo, obrigatório no perfil prod |
| `SPRING_PROFILES_ACTIVE` | Use prod no servidor com banco configurado |
| `CATALOG_SEED_ENABLED` | Completar catálogo inicial; padrão true |
| `CORS_ALLOWED_ORIGINS` | Origens exatas da versão web |
| `RATE_LIMIT_REQUESTS` / `RATE_LIMIT_WINDOW_SECONDS` | Janela global; padrões 120 / 60 |
| `AUTH_RATE_LIMIT_ENABLED` | Proteção dos fluxos de conta; padrão true |

Spring Boot não lê `.env` automaticamente. Use variáveis do processo, configuração de execução no IDE ou o painel da hospedagem. `.env.example` serve como referência e não contém segredo real.

O Dockerfile usa uma etapa Maven/Java 17 para compilar e outra Java 17 para executar. Publicar esta revisão não cria automaticamente volume, variáveis, conta de hospedagem ou configuração do mobile.

## Arquitetura

| Camada | Responsabilidade |
|---|---|
| `controller` | Contrato HTTP, DTOs e encaminhamento para Services |
| `service` | Regras de negócio, pontuação, idempotência e propriedade da conta |
| `repository` | Consultas JPA e agregações |
| `entity` | User, Quiz, Question, Option, Score e configurações persistentes |
| `dto` | Entradas e saídas sem expor o gabarito ou hash de senha |
| `security` | JWT, papéis, versão de sessão e política pública da V1 |
| `configuration` | OpenAPI, CORS, limites, catálogo e apresentação HTTP |
| `exception` | Conversão de falhas para APIError |

Arquivos centrais desta revisão:

- `AcademicVersionPolicy.java`: identifica as consultas públicas da V1.
- `SecurityConfig.java`: aplica autenticação e papéis; bloquear não depende do Swagger.
- `AcademicOpenApiViews.java`: escolhe operações, esquemas e detalhes de cada documentação.
- `VersionDocumentationConfig.java`: gera os grupos V1/V2.
- `HalResponseAdvice.java`: preserva HAL sem oferecer controles administrativos ao jogador.
- `ScoreController.java`: valida a propriedade do resultado consultado.

## Testes e demonstração

```bash
./mvnw clean test
node --test src/test/js/docs-navigation.test.cjs
```

No Windows, substitua `./mvnw` por `.\mvnw.cmd`. A coleção [AnimeQuiz Acadêmico](postman/AnimeQuiz_Academico.postman_collection.json) reúne as consultas V1, o fluxo V2 e os testes de bloqueio. Importe-a no Postman e defina `baseUrl`.

Roteiro sugerido:

1. Abrir V1 e consultar quizzes sem Authorize; mostrar a documentação curta.
2. Trocar para V2 mantendo a identidade visual original; mostrar somente Bearer no Authorize.
3. Consultar V2 sem token para obter 401.
4. Cadastrar/entrar, consultar catálogo e enviar uma tentativa real.
5. Mostrar pontos, XP e resultado da própria conta.
6. Tentar criar um quiz com o token USER para obter 403.
7. Repetir o bloqueio com `X-API-Version: 1`, comprovando que a segurança não foi contornada.

Testes verificam separação dos documentos, ausência de chave de API, rate limit apenas nas superfícies corretas, bloqueio de escrita administrativa e isolamento de resultados. Validações HTTP e de regras já existentes foram mantidas e ajustadas ao novo contrato público.

## Compatibilidade e recursos ocultados

Esta entrega altera a **API fornecida no ZIP**. Não contém uma nova versão do mobile ou APK. O catálogo V1 mantém o formato usado pelo app e os endpoints de conta continuam protegidos para compatibilidade com clientes que ainda enviam cabeçalho 1.

Google, recuperação de senha e integrações de e-mail continuam no código para evolução futura, mas não aparecem nos documentos públicos desta apresentação. `/api-keys/**` fica bloqueado e não é um segundo mecanismo de login. O CRUD administrativo permanece reservado ao operador ADMIN e oculto do Swagger do jogador.

Os arquivos de relatórios anteriores são históricos. Para as permissões e os exemplos atuais, use este README e os documentos `/api-docs/v1` e `/api-docs/v2` gerados pela aplicação em execução.
