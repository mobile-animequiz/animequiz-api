# Changelog

## Revisão de acabamento e documentação

- README atualizado para refletir os contratos atuais de cadastro, login, JWT, usuário autenticado e tentativas.
- Metadados OpenAPI revisados: removidos contato, licença e URL de produção fictícios; mantido apenas o servidor local conhecido.
- Exemplos de conflito no Swagger deixaram de usar e-mails e títulos associados aos dados de demonstração.
- Tags de perfil padronizadas como `Perfis`.
- Console H2 desabilitado por padrão; pode ser habilitado explicitamente em desenvolvimento local.
- Segredo JWT passou a ser obrigatório por variável de ambiente, sem chave padrão versionada. Os testes usam uma chave exclusiva em `src/test/resources/application.properties`.
- Removidos comentários de edição obsoletos no serviço e repositório de quizzes.
- Corrigidos erros de sintaxe preexistentes em quatro classes de teste, sem alterar os cenários ou as verificações.
- Nenhum endpoint, DTO público, contrato HTTP, regra de autorização, cálculo de pontuação, XP ou nível foi alterado.
- Arquivos locais do IntelliJ e artefatos compilados (`.idea/` e `target/`) não fazem parte do pacote preparado para o GitHub.
- Parsing XML do POM e análise sintática de 84 arquivos Java com `-source 17` concluídos sem erros. Validação Maven pendente: o Maven Wrapper não conseguiu baixar Maven 3.9.16 neste ambiente; a suíte não foi executada. Consulte `RELATORIO_REVISAO_GITHUB.md`.

## Etapa 1 (continuação): D5 implementado (perfil, XP, nível e estatísticas)

Escopo: D5 com as decisões aprovadas. Preserva D1 a D4. Sem JWT, sem cadastro com senha, sem alteração do frontend; tentativas parciais mantidas; **nenhuma dependência adicionada**; Spring Boot continua em 3.3.2.

### Novo

- **`GET /users/{userId}/profile`** (aditivo; rota usada pelo aplicativo): campos do perfil (`id`, `userId`, `username`, `bio`, `avatarUrl`) mais `level`, `rankTitle`, `xp`, `levelStartXp`, `nextLevelXp`, `completedQuizzes`, `correctAnswers` e `totalAnswers`. Somente leitura (nunca cria nem altera registros); `404` para usuário inexistente ou sem perfil. `GET /profiles/**` continua igual e sem estatísticas.
- **XP:** soma da **melhor pontuação de cada quiz distinto** (todas as tentativas permanecem no histórico; repetir um quiz não acumula XP indefinidamente).
- **Nível:** `xpParaIniciar(L) = 50 · L · (L − 1)`, máximo 50. No nível 50, `nextLevelXp = 127500` (valor de `xpParaIniciar(51)`) e o progresso deve ser limitado a 100% pelo cliente. Implementado em uma classe pura, `LevelCalculator`.
- **Patentes neutras:** Iniciante (1-4), Aprendiz (5-9), Explorador (10-14), Veterano (15-19), Mestre (20-29), Grão-Mestre (30-39) e Lenda (40-50).
- **`completedQuizzes`:** quizzes distintos já respondidos (repetir não aumenta). **`correctAnswers` e `totalAnswers`:** somados em todas as tentativas; `totalAnswers` são as respostas **efetivamente enviadas** (não o total de perguntas do quiz).
- **`Score` guarda os números de cada tentativa:** novas colunas `correct_answers`, `answered_questions` e `total_questions` (todas `NOT NULL`), com a invariante `0 ≤ acertos ≤ respondidas ≤ total` e `total ≥ 1` (validada no construtor e por Bean Validation). `AttemptService` as preenche; o comportamento das tentativas parciais **não mudou**.
- **`ScoreResponse` (aditivo):** passa a trazer `correctAnswers`, `answeredQuestions` e `totalQuestions` em `GET /scores`, `/scores/{id}` e `/scores/ranking`.
- **Todo usuário tem perfil:** `POST /users` cria um perfil vazio na mesma transação; o `data.sql` cria, com uma instrução idempotente (`INSERT ... SELECT ... WHERE NOT EXISTS`), o perfil de qualquer usuário que ainda não tenha um (usuários de exemplo e usuários antigos). A leitura do perfil nunca grava.

### Mudanças de comportamento a conhecer

