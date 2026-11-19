package com.animequiz.api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** V1 uses standard Swagger; V2 preserves the project's original presentation. */
@Controller
@Hidden
public class DocumentationController {
    @GetMapping({"/", "/docs", "/docs.html"})
    public ResponseEntity<Void> swagger(@RequestParam(defaultValue = "1") String version) {
        return redirect("2".equals(version) ? "/docs/v2" : "/swagger-ui.html");
    }

    @GetMapping("/docs/v1")
    public ResponseEntity<Void> v1() {
        return redirect("/swagger-ui.html");
    }

    @GetMapping("/docs/v2")
    public String v2() {
        return "forward:/docs-v2.html";
    }

    private ResponseEntity<Void> redirect(String target) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}

