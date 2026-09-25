# AnimeQuiz API
## Processos para Gerar o Projeto Spring Boot e Testar o Mesmo

1. **Geração do Projeto:**
   Primeiro você entra no site Spring Initializr https://start.spring.io/. Escolho o projeto maven, linguagem java e escolha a versão mais recente do Spring boot.

2. **Passos para verificar se a API está rodando no localhost:**
   Primeiro eu vou até a pasta `src/main/java` e dentro dela eu crio uma pasta chamada `Controller` e na mesma eu posso criar um `HelloController` para testar a minha API.

   Seguindo esse código:

   ```java
   package com.animequiz.api.controller;

   import org.springframework.web.bind.annotation.GetMapping;
   import org.springframework.web.bind.annotation.RequestMapping;
   import org.springframework.web.bind.annotation.RestController;

   @RestController
   @RequestMapping("/api")
   public class hellocontroller {

       @GetMapping("/hello")
       public String sayHello() {
           return "Bem-vindo à API do AnimeQuiz!";
       }
   }