- **Seeds:** os 3 scores de exemplo ganharam os novos campos (30/3/3/3, 20/2/3/3 e 40/2/3/3 para pontos/acertos/respondidas/total) e os 3 usuários de exemplo agora têm perfil. Por isso `POST /profiles` para os usuários 1, 2 e 3 passa a responder `409` (já existe perfil); use `PUT /profiles/{id}`.
- **Exclusão de usuário:** a regra existente (`409` se houver scores ou perfil) foi **mantida sem alteração**. Como agora todo usuário tem perfil, remover um usuário sem scores exige `DELETE /profiles/{id}` antes. Depois disso, `GET /users/{userId}/profile` responde `404` (não recria).
- **`Score`:** o construtor de 3 argumentos foi substituído por um de 6 (`user, quiz, points, correctAnswers, answeredQuestions, totalQuestions`).

### Documentação e testes

- README (contrato do app, regras de XP/nível/patentes, dados de exemplo, limitações), Swagger (nova rota, `404`, descrição de XP/nível/patentes e do histórico no score) e este CHANGELOG atualizados.
- Testes novos (não executados; ver abaixo): `LevelCalculatorTest`, `ScoreTest`, `UserProfileServiceTest`, `UserProfileStatsIntegrationTest`; casos novos em `AttemptServiceTest`, `DtoContractTest` e `OpenApiContractTest`. **Nenhum teste existente foi removido, alterado ou enfraquecido.**

### Status de execução

`./mvnw clean test` **não foi executado**: o ambiente de desenvolvimento desta etapa não alcança o Maven Central (`repo.maven.apache.org`) e só há JDK 21 (a execução com Java 17 será feita localmente). **Nenhum teste foi executado por Maven, portanto a etapa NÃO está aprovada.** O que foi verificado: sintaxe de todos os arquivos com `javac -source 17` sem dependências (sem erros de sintaxe nem de assinatura do projeto) e a lógica pura do `LevelCalculator`, executada de fato contra 216 verificações (fórmula, fronteiras, patentes, teto e a tabela do teste de integração), todas corretas.

## Etapa 1 (continuação): decisões D1 a D4 aplicadas

Escopo: implementar na API as decisões D1 a D4 da Etapa 1. Spring Boot continua em 3.3.2; **nenhuma dependência foi adicionada**; sem JWT/autenticação; D5 (perfil/XP/nível) e D6 (cadastro) **não** foram implementados; o aplicativo (Etapa 2) não foi tocado.

### Mudança incompatível (breaking)

| Mudança | Antes | Agora | Quem é afetado | Como migrar |
|---|---|---|---|---|
| `category` obrigatória em quizzes | `POST/PUT /quizzes` aceitavam `{ title, description, difficulty }` | Exigem também `category` (`SHONEN`, `SEINEN`, `SHOJO`, `JOSEI` ou `KODOMO`); sem ela, `400` com `A categoria é obrigatória` | Somente quem cria/edita quizzes (o aplicativo não cria) | Enviar `category` nos corpos de `POST/PUT /quizzes` |

### Novo

- **D1: enum `Category`** (`SHONEN`, `SEINEN`, `SHOJO`, `JOSEI`, `KODOMO`) em `Quiz` (coluna `category`, texto, não nula). Aparece em `QuizRequest` e `QuizResponse`. Os dois quizzes de exemplo (`data.sql`) são `SHONEN`. Valor fora do enum: `400`. Com H2 em memória não há migração; com Flyway (Fase 3) entra como migração.
- **D2: `questionCount`** em `QuizResponse` (aditivo): calculado a cada resposta, **não armazenado** na entidade, e ignorado se enviado pelo cliente. Listagens, buscas e `difficulty` usam UMA consulta agrupada por página (`group by quiz_id`), sem N+1; leitura/atualização de um quiz usa uma contagem; quiz recém-criado responde `0` sem consulta.
- **D3: `GET /quizzes/{quizId}/questions`** (paginado, HAL): cada questão vem com `options: [{ id, text }]`, em ordem crescente de `id`, **sem `isCorrect`** (a consulta de alternativas nem seleciona a coluna do gabarito). Custo constante: existência do quiz + página de questões + uma consulta de alternativas com `IN`. Quiz sem questões: `200` lista vazia; quiz inexistente: `404`. `GET /questions/by-quiz/{quizId}` e `GET /options/by-question/{questionId}` continuam funcionando, inalterados.

### Documentação

