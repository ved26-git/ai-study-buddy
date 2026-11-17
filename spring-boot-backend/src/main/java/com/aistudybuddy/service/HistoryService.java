package com.aistudybuddy.service;

import com.aistudybuddy.dto.StudyDTOs.DashboardStatsResponse;
import com.aistudybuddy.dto.StudyDTOs.DoubtResponse;
import com.aistudybuddy.dto.StudyDTOs.QuizListResponse;
import com.aistudybuddy.dto.StudyDTOs.RecentActivityDTO;
import com.aistudybuddy.entity.Doubt;
import com.aistudybuddy.entity.Quiz;
import com.aistudybuddy.entity.StudyNotes;
import com.aistudybuddy.entity.User;
import com.aistudybuddy.repository.DoubtRepository;
import com.aistudybuddy.repository.QuizRepository;
import com.aistudybuddy.repository.StudyNotesRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private final StudyNotesRepository notesRepository;
    private final QuizRepository quizRepository;
    private final DoubtRepository doubtRepository;

    public HistoryService(StudyNotesRepository notesRepository,
                          QuizRepository quizRepository,
                          DoubtRepository doubtRepository) {
        this.notesRepository = notesRepository;
        this.quizRepository = quizRepository;
        this.doubtRepository = doubtRepository;
    }

    /**
     * Aggregated user history. Uses safe DTOs for Quizzes and Doubts to prevent leaking answer keys.
     */
    public Map<String, Object> getUserHistory(User user) {
        List<StudyNotes> notes = notesRepository.findByUserOrderByCreatedAtDesc(user);
        List<Quiz> quizzes = quizRepository.findByUserOrderByCreatedAtDesc(user);
        List<Doubt> doubts = doubtRepository.findByUserOrderByCreatedAtDesc(user);

        List<QuizListResponse> quizDTOs = quizzes.stream()
                .map(q -> new QuizListResponse(
                        q.getQuizId(),
                        q.getTopic(),
                        q.getDifficulty(),
                        q.getTotalQuestions(),
                        q.getScore(),
                        q.getCreatedAt() != null ? q.getCreatedAt().toString() : null,
                        q.getCompletedAt() != null ? q.getCompletedAt().toString() : null
                ))
                .collect(Collectors.toList());

        List<DoubtResponse> doubtDTOs = doubts.stream()
                .map(d -> new DoubtResponse(d.getDoubtId(), d.getQuestion(), d.getAnswer(), d.getCreatedAt().toString()))
                .collect(Collectors.toList());

        Map<String, Object> history = new HashMap<>();
        history.put("summaries", notes);
        history.put("quizzes", quizDTOs);
        history.put("doubts", doubtDTOs);
        return history;
    }

    /**
     * Dashboard statistics calculated authoritatively from MySQL records.
     * Guaranteed safe arithmetic: returns 0% when empty, never emits NaN or Infinity.
     */
    public DashboardStatsResponse getDashboardStats(User user) {
        long totalSummaries = notesRepository.countByUser(user);
        long completedQuizzesCount = quizRepository.countByUserAndScoreIsNotNull(user);
        long totalDoubts = doubtRepository.countByUser(user);

        List<Quiz> quizzes = quizRepository.findByUserOrderByCreatedAtDesc(user);
        List<Quiz> completedQuizzes = quizzes.stream()
                .filter(q -> q.getScore() != null && q.getTotalQuestions() != null && q.getTotalQuestions() > 0)
                .toList();

        int averageQuizScore = 0;
        if (!completedQuizzes.isEmpty()) {
            double totalPercentage = 0.0;
            int validCount = 0;

            for (Quiz q : completedQuizzes) {
                if (q.getTotalQuestions() > 0) {
                    double pct = ((double) q.getScore() / q.getTotalQuestions()) * 100.0;
                    if (!Double.isNaN(pct) && !Double.isInfinite(pct)) {
                        totalPercentage += pct;
                        validCount++;
                    }
                }
            }

            if (validCount > 0) {
                double avg = totalPercentage / validCount;
                if (!Double.isNaN(avg) && !Double.isInfinite(avg)) {
                    averageQuizScore = (int) Math.round(avg);
                }
            }
        }

        return new DashboardStatsResponse(totalSummaries, completedQuizzesCount, averageQuizScore, totalDoubts);
    }

    public List<RecentActivityDTO> getRecentActivities(User user) {
        List<RecentActivityDTO> list = new ArrayList<>();

        notesRepository.findByUserOrderByCreatedAtDesc(user).stream().limit(5).forEach(n -> {
            list.add(new RecentActivityDTO("NOTE", n.getNoteId(), n.getTitle(), "Generated Study Notes Summary", n.getCreatedAt().toString()));
        });

        quizRepository.findByUserOrderByCreatedAtDesc(user).stream().limit(5).forEach(q -> {
            String detail = q.getScore() != null
                    ? String.format("Score: %d/%d (%d%%)", q.getScore(), q.getTotalQuestions(), Math.round(((float) q.getScore() / q.getTotalQuestions()) * 100))
                    : "Quiz in progress";
            list.add(new RecentActivityDTO("QUIZ", q.getQuizId(), q.getTopic(), detail, q.getCreatedAt().toString()));
        });

        doubtRepository.findByUserOrderByCreatedAtDesc(user).stream().limit(5).forEach(d -> {
            list.add(new RecentActivityDTO("DOUBT", d.getDoubtId(), d.getQuestion(), "AI Tutor Doubt Resolved", d.getCreatedAt().toString()));
        });

        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list.stream().limit(8).collect(Collectors.toList());
    }
}
