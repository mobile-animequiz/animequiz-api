package com.animequiz.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
@Table(name = "tb_api_keys")
public class ApiKey {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
    @NotBlank
    @Column(nullable = false, unique = true, length = 64)
    private String keyHash;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private boolean revoked = false;
    public ApiKey() {}
    public ApiKey(User owner, String keyHash) { this.owner = owner; this.keyHash = keyHash; }
    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getKeyHash() { return keyHash; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRevoked() { return revoked; }
    public void revoke() { this.revoked = true; }
}
