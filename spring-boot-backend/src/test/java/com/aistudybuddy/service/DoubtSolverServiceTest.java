package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.DoubtResponse;
import com.aistudybuddy.entity.Doubt;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.DoubtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoubtSolverServiceTest {

    @Mock
    private DoubtRepository doubtRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private DoubtSolverService doubtSolverService;

    private User sampleUser;
    private Doubt sampleDoubt;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Alex Johnson", "alex@aistudybuddy.com", "secret123");
        sampleUser.setUserId(1L);

        sampleDoubt = new Doubt(sampleUser, "What is polymorphism?", "Polymorphism allows objects to take multiple forms.");
        sampleDoubt.setDoubtId(20L);
    }

    @Test
    @DisplayName("Should generate explanation via Gemini and persist conversation in MySQL doubts table")
    void testSolveDoubtAndPersist() {
        when(geminiService.solveDoubt("What is polymorphism?")).thenReturn("Polymorphism allows objects to take multiple forms.");
        when(doubtRepository.save(any(Doubt.class))).thenReturn(sampleDoubt);

        DoubtResponse response = doubtSolverService.solveDoubt(sampleUser, "What is polymorphism?");

        assertNotNull(response);
        assertEquals(20L, response.getDoubtId());
        assertEquals("What is polymorphism?", response.getQuestion());
        assertTrue(response.getAnswer().contains("Polymorphism"));
        verify(doubtRepository, times(1)).save(any(Doubt.class));
    }

    @Test
    @DisplayName("Should delete doubt when user owns the record")
    void testDeleteDoubtSuccess() {
        when(doubtRepository.findByDoubtIdAndUser(20L, sampleUser)).thenReturn(java.util.Optional.of(sampleDoubt));

        doubtSolverService.deleteDoubt(20L, sampleUser);

        verify(doubtRepository, times(1)).delete(sampleDoubt);
    }

    @Test
    @DisplayName("Should throw DoubtNotFoundException when doubt does not exist or user unauthorized")
    void testDeleteDoubtNotFound() {
        when(doubtRepository.findByDoubtIdAndUser(99L, sampleUser)).thenReturn(java.util.Optional.empty());

        assertThrows(com.aistudybuddy.exception.DoubtNotFoundException.class, () ->
                doubtSolverService.deleteDoubt(99L, sampleUser));
    }
}
