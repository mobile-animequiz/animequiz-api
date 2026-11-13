package com.animequiz.api.service;

import com.animequiz.api.dto.AttemptRequest;
import com.animequiz.api.dto.AttemptResponse;
import com.animequiz.api.entity.IdempotentAttempt;
import com.animequiz.api.exception.BadRequestException;
import com.animequiz.api.exception.ConflictException;
import com.animequiz.api.repository.IdempotentAttemptRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Guarda resultado no H2 e impede repeticao com mesma chave por usuario nesta instancia. */
@Service
public class IdempotentAttemptService {
    public record Outcome(AttemptResponse result, boolean replayed) {}
    private final IdempotentAttemptRepository records;
    private final AttemptService attempts;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    public IdempotentAttemptService(IdempotentAttemptRepository records, AttemptService attempts,
                                    ObjectMapper mapper, org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.records = records; this.attempts = attempts; this.mapper = mapper;
        this.transactions = new TransactionTemplate(transactionManager);
    }
    // Sincronizacao local + transacao concluida dentro do monitor: adequado para uma instancia de demonstracao.
    public synchronized Outcome submit(Long quizId, Long userId, AttemptRequest request, String key) {
        if (key == null || key.isBlank()) return new Outcome(attempts.submit(quizId, userId, request), false);
        if (key.length() > 128 || !key.matches("[A-Za-z0-9_-]{8,128}"))
            throw new BadRequestException("X-Idempotency-Key deve ter de 8 a 128 caracteres (letras, numeros, _ ou -)");
        String payload = json(request) + ":quiz=" + quizId;
        String payloadHash = ApiKeyService.hash(payload);
        return transactions.execute(status -> {
            var existing = records.findByUserIdAndRequestKey(userId, key);
            if (existing.isPresent()) {
                if (!existing.get().getPayloadHash().equals(payloadHash))
                    throw new ConflictException("X-Idempotency-Key ja usada com outro conteudo");
                try { return new Outcome(mapper.readValue(existing.get().getResponseJson(), AttemptResponse.class), true); }
                catch (JsonProcessingException e) { throw new IllegalStateException("Resposta armazenada invalida", e); }
            }
            AttemptResponse response = attempts.submit(quizId, userId, request);
            records.save(new IdempotentAttempt(userId, key, payloadHash, json(response)));
            return new Outcome(response, false);
        });
    }
    private String json(Object object) {
        try { return mapper.writeValueAsString(object); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Falha ao serializar dados", e); }
    }
}
