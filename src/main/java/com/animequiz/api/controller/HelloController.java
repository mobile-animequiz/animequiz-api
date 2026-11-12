package com.animequiz.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Status", description = "Verificação simples de que a API está no ar")
public class HelloController {

    @GetMapping("/hello")
    @Operation(summary = "Mensagem de boas-vindas",
            description = "Retorna um texto simples (`text/plain`). Serve apenas para conferir que a API responde.")
    public String sayHello() {
        return "Bem-vindo à API do AnimeQuiz!";
    }
}
