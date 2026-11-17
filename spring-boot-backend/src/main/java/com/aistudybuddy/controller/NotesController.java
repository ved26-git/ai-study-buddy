package com.aistudybuddy.controller;

import com.aistudybuddy.dto.StudyDTOs.NoteResponse;
import com.aistudybuddy.dto.StudyDTOs.SummarizeRequest;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.service.AuthService;
import com.aistudybuddy.service.NotesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NotesController {

    private final NotesService notesService;
    private final AuthService authService;

    public NotesController(NotesService notesService, AuthService authService) {
        this.notesService = notesService;
        this.authService = authService;
    }

    @PostMapping("/summarize")
    public ResponseEntity<NoteResponse> summarizeNotes(@Valid @RequestBody SummarizeRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        NoteResponse response = notesService.summarizeAndSave(user, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getNotes() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(notesService.getUserNotes(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNoteById(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(notesService.getNoteById(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        notesService.deleteNote(id, user);
        return ResponseEntity.ok(Map.of("message", "Note deleted successfully"));
    }
}
