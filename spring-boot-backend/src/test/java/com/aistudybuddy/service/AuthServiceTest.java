package com.aistudybuddy.service;

import com.aistudybuddy.dto.AuthDTOs.*;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.UserRepository;
import com.aistudybuddy.security.JwtTokenProvider;
import com.aistudybuddy.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Alex Johnson", "alex@aistudybuddy.com", "encodedSecret123", "/assets/images/avatar.jpg");
        sampleUser.setUserId(1L);
    }

    @Test
    @DisplayName("Should successfully register a new student account")
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alex Johnson");
        req.setEmail("alex@aistudybuddy.com");
        req.setPassword("password123");
        req.setProfilePicture("/assets/images/avatar.jpg");

        when(userRepository.existsByEmail("alex@aistudybuddy.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedSecret123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateToken(anyString(), any())).thenReturn("mock_jwt_token");

        AuthResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("Registration successful", response.getMessage());
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals("Alex Johnson", response.getUser().getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if email already registered")
    void testRegisterDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alex Johnson");
        req.setEmail("alex@aistudybuddy.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("alex@aistudybuddy.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate valid credentials and issue JWT")
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alex@aistudybuddy.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("alex@aistudybuddy.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encodedSecret123")).thenReturn(true);
        when(tokenProvider.generateToken("alex@aistudybuddy.com", 1L)).thenReturn("mock_jwt_token");

        AuthResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("mock_jwt_token", response.getToken());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException on invalid password")
    void testLoginInvalidPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alex@aistudybuddy.com");
        req.setPassword("wrongPassword");

        when(userRepository.findByEmail("alex@aistudybuddy.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", "encodedSecret123")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Should resolve current user from Spring SecurityContextHolder")
    void testGetCurrentAuthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("alex@aistudybuddy.com");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("alex@aistudybuddy.com")).thenReturn(Optional.of(sampleUser));

        User currentUser = authService.getCurrentAuthenticatedUser();

        assertNotNull(currentUser);
        assertEquals("alex@aistudybuddy.com", currentUser.getEmail());
    }

    @Test
    @DisplayName("Should update user profile in MySQL database")
    void testUpdateProfileSuccess() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Alex J. Updated");
        req.setProfilePicture("/assets/images/new_avatar.jpg");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummary summary = authService.updateProfile(sampleUser, req);

        assertNotNull(summary);
        assertEquals("Alex J. Updated", summary.getName());
        assertEquals("/assets/images/new_avatar.jpg", summary.getProfilePicture());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Should invalidate JWT token on logout via TokenBlacklistService")
    void testLogoutBlacklistsToken() {
        String token = "valid_token_string";
        Date expiry = new Date(System.currentTimeMillis() + 3600000);

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getExpirationDateFromToken(token)).thenReturn(expiry);

        authService.logout("Bearer " + token);

        verify(tokenBlacklistService, times(1)).blacklistToken(token, expiry);
    }
}