- **D4:** `userId` no corpo de `POST /quizzes/{quizId}/attempts` documentado como **provisório, somente para desenvolvimento e testes**, a ser substituído pela identificação via token na Fase 3 (README, Swagger geral, descrição do endpoint e do campo `userId`). Nenhuma autenticação foi implementada.
- Swagger: `category`, `questionCount` e a nova rota (incluindo `400`/`404`) documentados; fluxo de tentativa atualizado.
- README: funcionalidades, fluxo, referência de endpoints, contrato para o aplicativo, dados de exemplo e limitações.

### Testes adicionados (não executados; ver "Status de execução")

`QuizServiceTest` e `QuestionServiceTest` (unitários, Mockito); `QuizContractIntegrationTest` (integração, incluindo contagem de consultas sem N+1); novos casos em `DtoContractTest` e `OpenApiContractTest`. Testes existentes atualizados para o novo construtor de `Quiz` e para enviar `category` (nenhum removido ou enfraquecido; dois passaram a verificar também `category`/`questionCount`).

### Pendente de decisão (nada implementado)

- **D5:** perfil com XP, quizzes concluídos, respostas corretas, total de questões, nível e patente (proposta técnica no relatório; inclui guardar `correctAnswers` e `totalQuestions` em `Score`, que **não** foi feito).
- **D6:** cadastro: fica para a Fase 3, junto com a autenticação.

### Status de execução

`mvn clean test` e `./mvnw clean test` continuam impossíveis nesta sessão (sem acesso ao Maven Central). **Nenhum teste foi executado.** Só foi feita verificação sintática com `javac` sem dependências (sem erro de sintaxe; todos os erros restantes são de dependência ausente).

## Etapa 1: Auditoria de integração com o aplicativo móvel

Escopo: comparar a API (Fase 2 finalizada) com o aplicativo Anime Quiz e corrigir apenas o que não depende de decisão de arquitetura. Spring Boot continua em 3.3.2; nenhuma dependência foi adicionada; nenhum endpoint novo.

### Corrigido

- `GET /questions/by-quiz/{quizId}` e `GET /options/by-question/{questionId}` agora devolvem os itens em **ordem crescente de `id`** (antes a ordem dependia do banco). O aplicativo rotula as alternativas A, B, C... pela ordem recebida e mostra as questões na ordem recebida. Um `sort` informado pelo cliente continua aceito e é aplicado depois dessa ordem.
- Mensagens de validação de `POST/PUT /quizzes` e `POST/PUT /users` passaram a ser as mesmas das entidades, em português (`O título é obrigatório`, `O nome de usuário deve ter entre 3 e 50 caracteres`, `Informe um e-mail válido`...). Antes, a validação do DTO respondia com o texto padrão do Bean Validation, e o aplicativo exibe o campo `message` diretamente ao usuário. Os códigos HTTP não mudaram.
- OpenAPI: as duas consultas acima documentam a ordenação; o exemplo de `400` de quizzes usa a mensagem real.

### Testes adicionados (não executados; ver "Status de execução" abaixo)

Listagem em HAL mesmo com `Accept: application/json` (é o que o aplicativo envia), campos do card da Home em `GET /quizzes/{id}`, ordem de questões e alternativas, mensagens em português, e aceitação de `questionId`/`optionId` enviados como string numérica (o aplicativo converte IDs com `String(id)`).

### Incompatibilidades encontradas que dependem de decisão (nada foi alterado)

Ver `relatorio-etapa1-auditoria.md`: categoria do quiz, `questionCount`, rota de questões com alternativas embutidas, identificação do usuário na tentativa, perfil com estatísticas, cadastro e autenticação.

### Status de execução

`mvn clean test` e `./mvnw clean test` continuam bloqueados no ambiente de preparação (HTTP 403 em `repo.maven.apache.org`). **Nenhum teste foi executado.**

## Fase 2: Correções (finalizada)

Escopo: corrigir os problemas apontados na auditoria sem adicionar autenticação, migração de Spring Boot, Flyway ou PostgreSQL (fases seguintes). O `pom.xml` não foi alterado (Spring Boot 3.3.2).

### Mudanças incompatíveis (breaking)

| Mudança | Antes | Agora | Motivo | Como migrar o app |
|---|---|---|---|---|
| Gabarito removido das respostas de alternativas | `OptionResponse` trazia `isCorrect` em `GET /options`, `GET /options/{id}`, `GET /options/by-question/{id}`, `POST /options` e `PUT /options/{id}` | `OptionResponse` traz só `id`, `questionId` e `text` | Qualquer cliente podia ler as respostas certas e gabaritar o quiz | Parar de ler `isCorrect`; enviar as escolhas para `POST /quizzes/{quizId}/attempts` |
| `POST /scores` desativado | Aceitava `userId`, `quizId` e `points` enviados pelo cliente | Responde `410 Gone` | O cliente informava a própria pontuação (ranking forjável) | Usar `POST /quizzes/{quizId}/attempts` |
| `PUT /scores/{id}` desativado | Reescrevia `points` | Responde `410 Gone` | Mesma razão | Não há substituto: a pontuação não é editável |
| DTO `ScoreRequest` removido | Corpo de `POST/PUT /scores` | Não existe mais | Era o canal pelo qual o cliente enviava a pontuação | Nenhuma ação (interno) |

