package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.NoteResponse;
import com.aistudybuddy.dto.StudyDTOs.SummarizeRequest;
import com.aistudybuddy.entity.StudyNotes;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.StudyNotesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotesServiceTest {

    @Mock
    private StudyNotesRepository notesRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private NotesService notesService;

    private User sampleUser;
    private StudyNotes sampleNote;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Alex Johnson", "alex@aistudybuddy.com", "secret123");
        sampleUser.setUserId(1L);

        sampleNote = new StudyNotes(sampleUser, "OS Deadlock", "Raw text about circular wait", "### Deadlock Summary\nKey points...");
        sampleNote.setNoteId(10L);
    }

    @Test
    @DisplayName("Should summarize notes via Gemini and save to MySQL")
    void testSummarizeAndSave() {
        SummarizeRequest request = new SummarizeRequest();
        request.setTitle("OS Deadlock");
        request.setContent("Raw text about circular wait");

        when(geminiService.summarizeNotes("OS Deadlock", "Raw text about circular wait"))
                .thenReturn("### Deadlock Summary\nKey points...");
        when(notesRepository.save(any(StudyNotes.class))).thenReturn(sampleNote);

        NoteResponse response = notesService.summarizeAndSave(sampleUser, request);

        assertNotNull(response);
        assertEquals("OS Deadlock", response.getTitle());
        assertTrue(response.getSummary().contains("Deadlock Summary"));
        verify(notesRepository, times(1)).save(any(StudyNotes.class));
    }

    @Test
    @DisplayName("Should retrieve only study notes belonging to authenticated user")
    void testGetUserNotes() {
        when(notesRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(List.of(sampleNote));

        List<NoteResponse> notes = notesService.getUserNotes(sampleUser);

        assertEquals(1, notes.size());
        assertEquals("OS Deadlock", notes.get(0).getTitle());
    }

    @Test
    @DisplayName("Should throw NoteNotFoundException if note not found or user unauthorized")
    void testGetNoteByIdUnauthorized() {
        when(notesRepository.findByNoteIdAndUser(99L, sampleUser)).thenReturn(Optional.empty());

        assertThrows(com.aistudybuddy.exception.NoteNotFoundException.class, () -> notesService.getNoteById(99L, sampleUser));
    }
}
