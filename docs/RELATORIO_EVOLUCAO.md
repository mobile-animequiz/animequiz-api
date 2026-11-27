# Verificação da revisão integrada — 26/11/2026

## Resultados

| Verificação | Resultado |
|---|---|
| Testes Java JUnit/MockMvc/JPA/HTTP | 321 aprovados, zero falhas/erros/ignorados |
| Navegação da documentação, Node | 4 aprovados |
| Testes do mobile | 36 aprovados |
| Mobile: lint sem cache e TypeScript | Aprovados |
| Exportação web do mobile | Concluída |
| Configuração de APK e recursos existentes | Validada; não é um APK gerado |

A suíte final foi executada com os testes HTTP opcionais habilitados: 321 testes sem ignorados. O código foi compilado com alvo Java 17, usando o JDK completo do Android Studio disponível neste ambiente.

A correção de provisionamento no Render acrescentou 13 verificações: bootstrap desativado por padrão, senha BCrypt, idempotência, contas existentes preservadas sem autorização explícita, promoção/reset com revogação de sessões, validação de senha/e-mail e colisão de nome. A integração com perfil prod confirmou login e criação de quiz pelo ADMIN, além do bloqueio de criação para USER. As variáveis reais do Render ainda precisam ser definidas pelo operador; os testes não alteraram o servidor publicado.

Foram exercitados: separação dos documentos V1/V2, DTO básico independente, validação de versão/idioma, autorização ADMIN/USER, idempotência do seed local, hash BCrypt e bloqueio do seed em prod, catálogo bilingue com IDs estáveis, mídia PNG, pontuação/XP, favoritos, upload multipart real e validações de imagem.

Na integração web com API real local, 23 verificações passaram: criação de quiz pelo ADMIN (201), recusa para USER (403), login pela interface, perfil, seleção de inglês, tema Oceano, destaque Menta, nome personalizado, galeria, foto visível, remoção salva, idioma após reabrir, catálogo traduzido e partida completa em inglês com XP oficial. Os testes usam banco em memória separado, sem alterar produção.

## Limites desta validação

Não houve teste em aparelho físico nem geração ou assinatura de APK. Não houve publicação no Render. Os fluxos externos de Google OAuth e envio de e-mail não foram validados com credenciais reais. O novo ícone ainda depende de aprovação; recursos existentes foram preservados. O cronômetro controla a interface, não garante tempo em clientes alterados.

Antes da apresentação, publique esta API e aponte o mobile para a mesma instância. O ADMIN de demonstração só existe em local/dev com senha no ambiente. Em produção, provisione ADMIN com senha forte por procedimento controlado e mantenha o perfil prod. O Swagger continua sendo documentação; a proteção real fica em Spring Security e services.

## Comandos para repetir

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Run-Api.ps1 -Test
node --test src/test/js/docs-navigation.test.cjs
```

O Bypass acima vale somente para o processo chamado; não altera permanentemente a política do Windows. Para incluir os dois testes HTTP opcionais, use um JDK completo e `mvnw.cmd clean test -Danimequiz.http-tests=true`. Evite trocar o banco de produção para executar verificações.

Veja [o guia funcional](EVOLUCAO_2026-11-26.md) para variáveis, chamadas administrativas e a estratégia de tradução/capas; [README_V1.md](../README_V1.md) documenta exclusivamente a apresentação básica.
