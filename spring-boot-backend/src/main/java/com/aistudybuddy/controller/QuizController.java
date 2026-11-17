package com.aistudybuddy.controller;

import com.aistudybuddy.dto.StudyDTOs.*;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final AuthService authService;

    public QuizController(QuizService quizService, AuthService authService) {
        this.quizService = quizService;
        this.authService = authService;
    }

    /**
     * Generate an AI-powered quiz.
     * Response strictly conceals correct answers and explanations.
     */
    @PostMapping("/generate")
    public ResponseEntity<QuizAttemptResponse> generateQuiz(@Valid @RequestBody QuizGenerateRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        QuizAttemptResponse quiz = quizService.generateQuiz(user, request);
        return new ResponseEntity<>(quiz, HttpStatus.CREATED);
    }

    /**
     * Submit answers for server-side evaluation.
     * Only reveals explanations and answer key after grading.
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<QuizSubmitResponse> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        QuizSubmitResponse response = quizService.submitQuiz(id, user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * List student's quizzes using safe QuizListResponse DTOs.
     * Raw questions JSON with answer keys is never leaked.
     */
    @GetMapping
    public ResponseEntity<List<QuizListResponse>> getUserQuizzes() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(quizService.getUserQuizzes(user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<QuizListResponse>> getUserQuizzesHistory() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(quizService.getUserQuizzes(user));
    }

    /**
     * Safe retrieval of a specific quiz:
     * Returns QuizAttemptResponse if unsubmitted, or QuizSubmitResponse if graded.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuizById(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(quizService.getQuizDetailsSafe(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        quizService.deleteQuiz(id, user);
        return ResponseEntity.ok(Map.of("message", "Quiz deleted successfully"));
    }
}
