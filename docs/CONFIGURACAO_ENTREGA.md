> **Documento histórico.** Este arquivo registra uma etapa anterior. Para a apresentação acadêmica atual (25/11/2026), consulte o [README](../README.md). As consultas públicas V1 não usam autenticação/limites; a V2 usa somente JWT Bearer. CRUD administrativo e integrações adiadas ficam fora do Swagger público. Orientações antigas sobre chaves de API, permissões e estado de testes não representam esta revisão.

# AnimeQuiz — configuração da API
Atualizado em 19/11/2026. Código implementado e validado localmente; esta revisão não foi publicada no Render.

## Contratos novos

Todos preservam V1/V2 e a pontuação existente. Rotas /users/me usam o titular do JWT, sem receber userId.

| Método e rota | Corpo / resposta | Sucesso |
|---|---|---|
| GET /users/me/settings | displayName, theme, accent, locale, hasAvatar, avatarVersion, googleLinked | 200 |
| PATCH /users/me/settings | JSON parcial: {"displayName":"Isaac","theme":"eclipse","accent":"cyan","locale":"pt-BR"} | 200 |
| PUT /users/me/avatar | multipart/form-data, campo file; JPEG/PNG até 2 MiB | 200 |
| GET /users/me/avatar | Bytes privados, Bearer obrigatório; ETag e Cache-Control private/no-store | 200 ou 404 sem foto |
| DELETE /users/me/avatar | Preferências atualizadas com hasAvatar=false | 200 |
| GET /users/me/favorites?page=0&size=20 | content, totalElements, totalPages, number, size | 200 |
| PUT /users/me/favorites/{quizId} | Sem corpo; repetição não duplica | 204 |
| DELETE /users/me/favorites/{quizId} | Sem corpo; repetição é segura para quiz existente | 204 |
| POST /auth/google | {"idToken":"ID_TOKEN_GOOGLE"} | 200, JWT próprio |
| POST /auth/google/link | Mesmo corpo + JWT da conta que será vinculada | 204 |
| POST /auth/forgot-password | {"email":"jogador@example.com"} | 202 genérico |
| POST /auth/reset-password | {"token":"TOKEN_DO_LINK","password":"NOVA_SENHA"} | 204 |

Favoritos: page >= 0, size 1–50, ordem crescente de ID. Nome: 2–50 caracteres sem controles. Temas midnight/eclipse; destaques violet/cyan/rose; idiomas pt-BR/en/device. Campos omitidos em PATCH são preservados. Avatar: conteúdo real validado, até 4096 pixels por dimensão e 8 milhões de pixels. Arquivos inválidos retornam 400; excesso do multipart, 413; tipo incorreto, 415. Nunca inclua JWT na URL da foto.

Erros seguem APIError. Rotas privadas retornam 401 para sessão inválida/expirada/revogada. A conta autenticada não recebe autorização administrativa para usar essas rotas. Quiz inexistente retorna 404. OpenAPI em /api-docs/v1 e /api-docs/v2 contém os schemas, validações e respostas.

## Banco e publicação

1. Faça backup/exportação da base existente antes de trocar datasource ou republicar. Uma base H2 antiga em memória não é automaticamente transferida para arquivo.
2. Monte armazenamento persistente no servidor. Exemplo: /var/data. Diretório efêmero de hospedagem não atende à persistência entre reinícios/deploys.
3. Configure as variáveis privadas abaixo no ambiente do processo Java. Spring Boot não carrega um arquivo .env automaticamente; .env.example é uma referência para o painel da hospedagem, variáveis do sistema ou seu gerenciador de execução.
4. Execute os testes, publique a branch revisada e valide login, catálogo, perfil e persistência após reiniciar o serviço.
5. Somente depois gere o APK final.

