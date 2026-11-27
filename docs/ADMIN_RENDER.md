# Administrador no Render

## Por que o login devolvia 401

O inicializador de demonstração `LocalAdminInitializer` só cria `isaac@gmail.com` nos perfis local/dev. Fazer merge e publicar no Render com perfil prod não cria essa conta. O login devolve a mesma mensagem para conta inexistente ou senha errada; não permite descobrir qual caso ocorreu.

O novo `AdminBootstrapInitializer` oferece provisionamento explícito por variáveis do servidor. Está desativado por padrão, funciona também em prod e não acrescenta endpoints públicos. O inicializador local continua bloqueado em produção. A senha pública `123123123` não é aceita nesse provisionamento.

## Publicar e configurar

1. Envie a branch da correção, abra o PR para main e faça merge com as verificações aprovadas.
2. No serviço da API no Render, abra Environment e adicione os valores abaixo. Mantenha SPRING_PROFILES_ACTIVE=prod e o DB_URL existente; não troque de banco para criar a conta.

| Variável | Valor |
|---|---|
| ADMIN_BOOTSTRAP_ENABLED | true |
| ADMIN_BOOTSTRAP_EMAIL | isaac@gmail.com |
| ADMIN_BOOTSTRAP_USERNAME | isaac |
| ADMIN_BOOTSTRAP_PASSWORD | Uma senha nova e exclusiva, preferencialmente aleatória, de pelo menos 16 caracteres e no máximo 72 bytes UTF-8 |
| ADMIN_BOOTSTRAP_UPDATE_EXISTING | false por padrão; veja a seção seguinte |

3. Salve e publique usando o código que contém esta correção. Não envie a senha por chat nem a coloque no GitHub ou mobile.
4. Nos logs, procure `Admin bootstrap: administrator created`. Entre no Swagger V2 por POST /auth/login com o e-mail configurado e a senha nova. Copie apenas accessToken para Authorize.
5. Confirme que o token permite criar o quiz. Depois defina ADMIN_BOOTSTRAP_ENABLED=false, remova ADMIN_BOOTSTRAP_PASSWORD e ADMIN_BOOTSTRAP_UPDATE_EXISTING do ambiente e publique novamente. A conta e o hash continuam no banco; o bootstrap não é necessário para fazer login.

A senha deve conter pelo menos cinco caracteres distintos, não pode estar em branco e deve respeitar o limite em bytes do BCrypt. Valores inválidos interrompem a inicialização com erro de configuração, sem imprimir a senha.

## Se o e-mail já existia

Por padrão, não redefine senha nem promove contas existentes. Um aviso nos logs informa `email already exists; account unchanged`. Se você pretende explicitamente tornar **essa conta configurada** administradora e redefinir sua senha, adicione ADMIN_BOOTSTRAP_UPDATE_EXISTING=true junto da nova senha e publique. Essa operação conserva ID, nome existente, perfil, favoritos e histórico; altera papel/senha e revoga as sessões anteriores. Remova a autorização de atualização e a senha após confirmar o acesso.

Se o administrador já possui o papel ADMIN e a senha configurada, reiniciar não altera o hash nem revoga sessões novamente. Se o nome isaac estiver ocupado por outra conta de e-mail diferente, escolha outro ADMIN_BOOTSTRAP_USERNAME; o bootstrap não toma essa outra conta.

## Persistência e diagnóstico

- 200 no login: use accessToken em Authorize, sem colocar e-mail/senha nesse campo.
- 401: credenciais incorretas ou conta ainda não provisionada nessa instância. Confira os logs e o endereço da requisição.
- 403 ao criar: o token não possui ADMIN; entre novamente após o provisionamento.
- 429: aguarde o limite de autenticação; não desative a proteção para tentar senhas repetidamente.
- O banco precisa persistir entre deploys. Se DB_URL apontar para um H2 em disco efêmero, o próximo deploy pode perder contas e quizzes. Use o volume persistente já previsto pelo projeto, sem mudar um datasource que contém dados sem planejar sua transferência.

Nenhuma mudança no CSS, nas versões de Swagger, nos contratos do mobile ou nas permissões de cadastro público é necessária. O cadastro continua criando USER.
