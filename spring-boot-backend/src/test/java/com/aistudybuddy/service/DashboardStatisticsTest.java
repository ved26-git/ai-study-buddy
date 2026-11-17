package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.DashboardStatsResponse;
import com.aistudybuddy.entity.Quiz;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.DoubtRepository;
import com.aistudybuddy.repository.QuizRepository;
import com.aistudybuddy.repository.StudyNotesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardStatisticsTest {

    @Mock
    private StudyNotesRepository notesRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private DoubtRepository doubtRepository;

    @InjectMocks
    private HistoryService historyService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Jane Doe", "jane@aistudybuddy.com", "pass123");
        sampleUser.setUserId(2L);
    }

    @Test
    @DisplayName("Should return 0% average and handle empty states safely without NaN or Infinity")
    void testGetDashboardStatsEmptyState() {
        when(notesRepository.countByUser(sampleUser)).thenReturn(0L);
        when(quizRepository.countByUserAndScoreIsNotNull(sampleUser)).thenReturn(0L);
        when(doubtRepository.countByUser(sampleUser)).thenReturn(0L);
        when(quizRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(Collections.emptyList());

        DashboardStatsResponse stats = historyService.getDashboardStats(sampleUser);

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalSummaries());
        assertEquals(0L, stats.getTotalQuizzes());
        assertEquals(0, stats.getAverageQuizScore());
        assertEquals(0L, stats.getTotalDoubts());
        assertFalse(Double.isNaN(stats.getAverageQuizScore()));
    }

    @Test
    @DisplayName("Should accurately calculate average quiz score across completed quizzes only")
    void testGetDashboardStatsAccurateAveraging() {
        // Quiz 1: 4/5 = 80%
        Quiz q1 = new Quiz(sampleUser, "Data Structures", "Medium", "[]", 5);
        q1.setScore(4);

        // Quiz 2: 5/5 = 100%
        Quiz q2 = new Quiz(sampleUser, "Algorithms", "Hard", "[]", 5);
        q2.setScore(5);

        // Quiz 3: In progress, not graded (score is null)
        Quiz q3 = new Quiz(sampleUser, "Database", "Easy", "[]", 5);
        q3.setScore(null);

        when(notesRepository.countByUser(sampleUser)).thenReturn(5L);
        when(quizRepository.countByUserAndScoreIsNotNull(sampleUser)).thenReturn(2L);
        when(doubtRepository.countByUser(sampleUser)).thenReturn(3L);
        when(quizRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(List.of(q1, q2, q3));

        DashboardStatsResponse stats = historyService.getDashboardStats(sampleUser);

        assertNotNull(stats);
        assertEquals(5L, stats.getTotalSummaries());
        assertEquals(2L, stats.getTotalQuizzes()); // Only completed quizzes counted
        // (80 + 100) / 2 = 90%
        assertEquals(90, stats.getAverageQuizScore());
        assertEquals(3L, stats.getTotalDoubts());
    }

    @Test
    @DisplayName("Should guard against division by zero if totalQuestions is zero")
    void testGetDashboardStatsZeroQuestionsGuard() {
        Quiz qZero = new Quiz(sampleUser, "Invalid Quiz", "Easy", "[]", 0);
        qZero.setScore(0);

        when(notesRepository.countByUser(sampleUser)).thenReturn(1L);
        when(quizRepository.countByUserAndScoreIsNotNull(sampleUser)).thenReturn(0L);
        when(doubtRepository.countByUser(sampleUser)).thenReturn(0L);
        when(quizRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(List.of(qZero));

        DashboardStatsResponse stats = historyService.getDashboardStats(sampleUser);

        assertNotNull(stats);
        assertEquals(0, stats.getAverageQuizScore());
    }
}
