package com.animequiz.api.exception;

/** Endpoint removido do contrato público (HTTP 410). A mensagem indica a alternativa. */
public class GoneException extends RuntimeException {
    public GoneException(String message) {
        super(message);
    }
}
