package com.animequiz.api.exception;

import com.animequiz.api.dto.APIError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

/**
 * Traduz exceções em respostas {@link APIError}.
 *
 * <p>Regra: erro do cliente nunca vira 500. Só o que é realmente inesperado cai no
 * handler genérico, e ali o erro é registrado em log (antes era engolido).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<APIError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<APIError> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(GoneException.class)
    public ResponseEntity<APIError> handleGone(GoneException ex) {
        return build(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados de requisição inválidos");
        return build(HttpStatus.BAD_REQUEST, detail);
    }

    /** JSON malformado, corpo ausente ou valor de enum inválido no corpo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente, malformado ou com valores inválidos");
    }

    /** Parâmetro de rota/query com tipo inválido, por exemplo {@code /quizzes/difficulty/XYZ}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'");
    }

    /** Parâmetro de query obrigatório ausente, por exemplo {@code GET /quizzes/search} sem {@code title}. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIError> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Você não possui permissão para executar esta operação");
    }

    /** Rede de segurança: violação de integridade que escapou das verificações explícitas. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIError> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados", ex);
        return build(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados");
    }

    /**
     * Demais exceções. Exceções do Spring MVC que carregam status próprio
     * ({@link ErrorResponse}: 404 de rota inexistente, 405, 415, parâmetro obrigatório ausente...)
     * mantêm o status 4xx; o restante é erro interno.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGeneric(Exception ex) {
        // Ordenação por propriedade inexistente (?sort=xyz): o Spring Data a lança de dentro do repositório,
        // às vezes embrulhada em InvalidDataAccessApiUsageException. É erro do cliente, não do servidor.
        PropertyReferenceException invalidSort = findCause(ex, PropertyReferenceException.class);
        if (invalidSort != null) {
            return build(HttpStatus.BAD_REQUEST,
                    "Propriedade de ordenação inválida: '" + invalidSort.getPropertyName() + "'");
        }

        // Validação de entidade (Bean Validation no persist/commit) é erro do cliente, não do servidor.
        ConstraintViolationException violation = findCause(ex, ConstraintViolationException.class);
        if (violation != null) {
            String detail = violation.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Dados de requisição inválidos");
            return build(HttpStatus.BAD_REQUEST, detail);
        }

        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = errorResponse.getStatusCode();
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
            // Preserva cabeçalhos exigidos pelo protocolo, como Allow no 405.
            builder.headers(errorResponse.getHeaders());
            return builder.body(apiError(status, clientMessage(status)));
        }

        log.error("Erro interno não tratado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor");
    }

    /** Procura, na cadeia de causas (incluindo a própria exceção), a primeira do tipo informado. */
    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return null;
    }

    private static String clientMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Requisição inválida";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método HTTP não suportado para este recurso";
            case 406 -> "Formato de resposta não suportado";
            case 415 -> "Tipo de conteúdo não suportado";
            default -> "Requisição não pôde ser atendida";
        };
    }

    private static ResponseEntity<APIError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(apiError(status, message));
    }

    private static APIError apiError(HttpStatusCode status, String message) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return new APIError(status.value(), reason, message, LocalDateTime.now());
    }
}
