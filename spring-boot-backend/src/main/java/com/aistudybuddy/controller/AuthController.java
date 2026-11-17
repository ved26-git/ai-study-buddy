package com.aistudybuddy.controller;

import com.aistudybuddy.dto.AuthDTOs.*;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.aistudybuddy.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import static org.mockito.Mockito.verify;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummary> getCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(new UserSummary(user.getUserId(), user.getName(), user.getEmail(), user.getProfilePicture()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserSummary> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        UserSummary updated = authService.updateProfile(user, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully and token invalidated"));
    }
}
