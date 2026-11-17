
package com.aistudybuddy.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    private static final String TEST_SECRET =
            "test-secret-key-must-be-at-least-32-bytes-long";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();

        ReflectionTestUtils.setField(
                tokenProvider, "jwtSecret", TEST_SECRET
        );

        ReflectionTestUtils.setField(
                tokenProvider, "jwtExpirationMs", 3600000L
        );
    }

    @Test
    @DisplayName("Should generate and validate a valid JWT")
    void testGenerateAndValidateToken() {

        String token = tokenProvider.generateToken(
                "student@example.com", 1L
        );

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(
                "student@example.com",
                tokenProvider.getEmailFromToken(token)
        );
    }

    @Test
    @DisplayName("Should reject an expired JWT")
    void testExpiredTokenRejected() {

        ReflectionTestUtils.setField(
                tokenProvider, "jwtExpirationMs", -1000L
        );

        String expiredToken = tokenProvider.generateToken(
                "student@example.com", 1L
        );

        assertFalse(tokenProvider.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Should reject a malformed JWT")
    void testMalformedTokenRejected() {

        assertFalse(
                tokenProvider.validateToken("not.a.valid.jwt")
        );
    }

    @Test
    @DisplayName("Should reject a JWT signed with a different secret")
    void testTamperedTokenRejected() {

        String token = tokenProvider.generateToken(
                "student@example.com", 1L
        );

        JwtTokenProvider anotherProvider = new JwtTokenProvider();

        ReflectionTestUtils.setField(
                anotherProvider,
                "jwtSecret",
                "another-test-secret-key-at-least-32-bytes-long"
        );

        assertFalse(anotherProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject a missing JWT secret")
    void testMissingSecretRejected() {

        ReflectionTestUtils.setField(
                tokenProvider, "jwtSecret", ""
        );

        assertThrows(
                IllegalStateException.class,
                () -> tokenProvider.validateSecretKey()
        );
    }

    @Test
    @DisplayName("Should reject a weak JWT secret")
    void testWeakSecretRejected() {

        ReflectionTestUtils.setField(
                tokenProvider, "jwtSecret", "short-secret"
        );

        assertThrows(
                IllegalStateException.class,
                () -> tokenProvider.validateSecretKey()
        );
    }

    @Test
    @DisplayName("Should return JWT expiration date")
    void testGetExpirationDateFromToken() {

        String token = tokenProvider.generateToken(
                "student@example.com", 1L
        );

        Date expiration =
                tokenProvider.getExpirationDateFromToken(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));


    @Test
    @DisplayName("Should reject zero or negative JWT expiration configuration")
    void testInvalidExpirationRejected() {
    ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", 0L);

    assertThrows(
        IllegalStateException.class,
        () -> tokenProvider.validateSecretKey()
    );
}    
    }
}
