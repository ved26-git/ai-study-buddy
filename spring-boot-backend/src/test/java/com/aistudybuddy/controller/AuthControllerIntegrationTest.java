
package com.aistudybuddy.controller;

import com.aistudybuddy.dto.AuthDTOs.*;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.security.JwtTokenProvider;
import com.aistudybuddy.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    @DisplayName("POST /api/auth/register should return 201 Created on valid input")
    void testRegisterEndpoint() throws Exception {

        RegisterRequest req = new RegisterRequest();
        req.setName("Alex Johnson");
        req.setEmail("alex@aistudybuddy.com");
        req.setPassword("password123");

        AuthResponse authResponse = new AuthResponse(
                "Registration successful",
                "mock_token",
                new UserSummary(1L, "Alex Johnson",
                        "alex@aistudybuddy.com", null)
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Registration successful"))
                .andExpect(jsonPath("$.token").value("mock_token"))
                .andExpect(jsonPath("$.user.name").value("Alex Johnson"));
    }

    @Test
    @DisplayName("POST /api/auth/logout should return 200 OK and invalidate token")
    void testLogoutEndpoint() throws Exception {

        String validToken = tokenProvider.generateToken(
                "alex@aistudybuddy.com",
                1L
        );

        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Logged out successfully and token invalidated"));

        verify(authService).logout("Bearer " + validToken);
    }

    @Test
    @WithMockUser(username = "alex@aistudybuddy.com")
    @DisplayName("GET /api/auth/me should return 200 OK with authenticated user summary")
    void testGetMeEndpoint() throws Exception {

        User user = new User(
                "Alex Johnson",
                "alex@aistudybuddy.com",
                "hash"
        );
        user.setUserId(1L);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(user);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex Johnson"))
                .andExpect(jsonPath("$.email")
                        .value("alex@aistudybuddy.com"));
    }

    @Test
    @WithMockUser(username = "alex@aistudybuddy.com")
    @DisplayName("PUT /api/auth/profile should update profile and return updated UserSummary")
    void testUpdateProfileEndpoint() throws Exception {

        User user = new User(
                "Alex Johnson",
                "alex@aistudybuddy.com",
                "hash"
        );
        user.setUserId(1L);

        UpdateProfileRequest updateReq = new UpdateProfileRequest();
        updateReq.setName("Alex Updated");
        updateReq.setProfilePicture(
                "/assets/images/student_avatar_male_1794205455718.jpg"
        );

        UserSummary updatedSummary = new UserSummary(
                1L,
                "Alex Updated",
                "alex@aistudybuddy.com",
                "/assets/images/student_avatar_male_1794205455718.jpg"
        );

        when(authService.getCurrentAuthenticatedUser()).thenReturn(user);

        when(authService.updateProfile(
                eq(user),
                any(UpdateProfileRequest.class)
        )).thenReturn(updatedSummary);

        mockMvc.perform(put("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex Updated"))
                .andExpect(jsonPath("$.profilePicture")
                        .value("/assets/images/student_avatar_male_1794205455718.jpg"));
    }
}
