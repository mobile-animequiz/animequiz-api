package com.animequiz.api.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="password_reset_tokens")
public class PasswordResetToken {
    @Id private Long userId;
    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="userId",insertable=false,updatable=false)
    @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE)
    private User owner;
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    public PasswordResetToken() {}
    public PasswordResetToken(Long id,String hash,Instant expiry) { userId=id; tokenHash=hash; expiresAt=expiry; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
}

