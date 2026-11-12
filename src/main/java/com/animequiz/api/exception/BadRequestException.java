package com.animequiz.api.exception;

/** Requisição sintaticamente válida, mas que viola uma regra de negócio (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