Funcionalidades removidas e justificativa: apenas as duas acima. Ambas permitiam fraude de pontuação e foram substituídas por um endpoint equivalente (a tentativa), não simplesmente apagadas; as rotas continuam existindo e respondem `410` com a mensagem de migração.

`OptionRequest.isCorrect` **continua** aceito em `POST /options` e `PUT /options/{id}` (campo apenas de entrada).

### Novo

- `POST /quizzes/{quizId}/attempts`: recebe `{ userId, answers: [{ questionId, optionId }] }`, corrige no servidor, cria o score e devolve o resultado agregado (`scoreId`, `userId`, `username`, `quizId`, `quizTitle`, `difficulty`, `totalQuestions`, `answeredQuestions`, `correctAnswers`, `pointsPerCorrectAnswer`, `points`). Regra: `pontos = acertos × 10 × peso` (`FACIL` = 1, `MEDIO` = 2, `DIFICIL` = 3). Questões não enviadas contam como erradas. A resposta não informa quais questões foram acertadas nem o gabarito.
  - `201` com `Location: /scores/{id}`.
  - `400`: questão que não pertence ao quiz, opção que não pertence à questão, opção inexistente, resposta duplicada para a mesma questão, `answers` vazio ou `userId` ausente.
  - `404`: quiz ou usuário inexistente.
  - `409`: quiz sem questões.
  - `415`: `Content-Type` diferente de JSON.
  - Transitório: `userId` vem no corpo até existir autenticação. Não foi criado nenhum endpoint de login, cadastro com senha ou JWT.

### Corrigido (antes respondia 500 ou ficava incorreto)

| Situação | Antes | Agora |
|---|---|---|
| JSON malformado ou corpo ausente | 500 | 400 |
| Valor inválido em parâmetro de rota/query (ex.: `/users/abc`, `/quizzes/difficulty/XYZ`) | 500 | 400 |
| Parâmetro de query obrigatório ausente (ex.: `GET /quizzes/search` sem `title`) | 500 | 400 |
| Ordenação por propriedade inexistente (ex.: `GET /users?sort=xyz`) | 500 | 400 |
| Rota inexistente | 500 | 404 |
| Método HTTP não suportado (ex.: `PATCH /quizzes/1`) | 500 | 405 (com `Allow`) |
| `Content-Type` não suportado | 500 | 415 |
| Texto de questão com menos de 5 ou mais de 255 caracteres | 500 | 400 (mesma validação da entidade, agora no DTO) |
| Texto de opção com mais de 150 caracteres | 500 | 400 |
| `PUT /users/{id}` com username já usado por outro usuário | 500 | 409 |
| `DELETE /users/{id}` de usuário com perfil | 500 | 409 |
| `DELETE /quizzes/{id}` de quiz com questões, scores ou favoritos | 500 | 409 |
| `DELETE /questions/{id}` de questão com opções | 500 | 409 |
| Erros inesperados | Engolidos, sem log | Registrados em log (a resposta continua 500 genérica) |

Os códigos "Antes" vêm da leitura do tratador global original, que respondia 500 para qualquer exceção sem tratamento específico.

### Dados de exemplo

- Scores de exemplo (150 e 200 pontos) eram impossíveis: o quiz tinha 1 questão. Agora o `data.sql` tem 3 questões por quiz e scores coerentes com a regra (30, 20 e 40 pontos).
- O quiz 1 (`FACIL`) tem as questões 1, 3 e 4; o quiz 2 (`MEDIO`) tem as questões 2, 5 e 6. Os IDs originais (questões 1 e 2, opções 1 a 4) foram preservados; os novos IDs (questões 3 a 6, opções 5 a 16) só acrescentam.
- Cada questão tem exatamente uma alternativa correta.

### Desempenho e configuração

- `spring.jpa.open-in-view=false`: o acesso ao banco fica restrito às camadas de serviço (todas `@Transactional`).
- N+1 eliminado nas listagens de questões, alternativas, scores e perfis (`@EntityGraph`); `UserProfile.user` passou a `LAZY`.

