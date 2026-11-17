package com.aistudybuddy.controller;

import com.aistudybuddy.dto.StudyDTOs.DoubtRequest;
import com.aistudybuddy.dto.StudyDTOs.DoubtResponse;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.DoubtSolverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doubts")
public class DoubtSolverController {

    private final DoubtSolverService doubtSolverService;
    private final AuthService authService;

    public DoubtSolverController(DoubtSolverService doubtSolverService, AuthService authService) {
        this.doubtSolverService = doubtSolverService;
        this.authService = authService;
    }

    @PostMapping("/ask")
    public ResponseEntity<DoubtResponse> askDoubt(@Valid @RequestBody DoubtRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        DoubtResponse response = doubtSolverService.solveDoubt(user, request.getQuestion());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DoubtResponse>> getDoubtHistory() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(doubtSolverService.getUserDoubts(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoubt(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        doubtSolverService.deleteDoubt(id, user);
        return ResponseEntity.ok(Map.of("message", "Doubt conversation deleted successfully"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAllDoubts() {
        User user = authService.getCurrentAuthenticatedUser();
        doubtSolverService.clearUserDoubts(user);
        return ResponseEntity.ok(Map.of("message", "All doubt chat history cleared"));
    }
}
