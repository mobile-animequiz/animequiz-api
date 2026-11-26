> **Documento histórico.** Este arquivo registra uma etapa anterior. Para a apresentação acadêmica atual (25/11/2026), consulte o [README](README.md). As consultas públicas V1 não usam autenticação/limites; a V2 usa somente JWT Bearer. CRUD administrativo e integrações adiadas ficam fora do Swagger público. Orientações antigas sobre chaves de API, permissões e estado de testes não representam esta revisão.

# Contrato funcional e versões da API

> Revisão de 19/11/2026: a seleção V1/V2 pertence à documentação da API. O APK usa V1 fixa. Acesse V1 padrão em /swagger-ui.html e V2 com o visual original em /docs/v2. Consulte docs/CONFIGURACAO_ENTREGA.md para os testes e a configuração atuais; exemplos antigos de seleção no mobile foram substituídos.

## Selecionar e demonstrar V1/V2

Ambas as versões funcionam na mesma aplicação Java e no mesmo banco. Selecione pelo cabeçalho HTTP `X-API-Version: 1` ou `X-API-Version: 2`. Sem cabeçalho, a API usa V1. Um valor diferente de 1 e 2 recebe `400` no formato `APIError`.

O servidor responde com `X-API-Version` e `Vary: X-API-Version`. O cabeçalho é usado também pelo mobile e fica exposto para clientes web através de CORS.

| Item | V1 | V2 |
|---|---|---|
| Cadastro, JWT, catálogo, perfil e ranking | Compartilhados | Compartilhados |
| `GET /quizzes/{quizId}/questions` | `id`, `quizId`, `quizTitle`, `text`, `options` | Mesmos campos + `imageUrl`, `imageAltText`, `timeLimitSeconds` |
| Imagem ausente | Campo não existe | Valor `null`; pergunta continua válida |
| Pontuação | Calculada no servidor | Mesma regra da V1 |
| Gabarito | Não é devolvido | Não é devolvido |
| Documento OpenAPI | `/api-docs/v1` | `/api-docs/v2` |

O versionamento anterior trocava apenas o exemplo de status. Agora o cabeçalho seleciona o contrato de uma operação real do aplicativo, sem renomear as rotas existentes.

Abra `/swagger-ui.html` para selecionar o documento, ou `/docs.html` para selecionar a versão e aplicá-la às chamadas de teste. O primeiro servidor do Swagger é a origem atual, para que uma apresentação local use a API local.

## Funcionalidades

| Funcionalidade | Entrada e acesso | Saída e regras |
|---|---|---|
| Criar conta | `POST /auth/register`, público; username, email e senha válidos | `201`, JWT e usuário; duplicidade retorna `409`. |
| Entrar | `POST /auth/login`, público; email e senha | `200`, JWT e usuário; credenciais inválidas retornam `401`. |
| Consultar sessão | `GET /users/me`, JWT | Dados da conta identificada pelo token. |
| Listar quizzes | `GET /quizzes`, público; page, size, sort | Coleção HAL paginada; categoria, dificuldade e quantidade real de perguntas. |
| Jogar quiz | `GET /quizzes/{id}/questions`, público | Perguntas com alternativas, paginadas, sem indicar a alternativa correta. |
| Enviar partida | `POST /quizzes/{id}/attempts`, JWT; answers com questionId e optionId | `201`, score calculado no servidor. Questões omitidas contam como incorretas. Exige pelo menos uma resposta. |
| Repetir envio com segurança | Mesmo corpo e `X-Idempotency-Key`, por usuário | `200`, mesmo score e `Idempotency-Replayed: true`; corpo diferente com mesma chave retorna `409`. |
| Consultar perfil | `GET /users/{id}/profile`, próprio usuário ou ADMIN | XP, nível, patente e estatísticas. O XP usa a melhor pontuação de cada quiz. |
| Consultar ranking | `GET /scores/ranking`, público | Pontuações em ordem decrescente. |
| Gerenciar conteúdo | CRUD `/quizzes`, `/questions`, `/options`, ADMIN | Valida vínculos, campos obrigatórios e conflitos antes de salvar. |

Pontos = acertos × 10 × peso da dificuldade (`FACIL=1`, `MEDIO=2`, `DIFICIL=3`). O aplicativo não envia sua própria pontuação nem decide a correção. A confirmação por pergunta na interface apenas registra uma escolha local; o resultado oficial é obtido ao enviar a tentativa completa ou parcial.

## Imagens e tempo

Administradores podem enviar `imageUrl` (HTTPS, até 1000 caracteres), `imageAltText` (até 255) e `timeLimitSeconds` (5 a 300, padrão 30) em POST/PUT `/questions`. Enviar `null` remove a imagem. Os campos de mídia são retornados na consulta V2 de perguntas com alternativas; o DTO de leitura V1 permanece igual.

As perguntas de demonstração de Naruto e One Piece recebem pôsteres ilustrativos relacionados ao anime, sem identificar a resposta. A imagem não é necessária para entender o enunciado. A disponibilidade dessas imagens externas depende do provedor; o app possui tratamento de falha. O texto acessível deve descrever a imagem sem revelar o gabarito.

Fontes das imagens de exemplo: [Naruto Shippuden](https://www.posters.de/products/poster-naruto-shippuden-characters-61x91-5cm-gpe6022) e [One Piece](https://98types.co.uk/products/one-piece-anime-poster). Substitua as URLs por arquivos autorizados e hospedados sob seu controle antes de uma distribuição pública.

`timeLimitSeconds` é uma sugestão para o app, que oferece modo com ou sem tempo. O servidor não verifica velocidade e não oferece pontos por rapidez. Idempotência e rate limit continuam limitados à instância atual; o banco H2 em memória perde os dados ao reiniciar a API.

## Executar

Com JDK 17 ou superior completo em `JAVA_HOME`, use `./Run-Api.ps1` na pasta `api`. O script gera uma chave JWT temporária caso não exista no ambiente e inicia o Maven Wrapper. Use `./Run-Api.ps1 -Test` para a suíte Maven. A primeira execução precisa baixar as dependências.

Não coloque JWT_SECRET no mobile. Para hospedar novamente, use uma chave persistente do ambiente de produção e publique os arquivos desta API no seu serviço Java existente. Sites não foi usado para hospedar Java ou gerar o APK.
