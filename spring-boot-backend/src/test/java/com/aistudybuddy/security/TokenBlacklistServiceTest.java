
package com.aistudybuddy.security;

import com.aistudybuddy.entity.RevokedToken;
import com.aistudybuddy.repository.RevokedTokenRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService =
                new TokenBlacklistService(revokedTokenRepository);
    }

    @Test
    void blacklistToken_shouldSaveTokenHash() {

        String token = "test-jwt-token";
        Date expiry = Date.from(
                java.time.Instant.now().plusSeconds(3600));

        when(revokedTokenRepository
                .findByTokenHashAndExpiryDateAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        tokenBlacklistService.blacklistToken(token, expiry);

        verify(revokedTokenRepository, times(1))
                .save(any(RevokedToken.class));

        assertTrue(tokenBlacklistService.isBlacklisted(token));
    }

    @Test
    void blacklistToken_shouldNotSaveDuplicateActiveToken() {

        String token = "duplicate-jwt-token";
        Date expiry = Date.from(
                java.time.Instant.now().plusSeconds(3600));

        RevokedToken existing = new RevokedToken(
                "existing-hash",
                LocalDateTime.now().plusHours(1));

        when(revokedTokenRepository
                .findByTokenHashAndExpiryDateAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existing));

        tokenBlacklistService.blacklistToken(token, expiry);

        verify(revokedTokenRepository, never())
                .save(any(RevokedToken.class));

        assertTrue(tokenBlacklistService.isBlacklisted(token));
    }

    @Test
    void blacklistToken_shouldRejectNullExpiry() {

        assertThrows(
                IllegalArgumentException.class,
                () -> tokenBlacklistService.blacklistToken(
                        "test-token", null));
    }

    @Test
    void blacklistToken_shouldRejectAlreadyExpiredToken() {

        Date expiredDate = Date.from(
                java.time.Instant.now().minusSeconds(60));

        assertThrows(
                IllegalArgumentException.class,
                () -> tokenBlacklistService.blacklistToken(
                        "expired-token", expiredDate));
    }

    @Test
    void isBlacklisted_shouldReturnFalseForUnknownToken() {

        when(revokedTokenRepository
                .findByTokenHashAndExpiryDateAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertFalse(
                tokenBlacklistService.isBlacklisted("unknown-token"));
    }

    @Test
    void isBlacklisted_shouldFindPersistedRevocation() {

        String token = "persisted-jwt-token";

        RevokedToken savedToken = new RevokedToken(
                "test-hash",
                LocalDateTime.now().plusHours(1));

        when(revokedTokenRepository
                .findByTokenHashAndExpiryDateAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(savedToken));

        // A fresh service instance has an empty in-memory cache.
        TokenBlacklistService restartedService =
                new TokenBlacklistService(revokedTokenRepository);

        assertTrue(restartedService.isBlacklisted(token));
    }

    @Test
    void isBlacklisted_shouldReturnFalseWhenDatabaseRecordIsExpired() {

        when(revokedTokenRepository
                .findByTokenHashAndExpiryDateAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertFalse(
                tokenBlacklistService.isBlacklisted("expired-token"));
    }

    @Test
    void isBlacklisted_shouldFailClosedWhenDatabaseFails() {

        when(revokedTokenRepository
                .findByTokenHashAndExpiryDateAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> tokenBlacklistService.isBlacklisted(
                        "test-token"));
    }

    @Test
    void blacklistToken_shouldFailWhenDatabaseSaveFails() {

        String token = "save-failure-token";
        Date expiry = Date.from(
                java.time.Instant.now().plusSeconds(3600));

        when(revokedTokenRepository
                .findByTokenHashAndExpiryDateAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        when(revokedTokenRepository.save(any(RevokedToken.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> tokenBlacklistService.blacklistToken(
                        token, expiry));
    }
}
