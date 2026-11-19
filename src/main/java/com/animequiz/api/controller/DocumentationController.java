package com.animequiz.api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Stable public entry points for the unmodified Swagger UI. */
@Controller
@Hidden
public class DocumentationController {
    @GetMapping({"/", "/docs", "/docs.html"})
    public ResponseEntity<Void> swagger() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui.html"))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}

