package com.animequiz.api.service;

import com.animequiz.api.entity.ApiKey;
import com.animequiz.api.entity.User;
import com.animequiz.api.exception.ResourceNotFoundException;
import com.animequiz.api.repository.ApiKeyRepository;
import com.animequiz.api.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Base64;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {
    public record CreatedKey(Long id, String apiKey, String warning) {}
    public record KeyInfo(Long id, java.time.Instant createdAt, boolean revoked) {}
    private final UserRepository users;
    private final ApiKeyRepository keys;
    private final SecureRandom random = new SecureRandom();
    public ApiKeyService(UserRepository users, ApiKeyRepository keys) {
        this.users = users; this.keys = keys;
    }
    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public CreatedKey create(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = "aq_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ApiKey saved = keys.save(new ApiKey(user, hash(raw)));
        return new CreatedKey(saved.getId(), raw, "Guarde esta chave agora: ela nao sera exibida novamente");
    }
    @Transactional(readOnly = true)
    public Page<KeyInfo> list(Long userId, Pageable pageable) {
        return keys.findByOwnerId(userId, pageable).map(key -> new KeyInfo(key.getId(), key.getCreatedAt(), key.isRevoked()));
    }
    @Transactional
    public void revoke(Long userId, Long keyId) {
        ApiKey key = keys.findByIdAndOwnerId(keyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chave nao encontrada"));
        key.revoke();
    }
    @Transactional(readOnly = true)
    public KeyInfo verify(String raw) {
        if (raw == null || !raw.startsWith("aq_") || raw.length() > 128)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Chave de API invalida");
        ApiKey key = keys.findByKeyHashAndRevokedFalse(hash(raw))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Chave de API invalida ou revogada"));
        return new KeyInfo(key.getId(), key.getCreatedAt(), key.isRevoked());
    }
}
