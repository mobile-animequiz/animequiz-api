package com.animequiz.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tb_idempotent_attempts", uniqueConstraints =
        @UniqueConstraint(name = "uk_idempotent_user_key", columnNames = {"user_id", "request_key"}))
public class IdempotentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "request_key", nullable = false, length = 128)
    @NotBlank
    private String requestKey;
    @Column(nullable = false, length = 64)
    private String payloadHash;
    @Lob @Column(nullable = false)
    private String responseJson;
    public IdempotentAttempt() {}
    public IdempotentAttempt(Long userId, String requestKey, String payloadHash, String responseJson) {
        this.userId = userId; this.requestKey = requestKey; this.payloadHash = payloadHash; this.responseJson = responseJson;
    }
    public String getPayloadHash() { return payloadHash; }
    public String getResponseJson() { return responseJson; }
}
