package com.aistudybuddy.controller;

import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.DoubtSolverService;
import com.aistudybuddy.service.HistoryService;
import com.aistudybuddy.service.NotesService;
import com.aistudybuddy.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;
    private final NotesService notesService;
    private final QuizService quizService;
    private final DoubtSolverService doubtSolverService;
    private final AuthService authService;

    public HistoryController(HistoryService historyService, NotesService notesService,
                             QuizService quizService, DoubtSolverService doubtSolverService,
                             AuthService authService) {
        this.historyService = historyService;
        this.notesService = notesService;
        this.quizService = quizService;
        this.doubtSolverService = doubtSolverService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHistory() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(historyService.getUserHistory(user));
    }

    @DeleteMapping("/notes/{id}")
    public ResponseEntity<?> deleteNoteHistory(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        notesService.deleteNote(id, user);
        return ResponseEntity.ok(Map.of("message", "Note removed from history"));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<?> deleteQuizHistory(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        quizService.deleteQuiz(id, user);
        return ResponseEntity.ok(Map.of("message", "Quiz removed from history"));
    }

    @DeleteMapping("/doubts/{id}")
    public ResponseEntity<?> deleteDoubtHistory(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        doubtSolverService.deleteDoubt(id, user);
        return ResponseEntity.ok(Map.of("message", "Doubt conversation removed from history"));
    }
}
