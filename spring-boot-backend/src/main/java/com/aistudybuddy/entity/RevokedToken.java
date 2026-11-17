package com.aistudybuddy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity representing revoked JWT tokens.
 * Persisted in MySQL to ensure token revocation survives application restarts.
 */
@Entity
@Table(name = "revoked_tokens", indexes = {
        @Index(name = "idx_revoked_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_revoked_expiry", columnList = "expiry_date")
})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RevokedToken() {}

    public RevokedToken(String tokenHash, LocalDateTime expiryDate) {
        this.tokenHash = tokenHash;
        this.expiryDate = expiryDate;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
