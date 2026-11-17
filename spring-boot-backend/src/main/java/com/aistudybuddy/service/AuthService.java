package com.aistudybuddy.service;

import com.aistudybuddy.dto.AuthDTOs.*;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.UserRepository;
import com.aistudybuddy.security.JwtTokenProvider;
import com.aistudybuddy.security.TokenBlacklistService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail().toLowerCase().trim())) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        String encodedPassword = passwordEncoder.encode(req.getPassword());
        User user = new User(req.getName().trim(), req.getEmail().toLowerCase().trim(), encodedPassword, req.getProfilePicture());
        User saved = userRepository.save(user);

        String token = tokenProvider.generateToken(saved.getEmail(), saved.getUserId());
        return new AuthResponse("Registration successful", token,
                new UserSummary(saved.getUserId(), saved.getName(), saved.getEmail(), saved.getProfilePicture()));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String token = tokenProvider.generateToken(user.getEmail(), user.getUserId());
        return new AuthResponse("Login successful", token,
                new UserSummary(user.getUserId(), user.getName(), user.getEmail(), user.getProfilePicture()));
    }

    /**
     * Updates profile data for the authenticated user and persists to MySQL.
     */
    public UserSummary updateProfile(User user, UpdateProfileRequest req) {
        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            user.setName(req.getName().trim());
        }
        if (req.getProfilePicture() != null) {
            user.setProfilePicture(req.getProfilePicture());
        }
        User updated = userRepository.save(user);
        return new UserSummary(updated.getUserId(), updated.getName(), updated.getEmail(), updated.getProfilePicture());
    }

    /**
     * Server-side logout: revokes the provided JWT until natural expiry via TokenBlacklistService.
     */
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7).trim();
            if (tokenProvider.validateToken(token)) {
                Date expiration = tokenProvider.getExpirationDateFromToken(token);
                tokenBlacklistService.blacklistToken(token, expiration);
            }
        }
    }

    /**
     * Resolves the current logged-in user from the authenticated Spring Security context.
     * Never trusts frontend headers or query parameters for user identity.
     */
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new BadCredentialsException("Access denied: User is not authenticated. Please log in.");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database: " + email));
    }
}