| Variável | Valor necessário |
|---|---|
| JWT_SECRET | Segredo aleatório de pelo menos 32 bytes, estável entre reinícios |
| SPRING_PROFILES_ACTIVE | prod |
| DB_URL | jdbc:h2:file:/var/data/animequizdb;DB_CLOSE_ON_EXIT=FALSE (ajuste ao volume real) |
| PUBLIC_APP_ORIGIN | Origem HTTPS pública desta API, sem caminho/query/fragmento |
| GOOGLE_CLIENT_IDS | IDs de cliente WEB OAuth aceitos, separados por vírgula |
| MAIL_API_KEY | Chave privada de API do Resend |
| MAIL_FROM | Remetente autorizado no domínio verificado do Resend |
| CORS_ALLOWED_ORIGINS | Origens web exatas, separadas por vírgula; Android nativo não depende de CORS |
| RATE_LIMIT_REQUESTS / RATE_LIMIT_WINDOW_SECONDS | Padrões 120 / 60 |
| AUTH_RATE_LIMIT_ENABLED | true |
| CATALOG_SEED_ENABLED | true para carregar/completar o catálogo inicial |

O padrão local grava ./data/animequizdb. O perfil prod exige DB_URL. H2 em arquivo é destinado a uma instância do serviço; não compartilhe o mesmo arquivo entre réplicas. A atualização de schema usa Hibernate update, sem migrações Flyway. Planeje backups e migrações antes de evoluções destrutivas. Os testes usam bases isoladas em memória e um arquivo temporário para comprovar durabilidade.

data.sql fica como fixture de testes e não é mais executado na produção. O importador inicializa/completa 8 quizzes × 15 perguntas; encontra títulos existentes, preserva os IDs das perguntas já cadastradas e as dificuldades existentes. Scores mantêm seus snapshots históricos. Não há criação de usuários de demonstração no startup normal. O catálogo textual e suas fontes estão em src/main/resources/catalog/pt-BR.json. Hotlinks antigos sem licença registrada são retirados das perguntas de demonstração.

## Google

No Google Cloud Console:
1. Configure o projeto OAuth e a tela de consentimento; inclua os testadores enquanto estiver em modo de teste.
2. Crie um cliente Android com pacote com.sn6team.animequiz e SHA-1 do certificado que assina o APK EAS. Cadastre certificados diferentes quando usar debug, outro keystore ou assinatura da Play Store.
3. Crie um cliente OAuth do tipo WEB. Seu ID vai em GOOGLE_CLIENT_IDS na API e EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID no ambiente preview do EAS.
4. Se usar a versão web, autorize sua origem HTTPS no cliente WEB. O adapter web usa Google Identity Services. iOS requer configuração adicional e seu próprio URL scheme.
5. Não é necessário colocar client secret no APK. Não envie apenas e-mail/nome como prova de identidade.

A API valida assinatura RS256, issuer, audience, expiração e e-mail verificado. Emite seu próprio JWT. Conta com o mesmo e-mail NÃO é vinculada automaticamente: login Google retorna 409; entre pela senha e use “Vincular minha conta Google” no perfil. A vinculação exige sessão própria, token Google válido e e-mail correspondente.

Sem GOOGLE_CLIENT_IDS, a rota retorna 503. Nenhum ID de cliente foi inventado nem uma conta Google configurada nesta entrega.

## E-mail e redefinição

Use Resend com domínio/remetente verificados e MAIL_API_KEY e MAIL_FROM configurados. A API usa POST https://api.resend.com/emails e não precisa de SMTP nem segredo no mobile.

PUBLIC_APP_ORIGIN deve servir /reset-password.html, /reset-password.js e /reset-password.css por HTTPS. A página já está no código da API; o link fica no formato https://SEU_HOST/reset-password.html#token=... . O fragmento é removido do endereço após leitura, sem enviá-lo ao servidor como parte da URL. A página não carrega bibliotecas externas, usa CSP e referrer-policy restritos e permite definir a nova senha em PT/EN.

Token aleatório de 256 bits, somente hash SHA-256 no banco, validade 15 minutos, uso único e substituição pelo pedido mais recente. O envio acontece após commit. Senha: mínimo 8 caracteres, máximo 72 bytes UTF-8, BCrypt. Após alteração, sessões anteriores são revogadas por versão de credenciais. Não faz login automático.

A resposta 202 é idêntica para conta existente/desconhecida e para o limite por e-mail. Sem origem/provedor configurados, 503 é idêntico para todas as contas. Falhas de entrega assíncrona geram log operacional genérico, sem endereço, token ou chave. A API não confirma entrega no 202; valide um envio real, caixa de spam e a configuração do domínio antes de liberar.

