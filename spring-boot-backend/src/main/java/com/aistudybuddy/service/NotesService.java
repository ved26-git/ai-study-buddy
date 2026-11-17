package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.NoteResponse;
import com.aistudybuddy.dto.StudyDTOs.SummarizeRequest;
import com.aistudybuddy.entity.StudyNotes;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.StudyNotesRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotesService {

    private final StudyNotesRepository notesRepository;
    private final GeminiService geminiService;

    public NotesService(StudyNotesRepository notesRepository, GeminiService geminiService) {
        this.notesRepository = notesRepository;
        this.geminiService = geminiService;
    }

    public NoteResponse summarizeAndSave(User user, SummarizeRequest request) {
        String title = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
                ? request.getTitle().trim()
                : "Study Summary";

        String summary = geminiService.summarizeNotes(title, request.getContent());

        StudyNotes note = new StudyNotes(user, title, request.getContent(), summary);
        StudyNotes saved = notesRepository.save(note);

        return mapToDTO(saved);
    }

    public List<NoteResponse> getUserNotes(User user) {
        return notesRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public NoteResponse getNoteById(Long noteId, User user) {
        StudyNotes note = notesRepository.findByNoteIdAndUser(noteId, user)
                .orElseThrow(() -> new com.aistudybuddy.exception.NoteNotFoundException("Study note #" + noteId + " not found or access denied."));
        return mapToDTO(note);
    }

    public void deleteNote(Long noteId, User user) {
        StudyNotes note = notesRepository.findByNoteIdAndUser(noteId, user)
                .orElseThrow(() -> new com.aistudybuddy.exception.NoteNotFoundException("Study note #" + noteId + " not found or access denied."));
        notesRepository.delete(note);
    }

    private NoteResponse mapToDTO(StudyNotes note) {
        return new NoteResponse(
                note.getNoteId(),
                note.getTitle(),
                note.getOriginalContent(),
                note.getSummary(),
                note.getCreatedAt().toString()
        );
    }
}
