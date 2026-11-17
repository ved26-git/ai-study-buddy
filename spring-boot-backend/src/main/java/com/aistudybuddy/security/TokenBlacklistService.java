
package com.aistudybuddy.security;

import com.aistudybuddy.entity.RevokedToken;
import com.aistudybuddy.repository.RevokedTokenRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private static final Logger log =
            LoggerFactory.getLogger(TokenBlacklistService.class);

    private final RevokedTokenRepository revokedTokenRepository;

    private final Map<String, Long> localCache =
            new ConcurrentHashMap<>();

    public TokenBlacklistService(
            RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public void blacklistToken(String token, Date expirationDate) {

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        if (expirationDate == null) {
            throw new IllegalArgumentException(
                    "Token expiration date is required.");
        }

        long expiryTime = expirationDate.getTime();

        if (expiryTime <= System.currentTimeMillis()) {
            throw new IllegalArgumentException(
                    "Cannot revoke an already expired token.");
        }

        String hash = hashToken(token.trim());

        LocalDateTime expiryLocal = LocalDateTime.ofInstant(
                expirationDate.toInstant(),
                ZoneId.systemDefault());

        try {
            var existing =
                    revokedTokenRepository.findByTokenHashAndExpiryDateAfter(
                            hash, LocalDateTime.now());

            if (existing.isEmpty()) {
                revokedTokenRepository.save(
                        new RevokedToken(hash, expiryLocal));
            }

            localCache.put(hash, expiryTime);
            cleanExpiredCache();

            log.info("JWT token successfully revoked.");

        } catch (Exception ex) {
            localCache.remove(hash);
            log.error("Could not persist JWT revocation.", ex);

            throw new IllegalStateException(
                    "Token revocation is temporarily unavailable.", ex);
        }
    }

    public boolean isBlacklisted(String token) {

        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String hash = hashToken(token.trim());

        Long cachedExpiry = localCache.get(hash);

        if (cachedExpiry != null) {
            if (cachedExpiry > System.currentTimeMillis()) {
                return true;
            }

            localCache.remove(hash, cachedExpiry);
        }

        try {
            var revokedToken =
                    revokedTokenRepository.findByTokenHashAndExpiryDateAfter(
                            hash, LocalDateTime.now());

            if (revokedToken.isPresent()) {

                LocalDateTime expiryDate =
                        revokedToken.get().getExpiryDate();

                long expiryTime = expiryDate
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();

                if (expiryTime > System.currentTimeMillis()) {
                    localCache.put(hash, expiryTime);
                    return true;
                }
            }

            return false;

        } catch (Exception ex) {
            log.error(
                    "Database unavailable during JWT revocation check.",
                    ex);

            throw new IllegalStateException(
                    "Token verification is temporarily unavailable.", ex);
        }
    }

    public void purgeExpired() {

        cleanExpiredCache();

        try {
            int deleted =
                    revokedTokenRepository.deleteExpiredTokens(
                            LocalDateTime.now());

            if (deleted > 0) {
                log.info("Purged {} expired revoked tokens.", deleted);
            }

        } catch (Exception ex) {
            log.warn("Could not purge expired token records.", ex);
        }
    }

    private void cleanExpiredCache() {

        long now = System.currentTimeMillis();

        localCache.entrySet().removeIf(
                entry -> entry.getValue() <= now);
    }

    private static String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable.", ex);
        }
    }
}
