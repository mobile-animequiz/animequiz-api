# Revisão acadêmica — 25/11/2026

## Escopo entregue

| Solicitação | Resultado |
|---|---|
| V1 mais simples | Apenas GETs de catálogo/status na documentação pública; descrição curta e schemas necessários |
| V1 sem API key/Bearer/rate limit | Nenhum esquema de segurança no OpenAPI V1; consultas públicas não passam pelo contador nem avaliam Bearer |
| V2 com um único método de autenticação | JWT Bearer; cadastro e login públicos, catálogo V2 e dados privados autenticados |
| Usuário comum sem criação/gestão de conteúdo | POST/PUT/DELETE administrativos negados para USER na API, inclusive com cabeçalho V1 |
| Apenas ações normais no Swagger V2 | Catálogo, conta própria, respostas, resultado próprio, perfil, foto, favoritos e ranking |
| Ocultar funcionalidades adiadas | Google, recuperação de senha, chaves de API e CRUD administrativo fora dos documentos públicos |
| V2 mais detalhada | Permissões, validações, exemplos, erros, paginação, limites e idempotência documentados |
| README detalhado | Execução, configuração, contratos, arquitetura, pontuação, permissões e roteiro de apresentação |
| Não alterar CSS | Os seis arquivos estáticos recebidos permanecem idênticos, verificados por SHA-256 |

Criar a própria conta, enviar respostas e alterar os próprios favoritos/foto são ações do jogador. Não concedem poder de criar quizzes, perguntas ou alternativas. O CRUD interno permanece reservado a ADMIN e não aparece no Swagger público. Nenhuma conta administrativa de demonstração foi adicionada.

## Compatibilidade

O mobile existente pode continuar enviando `X-API-Version: 1` para operações compartilhadas de conta e tentativas. Elas continuam exigindo JWT e aplicando os controles de segurança, embora não façam parte da apresentação pública básica V1. Trocar o cabeçalho não libera dados privados nem comandos administrativos.

As rotas `/api-keys/**` estão bloqueadas. As implementações de Google e recuperação de senha foram preservadas para compatibilidade futura, mas ocultadas desta demonstração. Nenhuma mudança foi feita nos arquivos do mobile, pois esta revisão usa o ZIP da API fornecido.

## Validação executada

- JDK 17 completo, Maven Wrapper e H2 de teste.
- **298 testes Java em 23 suítes: zero falhas, zero erros, zero ignorados.**
- **4 testes Node de navegação da documentação: todos passaram.**
- API iniciada com servidor HTTP real, segredo efêmero e banco H2 em memória isolado.
- **23 requisições da coleção exercitadas por um runner HTTP local**: V1, cadastro, login com JWT real, conta, catálogo V2, tentativa, repetição idempotente, score próprio, perfil, nome, favoritos, ranking e bloqueios 401/403.
- Documentos HTTP `/api-docs/v1` e `/api-docs/v2` conferidos: V1 sem esquemas; V2 somente `bearerAuth`; nenhum POST de criação de quiz exposto.
- **125 consultas V1 consecutivas** com Bearer inválido: todas 200, sem cabeçalhos de rate limit.
- Limite V2 e isolamento de dados também exercitados nos testes de integração, com janela/limite reduzidos exclusivamente no contexto de teste.
- Todos os `$ref` publicados nos dois OpenAPI resolvem para componentes presentes.
- Comparação SHA-256 confirmou igualdade dos seis arquivos originais em `src/main/resources/static`.
- `git diff --check` sem erros.

A coleção contém scripts para uso no Postman. A validação HTTP citada acima usou seus mesmos requests em um runner local; o aplicativo Postman não foi operado nesta sessão.

## Como revisar

1. Executar a API conforme o README e aguardar a carga inicial do catálogo.
2. Abrir `/swagger-ui.html`: a V1 deve oferecer somente consultas públicas e não exibir esquemas em Authorize.
3. Abrir `/docs/v2`: a apresentação original deve continuar, e Authorize deve oferecer somente Bearer.
4. Cadastrar uma conta de teste, copiar o `accessToken` e executar o fluxo do jogador.
5. Confirmar que um POST direto em `/quizzes` com esse token resulta em 403, tanto com cabeçalho 1 como 2.
6. Importar `postman/AnimeQuiz_Academico.postman_collection.json` e executar as pastas em ordem.

## Limites da entrega

Esta validação foi local. Não publicou o código no GitHub/Render, não migrou dados do servidor e não gerou um APK novo. O pacote contém código e testes; os endereços de produção só mudam depois do deploy. Credenciais Google/e-mail continuam sendo configurações externas das integrações ocultadas.
