# AnimeQuiz API — V1

API de consultas de quizzes de anime desenvolvida em Java 17, Spring Boot 3.3, Spring Data JPA, H2 e Spring HATEOAS. A documentação técnica usa OpenAPI e a apresentação interativa usa Swagger UI.

## Escopo e autenticação

Esta interface oferece consultas públicas de catálogo, perguntas, alternativas e disponibilidade. Não exige login, JWT nem chave de API. Essas consultas não usam rate limiting. O gabarito das alternativas não é disponibilizado.

Envie `X-API-Version: 1` ou omita esse cabeçalho. Um valor de versão inválido produz HTTP 400. O idioma do conteúdo desta interface é português do Brasil.

| Acesso local | Endereço |
|---|---|
| Base da API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Especificação JSON | `http://localhost:8080/api-docs/v1` |

## Arquitetura

`Controller → Service → Repository → Banco de dados`.

- Controllers recebem os parâmetros HTTP e devolvem representações HATEOAS.
- Services verificam a existência dos recursos, aplicam as regras de consulta e constroem DTOs.
- Repositories usam Spring Data JPA para carregar entidades e páginas.
- DTOs definem o contrato público e impedem a exposição de campos internos.
- O tratamento global de exceções converte falhas em respostas JSON `APIError`.

O frontend deve consumir os DTOs; entidades JPA não são retornadas diretamente.

## Entidades e relacionamentos

| Entidade | Dados consultáveis | Relacionamentos |
|---|---|---|
| Quiz | ID, título, descrição, dificuldade, categoria, quantidade de perguntas | Um quiz possui muitas perguntas |
| Question | ID, quiz associado e enunciado | Uma pergunta pertence a um quiz e possui alternativas |
| Option | ID, pergunta associada e texto | Uma alternativa pertence a uma pergunta |

O banco armazena a indicação da alternativa correta, mas esse campo não integra as respostas públicas. A quantidade de perguntas é calculada, não enviada pelo cliente.

As dificuldades são `FACIL`, `MEDIO` e `DIFICIL`. Categorias aceitas: consulte o enum `Category` no código; o catálogo inclui, por exemplo, `SHONEN`.

## Endpoints

Todos os endpoints abaixo usam o método **GET**.

| Rota | Parâmetros / finalidade |
|---|---|
| `/api/hello` | Mensagem simples de disponibilidade |
| `/api/versioned/status` | Confirma versão e status |
| `/quizzes` | Lista paginada de quizzes |
| `/quizzes/{id}` | Consulta um quiz pelo ID |
| `/quizzes/search` | Busca parcial pelo parâmetro obrigatório `title` |
| `/quizzes/difficulty/{difficulty}` | Filtra pela dificuldade |
| `/quizzes/{quizId}/questions` | Perguntas com alternativas, prontas para leitura |
| `/questions` | Lista paginada de perguntas |
| `/questions/{id}` | Consulta uma pergunta |
| `/questions/by-quiz/{quizId}` | Lista perguntas de um quiz |
| `/options` | Lista paginada de alternativas |
| `/options/{id}` | Consulta uma alternativa |
| `/options/by-question/{questionId}` | Lista alternativas de uma pergunta |

## Exemplos de requisição e resposta

```bash
curl 'http://localhost:8080/quizzes?page=0&size=10' \
  -H 'X-API-Version: 1' -H 'Accept: application/json'

curl 'http://localhost:8080/quizzes/search?title=Naruto'
curl 'http://localhost:8080/quizzes/difficulty/MEDIO'
curl 'http://localhost:8080/quizzes/1/questions?page=0&size=20'
```

Um quiz consultado individualmente tem este formato. IDs e quantidades abaixo são ilustrativos:

```json
{
  "id": 1,
  "title": "Naruto Shippuden Quiz",
  "description": "Missão ninja: personagens e fundamentos.",
  "difficulty": "FACIL",
  "category": "SHONEN",
  "questionCount": 16,
  "_links": { "self": { "href": "http://localhost:8080/quizzes/1" } }
}
```

Um item de `/quizzes/{quizId}/questions` contém:

```json
{
  "id": 1,
  "quizId": 1,
  "quizTitle": "Naruto Shippuden Quiz",
  "text": "Qual é o sonho de Naruto?",
  "options": [
    { "id": 1, "text": "Ser Hokage" },
    { "id": 2, "text": "Ser um marinheiro" }
  ]
}
```

O exemplo resume as alternativas para facilitar a leitura. O catálogo inicial usa quatro alternativas por pergunta.

## Paginação e regras de consulta

- `page` começa em zero; `size` define o tamanho da página, com padrão 20.
- Listagens usam `_embedded` para os itens, `_links` para navegação e `page` para os metadados.
- Os metadados incluem `size`, `number`, `totalElements` e `totalPages`.
- Listas vazias podem omitir `_embedded`; trate isso como zero itens.
- Um quiz existente sem perguntas retorna 200 com página vazia.
- Um quiz inexistente retorna 404.
- Perguntas e alternativas da tela de leitura têm ordem estável por ID.
- A consulta de perguntas com alternativas usa uma consulta em lote para evitar uma chamada por pergunta.
- Não use a posição A/B/C/D como identificador permanente; cada alternativa possui seu próprio ID.

## Erros

| Código | Motivo |
|---|---|
| 400 | Versão, ID, dificuldade ou parâmetros inválidos |
| 404 | Recurso não encontrado |
| 500 | Falha inesperada; detalhes técnicos ficam no servidor |

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Quiz não encontrado com ID: 999",
  "timestamp": "2026-11-26T12:00:00"
}
```

## Como executar

Requisitos: JDK 17 completo e acesso às dependências Maven na primeira execução. O Maven Wrapper acompanha o projeto.

Windows, na pasta do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .\Run-Api.ps1
```

O launcher configura um JDK completo e um segredo efêmero para a inicialização da aplicação. Esse segredo não é necessário para consumir as consultas públicas. Para executar diretamente com Maven, configure `JWT_SECRET` com pelo menos 32 bytes no ambiente do processo antes de iniciar:

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
chmod +x mvnw
./mvnw spring-boot:run
```

O banco local padrão é H2 em `./data/animequizdb`. O catálogo inicial é preenchido de forma idempotente, preservando IDs existentes. `PORT` altera a porta; `DB_URL` altera o endereço JDBC. Não versione o banco local nem arquivos com segredos. O Spring Boot não carrega `.env` automaticamente.

## Testes e apresentação

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .\Run-Api.ps1 -Test
node --test src/test/js/docs-navigation.test.cjs
```

Na apresentação, abra o Swagger, consulte `/quizzes`, escolha um ID retornado, carregue suas perguntas e mostre que as alternativas não incluem o gabarito. Depois consulte um ID inexistente para demonstrar o tratamento HTTP 404.