### Documentação

- **OpenAPI/Swagger** descreve apenas endpoints reais (inclui `GET /api/hello`, antes sem tag nem descrição) e, por operação, só os códigos que o código produz: `404` onde há busca por ID ou vínculo, `409` onde há duplicidade ou dependentes, `415` onde há corpo, `400` em todas as operações com entrada. `POST /scores` e `PUT /scores/{id}` aparecem como obsoletos e documentam só o `410`. A regra de pontuação, o fluxo de tentativas e o formato do resultado estão na descrição da API e no endpoint de tentativas.
- Textos de `@ApiResponse` que estavam desatualizados em `UserController` foram corrigidos (conflito de username, exclusão com perfil).
- `README.md` completo (requisitos, execução, variáveis de ambiente, testes, estrutura, fluxo de tentativas, exemplos e contrato para o app).

### Revisão final (sem refatorações grandes)

- Removido o construtor `Score(Integer, User)`: sem uso e criava um score sem quiz (inválido). Aliases `getIsCorrect/setIsCorrect` de `Option` já haviam sido removidos.
- `OptionRepository` ganhou `@EntityGraph` em `findAll(Pageable)` e `findByQuestionId`, no mesmo padrão dos demais repositórios.
- Verificado por busca no código: nenhuma resposta usa `getCorrect()`; o único consumidor do gabarito é `AttemptService`; não há caminho que aceite pontuação do cliente.

### Testes

Novos ou ampliados: `ScoreCalculatorTest`, `AttemptServiceTest`, `DtoContractTest`, `ApiContractIntegrationTest`, `OpenApiContractTest` (descrição de cada um no README).

> **Status de execução: NÃO executados.** `mvn clean test` e `./mvnw clean test` foram tentados no ambiente em que a fase foi preparada e falharam na resolução de dependências, antes de compilar qualquer arquivo do projeto: o acesso a `repo.maven.apache.org` foi recusado (HTTP 403 do proxy de saída; com o `mvnw`, o download do próprio Maven também falhou). Nenhum teste foi removido ou enfraquecido para contornar isso. O que foi feito no lugar: verificação **somente sintática** com `javac` (sem classpath das dependências, portanto não equivale a compilar) e revisão manual das chamadas entre classes. A compilação e a execução dos testes continuam **pendentes** e devem ser feitas localmente com `mvn clean test`.

### O que o aplicativo (frontend) precisa adaptar

1. **Alternativas:** não existe mais `isCorrect` em nenhuma resposta. Remover qualquer leitura desse campo e qualquer correção local.
2. **Pontuação:** parar de chamar `POST /scores` e `PUT /scores/{id}` (agora `410`) e de calcular pontos no cliente.
3. **Enviar respostas:** `POST /quizzes/{quizId}/attempts` com `{ "userId": <id>, "answers": [{ "questionId": <id>, "optionId": <id> }] }`. Uma resposta por questão; questões puladas podem ser omitidas.
4. **Resultado:** usar `correctAnswers`, `totalQuestions`, `answeredQuestions`, `pointsPerCorrectAnswer` e `points` da resposta `201`. Não há indicação de quais questões foram acertadas.
5. **Usuário:** o `id` devolvido por `POST /users` é o `userId` da tentativa; guardá-lo localmente. É provisório até haver autenticação.
6. **Erros:** o que antes era `500` agora é `400`/`404`/`405`/`409`/`415`; exibir `message` do corpo de erro (`status`, `error`, `message`, `timestamp`).
7. **Cabeçalhos:** enviar `Content-Type: application/json` em `POST`/`PUT`.
8. **Sem mudança:** demais endpoints, parâmetros de paginação (`page`, `size`, `sort`) e formatos de resposta continuam como antes.

### Pendências conhecidas (fases seguintes)

- Compilar e executar a suíte de testes localmente (item acima).
- Não há autenticação: qualquer cliente ainda pode criar/editar alternativas (inclusive o gabarito), remover scores, usar qualquer `userId` etc. Entra com JWT/API Keys.
- O console H2 (`/h2-console`) está habilitado e exibe o gabarito no banco. Restringir a desenvolvimento ao introduzir perfis de ambiente.
- Spring Boot 3.3.2 está fora do suporte de manutenção: migração para uma versão suportada compatível com Java 17 deve ser uma etapa separada, antes da Fase 3.
- O nome da chave de lista dentro de `_embedded` (HAL) não foi conferido com a API em execução.
