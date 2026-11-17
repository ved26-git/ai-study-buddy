package com.aistudybuddy.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Protected endpoint GET /api/notes should return 401 Unauthorized when no JWT token provided")
    void testProtectedNotesEndpointUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized access. Please provide a valid Bearer token."));
    }

    @Test
    @DisplayName("Protected endpoint GET /api/dashboard/stats should return 401 Unauthorized when no JWT token provided")
    void testProtectedDashboardEndpointUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized access. Please provide a valid Bearer token."));
    }

    @Test
    @DisplayName("Protected endpoint POST /api/quiz/generate should return 401 Unauthorized when no JWT token provided")
    void testProtectedQuizEndpointUnauthorized() throws Exception {
        mockMvc.perform(post("/api/quiz/generate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized access. Please provide a valid Bearer token."));
    }

    @Test
    @DisplayName("Protected endpoint GET /api/doubts/history should return 401 Unauthorized when no JWT token provided")
    void testProtectedDoubtsEndpointUnauthorized() throws Exception {
        mockMvc.perform(get("/api/doubts/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized access. Please provide a valid Bearer token."));
    }
}