## Rate limiting efetivamente implementado

Janela fixa por IP remoto da conexão e por processo. Não confia em X-Forwarded-For arbitrário.

| Escopo | Limite padrão |
|---|---|
| Global | 120 / 60 segundos |
| POST /auth/login | 20 / 60 segundos |
| POST /auth/register | 10 / 900 segundos |
| POST /auth/google | 20 / 60 segundos |
| POST /auth/google/link | 10 / 600 segundos |
| POST /auth/forgot-password | 5 / 900 segundos |
| POST /auth/reset-password | 10 / 900 segundos |
| Recuperação por e-mail normalizado | 3 / hora, mantém 202 genérico |

O global exclui OPTIONS e documentação. Os filtros executam após a camada de segurança; requisições barradas antes dela não consomem esses contadores. Os logins e a recuperação são públicos e passam pelo filtro específico antes do controller.

Limites por IP retornam HTTP 429, APIError e Retry-After em segundos. X-RateLimit-Limit/Remaining/Reset descrevem o global; X-AuthRateLimit-Limit/Remaining/Reset descrevem o fluxo de autenticação. Reset é Unix em segundos. Cabeçalhos expostos pelo CORS. O limite por e-mail não publica cabeçalhos nem revela cadastro. Os mapas têm limpeza/capacidade defensiva; reiniciar o processo reinicia os contadores.

Atrás de proxy, vários usuários podem compartilhar o IP remoto observado. Configure o proxy confiável no servidor antes de interpretar cabeçalhos encaminhados. Várias réplicas precisam de Redis/gateway para um limite agregado; isso não foi implementado.

## Swagger e validação

Tokens JWT inválidos/expirados e sessão ausente retornam APIError em JSON, HTTP 401 e WWW-Authenticate: Bearer, incluindo o filtro OAuth2. O schema de erro não herda o payload de sucesso.

A apresentação customizada introduzida em e5805bc e preservada em 6f2d815 foi restaurada exclusivamente para V2, com o mesmo CSS, paleta e fundo. V1 continua usando os recursos padrão do Swagger, sem injetar o CSS ou o vídeo da V2.

| Acesso | Apresentação |
|---|---|
| /, /docs, /docs.html e /docs/v1 | Redirecionamento 302/no-store para o Swagger padrão (V1 inicial) |
| /swagger-ui.html | V1 padrão; selecionar V2 abre sua página personalizada |
| /docs/v2 | V2 com CSS/fundo históricos e requisições X-API-Version: 2 |
| /docs.html?version=2 | Redirecionamento compatível para /docs/v2 |

Na V2, selecionar V1 retorna ao Swagger padrão. Os endpoints e os dois documentos OpenAPI foram preservados. O APK usa contrato 1 fixo e não tem seletor de versão.

302 testes Java passaram com Maven Wrapper, compilação --release 17 e JDK 25.0.2. A execução usou uma cópia isolada das mesmas fontes para preservar os arquivos de build existentes. Os quatro testes de apresentação cobrem o Swagger padrão da V1, a restauração da V2, os links antigos e o acesso público sem consumo do rate limit. Também passaram 4 testes Node de navegação, seleção e cabeçalhos (node --test src/test/js/docs-navigation.test.cjs). Google e Resend continuam substituídos por mocks nos testes de integração; não representam validação das credenciais reais.

O erro anterior de ApplicationContext tinha como causa Mockito/Byte Buddy 1.14.18 executando em Java 25, acima da versão suportada por essa dependência. O POM agora fixa Byte Buddy/agent 1.18.11 de forma consistente e o Surefire carrega o agente ao iniciar a JVM, sem depender de autoanexação dinâmica ou da opção experimental. O alvo Java 17 e as imagens Java 17 do Docker foram preservados.

No PowerShell, a partir da raiz que contém api e mobile, execute os testes da API com uma política limitada ao processo:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File ".\api\Run-Api.ps1" -Test
```

Não é necessário clonar novamente para corrigir este erro. Antes do merge, envie os commits novos da API e confirme a suíte. Depois do merge, atualize o clone existente preservando alterações locais.

Pendente nesta entrega: credenciais reais, deploy, migração/exportação da base existente, configuração do volume e teste por rede pública.
